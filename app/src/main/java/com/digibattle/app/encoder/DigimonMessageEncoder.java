package com.digibattle.app.encoder;

import java.math.BigInteger;

public abstract class DigimonMessageEncoder {
    public abstract boolean[] encode(boolean b);

    public abstract boolean[] handshake();

    public abstract boolean[] startMessageSignals();

    public abstract int rate();

    public abstract int getOneMessageSize();

    public abstract int getMessageMarkerOffset();

    public boolean[] encode(String hexString) {
        String binary = new BigInteger(hexString, 16).toString(2);
        binary = (String.format("%16s", binary).replace(" ", "0"));
        int binaryLength = binary.length();
        int oneMessageSize = getOneMessageSize();
        boolean[] result = new boolean[binaryLength * oneMessageSize];
        for (int i = 0; i < binaryLength; i++) {
            boolean[] hexResult = encode(binary.charAt(binaryLength - i - 1) == '1');
            System.arraycopy(hexResult, 0, result, i * oneMessageSize, oneMessageSize);
        }
        return result;
    }

    public boolean[] encodeWithHandshakeAndStartSignal(String hexString) {
        boolean[] handshake = handshake();
        boolean[] startSignals = startMessageSignals();
        boolean[] encoded = encode(hexString);
        boolean[] result = new boolean[handshake.length + encoded.length + startSignals.length];
        System.arraycopy(handshake, 0, result, 0, handshake.length);
        System.arraycopy(startSignals, 0, result, handshake.length, startSignals.length);
        System.arraycopy(encoded, 0, result, handshake.length + startSignals.length,
                encoded.length);
        return result;
    }

    public boolean[] decodeDigitalSignal(int inputRate, boolean[] digitalSignal) {
        if (digitalSignal == null) {
            return null;
        }
        int rate = rate();
        double interval = (((double) getOneMessageSize()) * inputRate / rate);
        int initOffset = (int) 1f * startMessageSignals().length * inputRate / rate;
        int markerOffset = (int) 1f * getMessageMarkerOffset() * inputRate / rate;
        double i = initOffset + markerOffset;
        int index = 0;
        int numOfResults = 16;
        boolean[] result = new boolean[numOfResults];
        while (i < digitalSignal.length && index < 16) {
            result[index] = digitalSignal[(int) i];
            i = i + interval;
            index++;
        }
        return result;
    }

    public String getHexMessage(boolean[] digitalMessage) {
        if (digitalMessage == null) {
            return "";
        }
        if (digitalMessage.length != 16) {
            return "Invalid size: " + digitalMessage.length;
        }
        String str = "";
        for (boolean b : digitalMessage) {
            str = (b ? "1" : "0") + str;
        }
        int decimal = Integer.parseInt(str, 2);
        String result = Integer.toString(decimal, 16);
        result = String.format("%4s", result).replace(" ", "0");
        return result;
    }

    public int[] getMarkerPosition(int inputRate) {
        int rate = rate();
        double interval = (((double) getOneMessageSize()) * inputRate / rate);
        int initOffset = (int) 1f * startMessageSignals().length * inputRate / rate;
        int markerOffset = (int) 1f * getMessageMarkerOffset() * inputRate / rate;
        double i = initOffset + markerOffset;
        int index = 0;
        int numOfResults = 16;
        int[] result = new int[numOfResults];
        while (index < 16) {
            result[index] = (int) i;
            i = i + interval;
            index++;
        }
        return result;
    }

    public int messageWithSignalSignalLenInMs() {
        return (getOneMessageSize() * 16 + startMessageSignals().length) * 1000 / rate();
    }
}
