#include <algorithm>
#include "PartitionState.h"
#include "OboeDebug.h"

using std::max;

bool PartitionState::updateDigitalInputSignalAndPartition(int16_t currentSignal) {
    if (!mStarted) {
        mStarted = true;
        mPreviousInputSignal = currentSignal;
        mPreviousDigitalInputSignal = true;
        return false;
    }
    int diff = ((int) currentSignal) - mPreviousInputSignal;
    bool currentVoltage = mPreviousDigitalInputSignal;
    if (diff >= mVoltageChangeThreshold) {
        currentVoltage = true;
    } else if (diff <= -mVoltageChangeThreshold) {
        currentVoltage = false;
    } else {
        // currentVoltage == mPreviousDigitalInputSignal
    }
    if (currentVoltage) {
        mNumOfContinuousHighInput++;
        mNumOfContinuousLowInput = 0;
    } else {
        mNumOfContinuousHighInput = 0;
        mNumOfContinuousLowInput++;
    }

    // Determine if a partition is started / stopped / continue
    bool statusChanged = false;
    switch (mCurrentPartitionStatus) {
        case PARTITION_STATUS_PENDING:
            if (mNumOfContinuousLowInput >= mChangePartitionStatusThreshold) {
                mCurrentPartitionStatus = PARTITION_STATUS_HANDSHAKE;
                statusChanged = true;
                LOGI("Change to PARTITION_STATUS_HANDSHAKE");
            }
            break;
        case PARTITION_STATUS_HANDSHAKE:
            if (mNumOfContinuousLowInput <= 3 &&
                mNumOfContinuousHighInput <= 3) {
                mCurrentPartitionStatus = PARTITION_STATUS_PROCESSING_MESSAGE;
                statusChanged = true;
                LOGI("Change to PARTITION_STATUS_PROCESSING_MESSAGE");
            }
            break;
        case PARTITION_STATUS_PROCESSING_MESSAGE:
            if (mNumOfContinuousLowInput >= mChangePartitionStatusThreshold ||
                mNumOfContinuousHighInput >= mChangePartitionStatusThreshold) {
                mCurrentPartitionStatus = PARTITION_STATUS_PENDING;
                statusChanged = true;
                LOGI("Change to PARTITION_STATUS_PENDING");
                mCurrentPartition++;
            }
            break;
    }
    mPreviousInputSignal = currentSignal;
    mPreviousDigitalInputSignal = currentVoltage;
    return statusChanged;
}
