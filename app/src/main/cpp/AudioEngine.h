#ifndef AUDIOENGINE_H
#define AUDIOENGINE_H

#define STATUS_PENDING_SIGNAL 0
#define STATUS_PROCESSING_SIGNAL 1
#define STATUS_FINISHED 2

#include <oboe/Oboe.h>
#include <vector>

#include "PartitionState.h"

const int MAX_SUPPORTED_PARTITIONS = 32;

using namespace oboe;

class Frame {
public:
    int16_t *content = nullptr;
    int size;

    Frame(int s) {
        size = s;
        content = new int16_t[size];
    }

    ~Frame() {
        if (content) {
            delete content;
        }
    }
};

class AudioEngine : public AudioStreamCallback {

public:
    AudioEngine(int startInputSignalThreshold, bool asSender,
                std::shared_ptr<Frame> outputFrames[MAX_SUPPORTED_PARTITIONS], int numOfPartitions,
                int outputRate,
                int handshakeSize, int partitionStateChangeThreshold, bool timeoutToFinish,
                int expectedRTT, int expectedMsgLenMs);

    DataCallbackResult
    onAudioReady(AudioStream *oboeStream, void *audioData, int32_t numFrames) override;

    void start();

    void stop();

    int64_t getRTT();

    int getStatus();

    std::vector<int16_t> getRecordedSignal();

    int getRecordedRate();

    int getPartitionIndex(int partitionNumber, int label);


private:
    AudioStream *mOutputStream = nullptr;
    AudioStream *mInputStream = nullptr;
    int mInputFramesPerBurst = 0;
    int mOutputFramesPerBurst = 0;
    int mInputRate = 0;
    int mOutputRate = 0;

    // From user settings
    bool mAsSender = false;
    bool mTimeoutToFinish = false;
    int mNumOfOutputFramePartitions = 0;
    int mOriginalHandshakeSize = 0;
    int mStartInputSignalThreshold = 0;
    int mPartitionStateChangeThreshold = 0;
    std::shared_ptr<Frame> mOutputFramesBeforeResampling[MAX_SUPPORTED_PARTITIONS];
    int mOutputFramesRateBeforeResampling = 0;
    int mExpectedRTTms = 0;
    int mExpectedMsgLenMs = 0;

    // Generated from user settings
    std::shared_ptr<Frame> mOutputFrames[MAX_SUPPORTED_PARTITIONS];
    int mNativeHandshakeSize = 0;
    int mExpectedMessageFrameLenMinusRTT = 0;
    bool mResampleOutputDone = false;

    // Current state
    int mCurrentOutputFrameIndex = 0;
    int mCurrentInputFrameIndex = 0;
    int mPreviousPartitionNumber = 0;
    bool mAppliedLatencyOptimization = false;
    bool mInputStreamReady = false;
    bool mOutputStreamReady = false;
    std::vector<int16_t> mInputRecord;
    int mCurrentStatus = STATUS_PENDING_SIGNAL;
    std::shared_ptr<PartitionState> mPartitionState;
    int64_t mStartSendSignalTime = 0;
    int64_t mStartReceiveSignalTime = 0;
    int mPartitionHandshakeStartIndex[MAX_SUPPORTED_PARTITIONS] = {};
    int mPartitionHandshakeEndIndex[MAX_SUPPORTED_PARTITIONS] = {};
    int mPartitionSignalEndIndex[MAX_SUPPORTED_PARTITIONS] = {};

    bool setupOutputStream();

    bool setupInputStream();

    DataCallbackResult handleNewInput(int16_t *audioData, int32_t numFrames);

    DataCallbackResult handleNewOutput(int16_t *audioData, int32_t numFrames);

    void resampleOutputFrames();
};


#endif //AUDIOENGINE_H
