package com.digibattle.app;

import android.util.Log;

/**
 * Class to send / receive digital / analog signals.
 */
public class SignalProcessor {
    private static final String TAG = "SignalProcessor";

    private static final boolean USE_NATIVE = true;
    private final AudioEngine mAudioEngine = USE_NATIVE ? new NativeAudioEngine() : null;

    public static final int MAX_PARTITIONS_NUMBER = 20;
    private static final int DEFAULT_PARTITION_STATUS_CHANGE_THRESHOLD_IN_48000 = 300;

    private static final int PARTITION_STATE_HANDSHAKE_START = 0;
    private static final int PARTITION_STATE_HANDSHAKE_END = 1;
    private static final int PARTITION_STATE_MESSAGE_END = 2;

    /**
     * Send digital signal out in partitions.
     * Flow:
     * 1). I send partition[0].
     * 2). Then I wait for reply.
     * 3). Once reply is finished, then I send partition[1].
     * 4). Then I wait for reply.
     * 5). Loop until I send out all partitions.
     *
     * @param signal          A 2d array, 1st dimension is partition index and 2nd is signal
     *                        index in that partition.
     * @param inputRate       Input signal rate.
     * @param handshakeSize   Handshake signal length at input signal rate. (TODO: Not useful?)
     * @param timeoutToFinish Time to finish after all messages are sent.
     * @param msgLenMs        Each message length in milliseconds. Working with OPTIMIZE_BY_RTT
     *                        to reduce reply latency.
     * @return Output rate
     */
    public int sendDigitalSignal(boolean[][] signal, int inputRate, int handshakeSize,
            boolean timeoutToFinish, int msgLenMs) {
        int partitionThreshold =
                inputRate * DEFAULT_PARTITION_STATUS_CHANGE_THRESHOLD_IN_48000 / 48000;
        return sendAnalogSignal(SignalUtils.digital2Analog(signal), inputRate, handshakeSize,
                partitionThreshold, timeoutToFinish, msgLenMs);
    }

    public int sendAnalogSignal(short[][] signal, int inputRate, int handshakeSize,
            int partitionChangeThreshold, boolean timeoutToFinish, int msgLenMs) {
        mAudioEngine.initEngine(DigiBattleConfig.expectedRTT, msgLenMs, signal, inputRate,
                DigiBattleConfig.voltageChangeThreshold, true, handshakeSize, partitionChangeThreshold,
                timeoutToFinish);
        mAudioEngine.startEngine();
        return mAudioEngine.getReceivedRate();
    }

    /**
     * Same as sendDigitalSignal, but we wait for incoming signal then reply.
     * Flow:
     * 1). I send incoming signal.
     * 2). Then I reply with partition[0].
     * 3). Then I wait for incoming signal.
     * 4). Then I reply with partition[1].
     * 5). Loop until I send out all partitions.
     */
    public int waitDigitalSignal(boolean[][] signal, int rate, int handshakeSize,
            boolean timeoutToFinish, int msgLenMs) {
        int partitionThreshold = rate * DEFAULT_PARTITION_STATUS_CHANGE_THRESHOLD_IN_48000 / 48000;
        return waitAnalogSignal(SignalUtils.digital2Analog(signal), rate, handshakeSize,
                partitionThreshold, timeoutToFinish, msgLenMs);
    }

    public int waitAnalogSignal(short[][] signal, int rate, int handshakeSize,
            int partitionChangeThreshold, boolean timeoutToFinish, int msgLenMs) {
        mAudioEngine.initEngine(DigiBattleConfig.expectedRTT, msgLenMs, signal, rate,
                DigiBattleConfig.voltageChangeThreshold, false, handshakeSize,
                partitionChangeThreshold, timeoutToFinish);
        mAudioEngine.startEngine();
        return mAudioEngine.getReceivedRate();
    }

    /**
     * Get recorded digital signals when send/wait signal is done.
     */
    public boolean[] receiveDigitalSignal() {
        return SignalUtils.analog2Digital(receiveAnalogSignal(),
                DigiBattleConfig.voltageChangeThreshold);
    }

    /**
     * Get recorded analog signals when send/wait signal is done.
     */
    public short[] receiveAnalogSignal() {
        return mAudioEngine.getReceivedSignal();
    }

    /**
     * Get all partition indexes.
     *
     * @return A 2d array that 1st dimension is partition number, and the 2nd is the type.
     */
    public int[][] getPartitionsIndex() {
        int[][] result = new int[MAX_PARTITIONS_NUMBER][3];
        for (int i = 0; i < MAX_PARTITIONS_NUMBER; i++) {
            int[] index = mAudioEngine.getPartitionIndex(i);
            result[i][PARTITION_STATE_HANDSHAKE_START] = index[PARTITION_STATE_HANDSHAKE_START];
            result[i][PARTITION_STATE_HANDSHAKE_END] = index[PARTITION_STATE_HANDSHAKE_END];
            result[i][PARTITION_STATE_MESSAGE_END] = index[PARTITION_STATE_MESSAGE_END];
            Log.i(TAG,
                    "getPartitionsIndex, i: " + i + ", " + result[i][0] + "," + result[i][1] + ","
                            + result[i][2]);
        }
        return result;
    }

    public void stop() {
        mAudioEngine.stopEngine();
    }

    /**
     * Round trip time for receiving my output from audio jack.
     */
    public long getRTT() {
        return mAudioEngine.getRTT();
    }

    public int getStatus() {
        return mAudioEngine.getStatus();
    }

    public boolean isFinished() {
        return getStatus() == AudioEngine.STATUS_FINISHED;
    }
}
