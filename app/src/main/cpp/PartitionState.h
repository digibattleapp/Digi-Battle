#ifndef DIGIBATTLE_PARTITIONS_H
#define DIGIBATTLE_PARTITIONS_H

#define PARTITION_STATUS_PENDING 0
#define PARTITION_STATUS_HANDSHAKE 1
#define PARTITION_STATUS_PROCESSING_MESSAGE 2

#include <oboe/Oboe.h>
#include <vector>

using namespace oboe;

class PartitionState {
private:
    int mVoltageChangeThreshold = 0;

    int mCurrentPartitionStatus = PARTITION_STATUS_PENDING;
    int mCurrentPartition = 0;
    bool mStarted = false;

    int mNumOfContinuousLowInput = 0;
    int mNumOfContinuousHighInput = 0;

    int16_t mPreviousInputSignal = 0;
    bool mPreviousDigitalInputSignal = false;
    int mChangePartitionStatusThreshold = 0;

public:
    PartitionState(int voltageChangeThreshold, int changePartitionStatusThreshold) {
        mVoltageChangeThreshold = voltageChangeThreshold;
        mChangePartitionStatusThreshold = changePartitionStatusThreshold;
    }

    bool updateDigitalInputSignalAndPartition(int16_t currentSignal);

    int getPartitionState() { return mCurrentPartitionStatus; }

    int getPartitionNumber() { return mCurrentPartition; }
};

#endif //DIGIBATTLE_PARTITIONS_H
