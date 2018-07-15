package com.digibattle.app.encoder;

import java.util.Arrays;

public class DigimonOriginalEncoder extends DigimonMessageEncoder {

    private static final String TAG = "DigimonOriginalEncoder";

    static final boolean[] MESSAGE_1_SIGNALS = new boolean[]{
            true, true, true, true, true,
            true, true, true, true, true,
            true, true, true, false, false,
            false, false, false, false, false,
    };

    static final boolean[] MESSAGE_0_SIGNALS = new boolean[]{
            true, true, true, true, true,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false
    };

    static final boolean[] START_MESSAGE_SIGNALS = new boolean[]{
            true, true, true, true,
            true, true, true, true, true,
            true, false, false, false, false
    };

    public static final int HANDSHAKE_SIZE = 287; // (2703-130)*4800/48000
    // public static final int HANDSHAKE_SIZE = 219; // 0.0625f * rate()
    public static final int RATE = 4800;

    public static final int MARKER_OFFSET_INDEX = 8;
    public static final int ONE_MESSAGE_SIZE = 20;

    @Override
    public boolean[] encode(boolean b) {
        // Size 20
        if (b) {
            return MESSAGE_1_SIGNALS;
        } else {
            return MESSAGE_0_SIGNALS;
        }
    }

    @Override
    public boolean[] startMessageSignals() {
        return START_MESSAGE_SIGNALS;
    }

    @Override
    public boolean[] handshake() {
        // 0.0625s
        // boolean[] result = new boolean[(int) (0.0625f * rate())];
        boolean[] result = new boolean[HANDSHAKE_SIZE];
        Arrays.fill(result, false);
        return result;
    }

    @Override
    public int rate() {
        return RATE;
    }

    @Override
    public int getOneMessageSize() {
        return ONE_MESSAGE_SIZE;
    }

    @Override
    public int getMessageMarkerOffset() {
        return MARKER_OFFSET_INDEX;
    }
}
