package com.digibattle.app;

public class NativeAudioEngine extends AudioEngine {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public native void stopEngine();

    public native void startEngine();

    public native void initEngine(int expectedRTTms, int expectedMsgLenMs, short[][] outputFrames,
            int outputRate,
            int inputSignalStartThreshold,
            boolean asSender, int handshakeSize, int partitionStatusChangeThreshold,
            boolean timeoutToFinish);


    public native int getStatus();

    public native long getRTT();

    public native short[] getReceivedSignal();

    public native int getReceivedRate();

    public native int[] getPartitionIndex(int partitionNumber);
}
