/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef OBOE_STREAM_BUFFERED_H
#define OBOE_STREAM_BUFFERED_H

#include <cstring>
#include <assert.h>
#include "common/OboeDebug.h"
#include "oboe/AudioStream.h"
#include "oboe/AudioStreamCallback.h"
#include "fifo/FifoBuffer.h"

namespace oboe {

// A stream that contains a FIFO buffer.
// This is used to implement blocking reads and writes.
class AudioStreamBuffered : public AudioStream {
public:

    AudioStreamBuffered();
    explicit AudioStreamBuffered(const AudioStreamBuilder &builder);

    void allocateFifo();


    ErrorOrValue<int32_t> write(const void *buffer,
                  int32_t numFrames,
                  int64_t timeoutNanoseconds) override;

    ErrorOrValue<int32_t> read(void *buffer,
                 int32_t numFrames,
                 int64_t timeoutNanoseconds) override;

    Result setBufferSizeInFrames(int32_t requestedFrames) override;

    int32_t getBufferSizeInFrames() const override;

    int32_t getBufferCapacityInFrames() const override;

    int32_t getXRunCount() const override {
        return mXRunCount;
    }

    int64_t getFramesWritten() const override;

    int64_t getFramesRead() const override;

protected:

    DataCallbackResult onDefaultCallback(void *audioData, int numFrames) override;

    // If there is no callback then we need a FIFO between the App and OpenSL ES.
    bool usingFIFO() const { return getCallback() == nullptr; }

    virtual Result updateServiceFrameCounter() { return Result::OK; };

private:


    int64_t predictNextCallbackTime();

    void markCallbackTime(int numFrames);

    // Read or write to the FIFO.
    ErrorOrValue<int32_t> transfer(void *buffer, int32_t numFrames, int64_t timeoutNanoseconds);

    void incrementXRunCount() {
        mXRunCount++;
    }

    std::unique_ptr<FifoBuffer>                  mFifoBuffer;

    int64_t mBackgroundRanAtNanoseconds = 0;
    int32_t mLastBackgroundSize = 0;
    int32_t mXRunCount = 0;
};

} // namespace oboe

#endif //OBOE_STREAM_BUFFERED_H
