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

#include <jni.h>
#include <string>

#include "AudioEngine.h"
#include "PartitionState.h"
#include "OboeDebug.h"

std::shared_ptr<AudioEngine> engine;

extern "C"
JNIEXPORT void JNICALL
Java_com_digibattle_app_NativeAudioEngine_startEngine(JNIEnv *env, jobject instance) {

    if (engine == nullptr) {
        LOGE("Engine is not inited");
        return;
    }
    engine->start();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_digibattle_app_NativeAudioEngine_stopEngine(JNIEnv *env, jobject instance) {

    if (engine == nullptr) {
        LOGE("Engine is not inited");
        return;
    }
    engine->stop();
    engine = nullptr;

}


extern "C"
JNIEXPORT void JNICALL
Java_com_digibattle_app_NativeAudioEngine_initEngine(JNIEnv *env, jobject instance,
                                                     jint expectedRTTms,
                                                     jint expectedMsgLenMs,
                                                     jobjectArray outputFrames,
                                                     jint outputRate,
                                                     jint inputSignalStartThreshold,
                                                     jboolean asSender,
                                                     jint handshakeSize,
                                                     jint partitionStatusChangeThreshold,
                                                     jboolean timeoutToFinish) {
    LOGI("Engine init starts");
    jsize len = env->GetArrayLength(outputFrames);
    std::shared_ptr<Frame> targetOutputFrames[MAX_SUPPORTED_PARTITIONS];
    for (int i = 0; i < len; i++) {
        jboolean isCopy1;
        jshortArray array = (jshortArray) env->GetObjectArrayElement(outputFrames, i);
        int len2 = env->GetArrayLength(array);
        targetOutputFrames[i] = std::make_shared<Frame>(len2);
        jshort *srcArrayElems = env->GetShortArrayElements(array, &isCopy1);
        memcpy(targetOutputFrames[i].get()->content, srcArrayElems, len2 * sizeof(int16_t));
        if (isCopy1 == JNI_TRUE) {
            env->ReleaseShortArrayElements(array, srcArrayElems, JNI_ABORT);
        }
    }
    engine = std::make_shared<AudioEngine>(inputSignalStartThreshold, asSender, targetOutputFrames,
                                           len, outputRate, handshakeSize,
                                           partitionStatusChangeThreshold, timeoutToFinish,
                                           expectedRTTms, expectedMsgLenMs);
    LOGI("Engine init done");
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_digibattle_app_NativeAudioEngine_getStatus(JNIEnv *env, jobject instance) {
    LOGI("Get status starts");
    return engine->getStatus();
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_digibattle_app_NativeAudioEngine_getRTT(JNIEnv *env, jobject instance) {
    LOGI("Get RTT");
    return engine->getRTT();
}


extern "C"
JNIEXPORT jshortArray JNICALL
Java_com_digibattle_app_NativeAudioEngine_getReceivedSignal(JNIEnv *env, jobject instance) {
    if (engine == nullptr) {
        LOGE("Engine is not inited");
        return NULL;
    }
    jshortArray result;
    std::vector<int16_t> signals = engine->getRecordedSignal();
    int size = signals.size();
    result = env->NewShortArray(size);
    if (result == NULL) {
        return NULL; /* out of memory error thrown */
    }
    int i;
    // fill a temp structure to use to populate the java int array
    jshort fill[size];
    for (i = 0; i < size; i++) {
        fill[i] = signals[i];
    }
    // move from the temp structure to the java structure
    env->SetShortArrayRegion(result, 0, size, fill);
    return result;

}


extern "C"
JNIEXPORT jint JNICALL
Java_com_digibattle_app_NativeAudioEngine_getReceivedRate(JNIEnv *env, jobject instance) {
    if (engine == nullptr) {
        LOGE("Engine is not inited");
        return 0;
    }
    return engine->getRecordedRate();
}


extern "C"
JNIEXPORT jintArray JNICALL
Java_com_digibattle_app_NativeAudioEngine_getPartitionIndex(JNIEnv *env, jobject instance,
                                                            jint partitionNumber) {
    if (engine == nullptr) {
        LOGE("Engine is not inited");
        return NULL;
    }
    jintArray result = env->NewIntArray(3);
    if (result == NULL) {
        return NULL;
    }
    int i;
    int fill[3];
    for (i = 0; i < 3; i++) {
        fill[i] = engine->getPartitionIndex(partitionNumber, i);
    }
    env->SetIntArrayRegion(result, 0, 3, fill);
    return result;
}

