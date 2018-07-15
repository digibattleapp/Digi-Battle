package com.digibattle.app;

public abstract class AudioEngine {


    public static final int STATUS_PENDING_SIGNAL = 0;
    public static final int STATUS_PROCESSING_SIGNAL = 1;
    public static final int STATUS_FINISHED = 2;

    // Stop sending / receiving signals.
    public abstract void stopEngine();

    // Start sending / receiving signals.
    public abstract void startEngine();

    /**
     * @param expectedRTTms                  Expected RTT time in milliseconds.
     * @param expectedMsgLenMs               Expected message size in milliseconds.
     * @param outputFrames                   Output frames in partitions.
     * @param outputRate                     Output frames rate.
     * @param inputSignalStartThreshold      Threshold to trigger receiving data.
     * @param asSender                       True if I'm the sender.
     * @param handshakeSize                  Handshake size in terms of number of frames.
     * @param partitionStatusChangeThreshold Number of frames for same values to trigger
     *                                       partition change.
     * @param timeoutToFinish                Timeout after sending all signals in milliseconds.
     */
    public abstract void initEngine(int expectedRTTms, int expectedMsgLenMs, short[][] outputFrames,
            int outputRate,
            int inputSignalStartThreshold,
            boolean asSender, int handshakeSize, int partitionStatusChangeThreshold,
            boolean timeoutToFinish);


    public abstract int getStatus();

    /**
     * Get round trip time in milliseconds.
     */
    public abstract long getRTT();

    public abstract int[] getPartitionIndex(int partitionNumber);

    public abstract short[] getReceivedSignal();

    public abstract int getReceivedRate();
}
