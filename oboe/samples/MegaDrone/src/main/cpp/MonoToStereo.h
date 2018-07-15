/*
 * Copyright 2018 The Android Open Source Project
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

#ifndef MEGADRONE_MONOTOSTEREO_H
#define MEGADRONE_MONOTOSTEREO_H

#include "RenderableAudio.h"
#include "Constants.h"

template <typename T>
class MonoToStereo : public RenderableAudio<T> {

public:

    MonoToStereo(RenderableAudio<T> *input) : mInput(input){};

    void renderAudio(T *audioData, int32_t numFrames) override {

        mInput->renderAudio(audioData, numFrames);

        // We assume that audioData has sufficient frames to hold the stereo output, so copy each
        // frame in the input to the output twice, working our way backwards through the input array
        // e.g. 123 => 112233
        for (int i = numFrames - 1; i >= 0; --i) {

            audioData[i * kStereoChannelCount] = audioData[i];
            audioData[i * kStereoChannelCount + 1] = audioData[i];
        }
    }

    RenderableAudio<T> *mInput;
};


#endif //MEGADRONE_MONOTOSTEREO_H
