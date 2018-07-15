package com.digibattle.app;

import android.os.Looper;

import com.digibattle.app.encoder.DigimonMessageEncoder;

public class DigimonMessageHelper {

    public static class DigimonMessageResult {
        public String[] hexMsg;
        public boolean[] digitalSignal;
        public short[] analogSignal;
        public int[][] partitionIndex;
        public int rate;
    }

    public static class ErrorResult extends DigimonMessageResult {
        public ErrorResult(String errorMsg) {
            hexMsg = new String[1];
            hexMsg[0] = errorMsg;
        }
    }

    private final SignalProcessor mProcessor = new SignalProcessor();
    private final DigimonMessageEncoder mEncoder;

    private boolean mIsRunning = false;
    private boolean mStopRequested = false;

    public DigimonMessageHelper(DigimonMessageEncoder encoder) {
        this.mEncoder = encoder;
    }

    /**
     * Send digimon message out!
     * Each partition is a 4 char hex message, like e123 or 0f0f.
     *
     * @param hexStringPartition Array of 4 char hex messages as input.
     * @return Array of 4 char hex messages as received signals.
     */
    public DigimonMessageResult sendDigimonMessage(String[] hexStringPartition,
            Runnable startProcessingListener) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            throw new IllegalStateException("Do not run it in main thread.");
        }
        synchronized (this) {
            if (!mIsRunning) {
                mIsRunning = true;
                mStopRequested = false;
            } else {
                return new ErrorResult("Already running");
            }
        }
        return processDigimonMessage(hexStringPartition, true, startProcessingListener, null);
    }

    /**
     * Wait for incoming digimon message and reply!
     * Each partition is a 4 char hex message, like e123 or 0f0f.
     *
     * @param hexStringPartition Array of 4 char hex messages as input.
     */
    public DigimonMessageResult replyDigimonMessage(String[] hexStringPartition,
            Runnable startProcessingListener, Runnable startReplyingListener) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            throw new IllegalStateException("Do not run it in main thread.");
        }
        synchronized (this) {
            if (!mIsRunning) {
                mIsRunning = true;
                mStopRequested = false;
            } else {
                return new ErrorResult("Already running");
            }
        }
        return processDigimonMessage(hexStringPartition, false, startProcessingListener,
                startReplyingListener);
    }

    /**
     * Stop existing send / wait messages function.
     */
    public void stop() {
        mStopRequested = true;
    }

    private DigimonMessageResult processDigimonMessage(String[] hexStringPartition, boolean sender,
            Runnable startProcessingListener, Runnable startReplyingListener) {
        int partitionLength = hexStringPartition.length;
        boolean[][] digitalSignalPartition = new boolean[partitionLength][];
        for (int i = 0; i < partitionLength; i++) {
            digitalSignalPartition[i] = mEncoder.encodeWithHandshakeAndStartSignal(
                    hexStringPartition[i]);
        }
        try {
            int outputRate;
            if (sender) {
                outputRate = mProcessor.sendDigitalSignal(digitalSignalPartition, mEncoder.rate(),
                        mEncoder.handshake().length, true,
                        mEncoder.messageWithSignalSignalLenInMs());

            } else {
                outputRate = mProcessor.waitDigitalSignal(digitalSignalPartition, mEncoder.rate(),
                        mEncoder.handshake().length, true,
                        mEncoder.messageWithSignalSignalLenInMs());
            }
            if (startProcessingListener != null) {
                startProcessingListener.run();
            }
            boolean triggeredReplyListener = false;
            DigimonMessageResult result = new DigimonMessageResult();
            try {
                while (!mStopRequested) {
                    if (startReplyingListener != null && !triggeredReplyListener
                            && mProcessor.getStatus() == AudioEngine.STATUS_PROCESSING_SIGNAL) {
                        triggeredReplyListener = true;
                        startReplyingListener.run();
                    }
                    if (mProcessor.isFinished()) {
                        int[][] partitionsIndex = mProcessor.getPartitionsIndex();
                        boolean[] digitalSignal = mProcessor.receiveDigitalSignal();
                        int numOfMessage = partitionsIndex.length;
                        String[] hexMsg = new String[numOfMessage];
                        for (int i = 0; i < numOfMessage; i++) {
                            boolean[] partitionSignal = SignalUtils.getPartition(digitalSignal,
                                    partitionsIndex[i][1], partitionsIndex[i][2]);
                            boolean[] digitalMessage = mEncoder.decodeDigitalSignal(outputRate,
                                    partitionSignal);
                            hexMsg[i] = mEncoder.getHexMessage(digitalMessage);
                        }
                        result.partitionIndex = partitionsIndex;
                        result.digitalSignal = mProcessor.receiveDigitalSignal();
                        result.analogSignal = mProcessor.receiveAnalogSignal();
                        result.hexMsg = hexMsg;
                        result.rate = outputRate;
                        return result;
                    }
                    Thread.sleep(100);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            mProcessor.stop();
            mIsRunning = false;
        }
        return new ErrorResult("Failed");
    }

    /**
     * Get the fucking stupid checksum value for digimon 20th.
     */
    public static char get20ThChecksumChar(String[] messages) {
        int total = 0;
        for (int i = 0; i < 9; i++) {
            total += hexToInt(messages[i].charAt(0));
            total += hexToInt(messages[i].charAt(1));
            total += hexToInt(messages[i].charAt(2));
            total += hexToInt(messages[i].charAt(3));
        }
        total += hexToInt(messages[9].charAt(1));
        total += hexToInt(messages[9].charAt(2));
        total += hexToInt(messages[9].charAt(3));
        char[] table =
                {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        return table[(16 - total % 16) % 16];
    }

    public int[] getMarkerPos(int rate) {
        return mEncoder.getMarkerPosition(rate);
    }

    private static int hexToInt(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else {
            return c - 'a' + 10;
        }
    }
}
