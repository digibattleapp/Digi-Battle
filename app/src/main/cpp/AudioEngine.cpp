#include <memory>
#include "AudioEngine.h"
#include "OboeDebug.h"

constexpr int64_t kMillisecondsInSecond = 1000;
constexpr int64_t kNanosecondsInMillisecond = 1000000;

inline int64_t nowUptimeMillis() {
    struct timespec res;
    clock_gettime(CLOCK_MONOTONIC, &res);
    return (res.tv_sec * kMillisecondsInSecond) + res.tv_nsec / kNanosecondsInMillisecond;
}

inline int resampleSize(int sourceSize, int sourceRate, int targetRate) {
    return sourceSize * targetRate / sourceRate;
}

inline bool isOdd(int i) {
    return i % 2 == 1;
}

inline bool isEven(int i) {
    return i % 2 == 0;
}

AudioEngine::AudioEngine(int startInputSignalThreshold, bool asSender,
                         std::shared_ptr<Frame> outputFrames[MAX_SUPPORTED_PARTITIONS],
                         int numOfPartitions,
                         int outputRate, int handshakeSize, int partitionStateChangeThreshold,
                         bool timeoutToFinish,
                         int expectedRTTms, int expectedMsgLenMs) {
    for (int i = 0; i < MAX_SUPPORTED_PARTITIONS; i++) {
        mOutputFramesBeforeResampling[i] = outputFrames[i];
    }
    mOutputFramesRateBeforeResampling = outputRate;
    mNumOfOutputFramePartitions = numOfPartitions;
    mAsSender = asSender;
    mTimeoutToFinish = timeoutToFinish;
    mOriginalHandshakeSize = handshakeSize;
    mStartInputSignalThreshold = startInputSignalThreshold;
    mPartitionStateChangeThreshold = partitionStateChangeThreshold;
    mExpectedRTTms = expectedRTTms;
    mExpectedMsgLenMs = expectedMsgLenMs;
}

bool AudioEngine::setupOutputStream() {
    LOGD("Start setupOutputStream()");
    AudioStreamBuilder outputStreamBuilder;
    outputStreamBuilder.setCallback(this);
    outputStreamBuilder.setPerformanceMode(PerformanceMode::LowLatency);
    //outputStreamBuilder.setSharingMode(SharingMode::Exclusive);
    outputStreamBuilder.setDirection(Direction::Output);
    outputStreamBuilder.setChannelCount(1);
    outputStreamBuilder.setFormat(AudioFormat::I16);
    Result outputStreamResult = outputStreamBuilder.openStream(&mOutputStream);
    if (outputStreamResult != Result::OK) {
        LOGE("Failed to open output stream. Error: %s", convertToText(outputStreamResult));
        return false;
    }
    mOutputFramesPerBurst = mOutputStream->getFramesPerBurst();
    mOutputRate = mOutputStream->getSampleRate();

    int channelCount = mOutputStream->getChannelCount();
    if (channelCount != 1) {
        LOGW("Requested %d channels but received %d", 1, channelCount);
        return false;
    }

    // Set the buffer size to the burst size - this will give us the minimum possible latency
    mOutputStream->setBufferSizeInFrames(mOutputFramesPerBurst);
    mOutputStream->requestStart();
    return true;
}

bool AudioEngine::setupInputStream() {
    LOGD("Start setupInputStream()");
    AudioStreamBuilder inputStreamBuilder;
    inputStreamBuilder.setCallback(this);
    inputStreamBuilder.setPerformanceMode(PerformanceMode::LowLatency);
    //inputStreamBuilder.setSharingMode(SharingMode::Exclusive);
    inputStreamBuilder.setDirection(Direction::Input);
    inputStreamBuilder.setChannelCount(1);
    inputStreamBuilder.setFormat(AudioFormat::I16);
    Result inputStreamResult = inputStreamBuilder.openStream(&mInputStream);
    if (inputStreamResult != Result::OK) {
        LOGE("Failed to open input stream. Error: %s", convertToText(inputStreamResult));
        return false;
    }
    mInputFramesPerBurst = mInputStream->getFramesPerBurst();
    mInputRate = mInputStream->getSampleRate();

    int channelCount = mInputStream->getChannelCount();
    if (channelCount != 1) {
        LOGW("Requested %d channels but received %d", 1, channelCount);
        return false;
    }

    // Set the buffer size to the burst size - this will give us the minimum possible latency
    mInputStream->setBufferSizeInFrames(mInputFramesPerBurst);
    mInputStream->requestStart();
    return true;
}

void AudioEngine::resampleOutputFrames() {
    if (mOutputFramesRateBeforeResampling <= 0 || mOutputRate <= 0) {
        LOGW("Invalid output rate");
        return;
    }
    for (int i = 0; i < mNumOfOutputFramePartitions; i++) {
        int16_t *frameContent = mOutputFramesBeforeResampling[i].get()->content;
        long frameSize = mOutputFramesBeforeResampling[i].get()->size;
        int outputFrameSize = static_cast<int>(frameSize * mOutputRate /
                                               mOutputFramesRateBeforeResampling);
        mOutputFrames[i] = std::make_shared<Frame>(outputFrameSize);
        int16_t *content = mOutputFrames[i].get()->content;
        if (outputFrameSize == frameSize) {
            // No resample need
            memcpy(content, frameContent, outputFrameSize);
        } else {
            for (int j = 0; j < outputFrameSize; j++) {
                content[j] = frameContent[
                        static_cast<long>(j) * mOutputFramesRateBeforeResampling / mOutputRate];
            }
        }
    }
    mResampleOutputDone = true;
}

void AudioEngine::start() {
    LOGD("Start start()");
    if (!setupOutputStream()) {
        return;
    }
    mNativeHandshakeSize = resampleSize(mOriginalHandshakeSize, mOutputFramesRateBeforeResampling,
                                        mOutputRate);
    int resampledPartitionStateChangeThreshold = resampleSize(mPartitionStateChangeThreshold,
                                                              mOutputFramesRateBeforeResampling,
                                                              mOutputRate);
    resampleOutputFrames();
    if (!setupInputStream()) {
        return;
    }
    if (mExpectedRTTms > 0 && mExpectedMsgLenMs > 0) {
        // Optimize reply latency
        if (mExpectedMsgLenMs < mExpectedRTTms) {
            mExpectedMessageFrameLenMinusRTT = 0;
        } else {
            mExpectedMessageFrameLenMinusRTT =
                    mOutputRate * (mExpectedMsgLenMs - mExpectedRTTms / 2) / 1000;
        }
        int minLen = mOutputRate * mExpectedMsgLenMs / 1000 / 16 /* magic number */;
        mExpectedMessageFrameLenMinusRTT = mExpectedMessageFrameLenMinusRTT > minLen ?
                                           mExpectedMessageFrameLenMinusRTT : minLen;
        LOGE("ExpectedMessageFrameLenMinusRTT: %d %d %d %d", mExpectedMessageFrameLenMinusRTT,
             mOutputRate, mExpectedMsgLenMs, mExpectedRTTms);
    }
    mPartitionState = std::make_shared<PartitionState>(mStartInputSignalThreshold,
                                                       resampledPartitionStateChangeThreshold);
    mCurrentStatus = STATUS_PENDING_SIGNAL;
    LOGD("Finished start()");
}

void AudioEngine::stop() {
    LOGD("stop()");
    if (mInputStream != nullptr) {
        oboe::Result result = mInputStream->requestStop();
        if (result != oboe::Result::OK) {
            LOGE("Error stopping output stream. %s", oboe::convertToText(result));
        }

        result = mInputStream->close();
        if (result != oboe::Result::OK) {
            LOGE("Error closing output stream. %s", oboe::convertToText(result));
        }
    }
    if (mOutputStream != nullptr) {
        oboe::Result result = mOutputStream->requestStop();
        if (result != oboe::Result::OK) {
            LOGE("Error stopping output stream. %s", oboe::convertToText(result));
        }

        result = mOutputStream->close();
        if (result != oboe::Result::OK) {
            LOGE("Error closing output stream. %s", oboe::convertToText(result));
        }
    }
    LOGD("Finished stop()");
}

DataCallbackResult
AudioEngine::onAudioReady(AudioStream *oboeStream, void *audioData, int32_t numFrames) {
    if (oboeStream == mInputStream) {
        if (!mInputStreamReady) {
            LOGD("InputStream ready");
            mInputStreamReady = true;
        }
        return handleNewInput(static_cast<int16_t *>(audioData), numFrames);
    } else if (oboeStream == mOutputStream) {
        if (!mOutputStreamReady) {
            LOGD("OutputStream ready");
            mOutputStreamReady = true;
        }
        return handleNewOutput(static_cast<int16_t *>(audioData), numFrames);
    } else {
        LOGE("WTF?");
    }
    return DataCallbackResult::Continue;
}

DataCallbackResult AudioEngine::handleNewInput(int16_t *audioData, int32_t numFrames) {
    if (mOutputStreamReady && mInputStreamReady && mPartitionState != nullptr) {
        for (int i = 0; i < numFrames; i++) {
            mInputRecord.push_back(audioData[i]);
            bool changedState = mPartitionState->updateDigitalInputSignalAndPartition(audioData[i]);
            int partitionState = mPartitionState->getPartitionState();
            int partitionNumber = mPartitionState->getPartitionNumber();
            if (mCurrentStatus == STATUS_PENDING_SIGNAL &&
                (partitionNumber > 0 ||
                 partitionState != PARTITION_STATUS_PENDING || mAsSender)) {
                LOGI("Start processing signal!");
                mCurrentStatus = STATUS_PROCESSING_SIGNAL;
            }
            if (mStartReceiveSignalTime == 0 &&
                (partitionNumber > 0 ||
                 partitionState != PARTITION_STATUS_PENDING)) {
                mStartReceiveSignalTime = nowUptimeMillis();
                LOGI("record mStartReceiveSignalTime %ld", mStartReceiveSignalTime);
            }
            if (changedState) {
                switch (partitionState) {
                    case PARTITION_STATUS_PENDING:
                        if (partitionNumber - 1 < MAX_SUPPORTED_PARTITIONS &&
                            partitionNumber >= 1) {
                            mPartitionSignalEndIndex[partitionNumber - 1] = mCurrentInputFrameIndex;
                        } else {
                            LOGE("partitionNumber too large: %d", partitionNumber);
                        }
                        break;
                    case PARTITION_STATUS_HANDSHAKE:
                        if (partitionNumber < MAX_SUPPORTED_PARTITIONS) {
                            mPartitionHandshakeStartIndex[partitionNumber] = mCurrentInputFrameIndex;
                        } else {
                            LOGE("partitionNumber too large: %d", partitionNumber);
                        }
                        break;
                    case PARTITION_STATUS_PROCESSING_MESSAGE:
                        if (partitionNumber < MAX_SUPPORTED_PARTITIONS) {
                            mPartitionHandshakeEndIndex[partitionNumber] = mCurrentInputFrameIndex;
                        } else {
                            LOGE("partitionNumber too large: %d", partitionNumber);
                        }
                        break;
                }
            }
            bool startSendNextPartition = false;
            if (partitionState == PARTITION_STATUS_PROCESSING_MESSAGE) {
                /*
                 * Reduce latency optimization
                 *
                 * Problem: Phone's audio jack may have some latency to receive audio signal,
                 * like the signal is already sent but we receive it after Xms.
                 *
                 * Solution: Do not wait for all messages are received, we jump to next partition
                 * when we *think* sender sent all messages already.-
                 */
                if ((mAsSender && isOdd(partitionNumber)) ||
                    (!mAsSender && isEven(partitionNumber))) {
                    if (mExpectedMessageFrameLenMinusRTT != 0 &&
                        ((partitionNumber + 1) < (mNumOfOutputFramePartitions * 2)) &&
                        mPartitionHandshakeEndIndex[partitionNumber] != 0 &&
                        (mCurrentInputFrameIndex - mPartitionHandshakeEndIndex[partitionNumber])
                        >= mExpectedMessageFrameLenMinusRTT) {
                        startSendNextPartition = true;
                    }
                }
            }
            if (startSendNextPartition) {
                if (!mAppliedLatencyOptimization) {
                    mCurrentOutputFrameIndex = 0;
                    mPreviousPartitionNumber = partitionNumber + 1;
                }
                mAppliedLatencyOptimization = true;
            } else {
                mAppliedLatencyOptimization = false;
            }
            mCurrentInputFrameIndex++;
        }
        if (mCurrentStatus == STATUS_PENDING_SIGNAL) {
            mInputRecord.clear();
            mCurrentInputFrameIndex = 0;
        }
    }
    return DataCallbackResult::Continue;
}

DataCallbackResult AudioEngine::handleNewOutput(int16_t *audioData, int32_t numFrames) {
    if (!mResampleOutputDone || mPartitionState == nullptr) {
        memset(audioData, 0, sizeof(int16_t) * numFrames);
        return DataCallbackResult::Continue;
    }
    bool needSend = false;
    int partitionNumber = mPartitionState->getPartitionNumber();

    if (mPreviousPartitionNumber < partitionNumber) {
        mCurrentOutputFrameIndex = 0;
        mPreviousPartitionNumber = partitionNumber;
    }
    int outputFramesPartitionIndex = mPreviousPartitionNumber / 2;

    if (mInputStreamReady && mOutputStreamReady && mResampleOutputDone) {
        needSend = mAsSender ? isEven(mPreviousPartitionNumber) : isOdd(mPreviousPartitionNumber);
    }

    if (outputFramesPartitionIndex >= mNumOfOutputFramePartitions ||
        ((outputFramesPartitionIndex < mNumOfOutputFramePartitions - 1)
         && mOutputFrames[outputFramesPartitionIndex].get()->size <= mCurrentOutputFrameIndex)) {
        if (needSend) {
            needSend = false;
        }
        if (mTimeoutToFinish) {
            if (mCurrentOutputFrameIndex > mNativeHandshakeSize * 4) {
                LOGI("Changing status to finish");
                mCurrentStatus = STATUS_FINISHED;
            }
        }
    }

    if (needSend) {
        int16_t* currentOutputPartitionFrameContent = mOutputFrames[outputFramesPartitionIndex].get()->content;
        int currentOutputPartitionFrameSize = mOutputFrames[outputFramesPartitionIndex].get()->size;
        if (mStartSendSignalTime == 0) {
            mStartSendSignalTime = nowUptimeMillis();
            LOGI("record mStartSendSignalTime %ld", mStartSendSignalTime);
        }
        int int16_t_size = sizeof(int16_t);
        if (numFrames <= currentOutputPartitionFrameSize - mCurrentOutputFrameIndex) {
            memcpy(audioData, currentOutputPartitionFrameContent + mCurrentOutputFrameIndex,
                   numFrames * int16_t_size);
        } else {
            memset(audioData, 0, int16_t_size * numFrames);
            if (currentOutputPartitionFrameSize > mCurrentOutputFrameIndex) {
                memcpy(audioData, currentOutputPartitionFrameContent + mCurrentOutputFrameIndex,
                       (currentOutputPartitionFrameSize - mCurrentOutputFrameIndex) * int16_t_size);
                LOGI("Finished sending signal");
            } else {
                LOGI("Should not happen: %d %d %d", mPreviousPartitionNumber,
                     outputFramesPartitionIndex, mCurrentOutputFrameIndex);
            }
        }
    } else {
        memset(audioData, 0, sizeof(int16_t) * numFrames);
    }
    if (mStartSendSignalTime != 0) {
        mCurrentOutputFrameIndex += numFrames;
    }
    return DataCallbackResult::Continue;
}

int AudioEngine::getStatus() {
    return mCurrentStatus;
}

int64_t AudioEngine::getRTT() {
    return mStartReceiveSignalTime - mStartSendSignalTime;
}

int AudioEngine::getRecordedRate() {
    return mInputRate;
}

std::vector<int16_t> AudioEngine::getRecordedSignal() {
    return mInputRecord;
}

int AudioEngine::getPartitionIndex(int partitionNumber, int label) {
    if (partitionNumber < 0 || partitionNumber >= MAX_SUPPORTED_PARTITIONS) {
        LOGE("Bad partition number %d", partitionNumber);
        return -1;
    }
    switch (label) {
        case PARTITION_STATUS_PENDING:
            return mPartitionHandshakeStartIndex[partitionNumber];
        case PARTITION_STATUS_HANDSHAKE:
            return mPartitionHandshakeEndIndex[partitionNumber];
        case PARTITION_STATUS_PROCESSING_MESSAGE:
            return mPartitionSignalEndIndex[partitionNumber];
        default:
            return -1;
    }
}