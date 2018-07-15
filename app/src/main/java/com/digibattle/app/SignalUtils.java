package com.digibattle.app;

import java.math.BigInteger;

public class SignalUtils {

    private static final String TAG = "SignalUtils";

    // If it has transistor then we invert the output signal.
    // TODO: The logic without transistor might be wrong as I didn't test.
    public static final boolean HAS_TRANSISTOR = true;
    public static final short ANALOG_FROM_VOLTAGE_HIGH =
            HAS_TRANSISTOR ? Short.MIN_VALUE : Short.MAX_VALUE;
    public static final short ANALOG_FROM_VOLTAGE_LOW =
            HAS_TRANSISTOR ? Short.MAX_VALUE : Short.MIN_VALUE;

    public static boolean[] analog2Digital(short[] analog, int voltageChangeThreshold) {
        if (analog == null || analog.length == 0) {
            return null;
        }
        // We "guess" first signal is true if the first signal value > threshold.
        boolean firstSignal = analog[0] > DigiBattleConfig.voltageChangeThreshold;
        return analog2Digital(analog, voltageChangeThreshold, firstSignal);
    }

    public static boolean[] analog2Digital(short[] analog, int voltageChangeThreshold,
            boolean initValue) {
        if (analog == null || analog.length == 0) {
            return null;
        }
        boolean result[] = new boolean[analog.length];
        result[0] = initValue;
        for (int i = 0; i < analog.length - 1; i++) {
            int diff = ((int) analog[i + 1]) - analog[i];
            if (diff > voltageChangeThreshold) {
                result[i + 1] = true;
            } else if (diff < -voltageChangeThreshold) {
                result[i + 1] = false;
            } else {
                result[i + 1] = result[i];
            }
        }
        return result;
    }

    public static short[][] digital2Analog(boolean[][] digital) {
        short result[][] = new short[digital.length][];
        for (int i = 0; i < digital.length; i++) {
            result[i] = digital2Analog(digital[i]);
        }
        return result;
    }

    public static short[] digital2Analog(boolean[] digital) {
        short result[] = new short[digital.length];
        for (int i = 0; i < digital.length; i++) {
            result[i] = digital[i] ? ANALOG_FROM_VOLTAGE_HIGH : ANALOG_FROM_VOLTAGE_LOW;
            // Magic comes, it works unless it doesn't.
            // We cannot keep the output voltage always high / low in speaker output,
            // so what we are trying to do here is:
            // Do not set the value to max / min at the beginning of signal change, we set it as
            // INIT_RATIO * value and keep increasing / decreasing until it reaches max/min,
            // so we "hope" we can keep the output high/low voltage longer.
            if (HAS_TRANSISTOR) {
                if (i == 0 || digital[i - 1] != digital[i]) {
                    result[i] = (short) (result[i] * DigiBattleConfig.analogInitRatio);
                } else {
                    if (digital[i]) {
                        if (result[i - 1] > Short.MIN_VALUE + DigiBattleConfig.analogDelta) {
                            result[i] = (short) (result[i - 1] - DigiBattleConfig.analogDelta);
                        } else {
                            result[i] = result[i - 1];
                        }
                    } else {
                        if (result[i - 1] < Short.MAX_VALUE - DigiBattleConfig.analogDelta) {
                            result[i] = (short) (result[i - 1] + DigiBattleConfig.analogDelta);
                        } else {
                            result[i] = result[i - 1];
                        }
                    }
                }
            }
        }
        return result;
    }

    // Get all signals from start to end.
    public static short[] getPartition(short[] analog, int partitionStart, int partitionEnd) {
        if (analog == null || analog.length == 0) {
            return new short[1];
        }
        if (partitionStart >= partitionEnd) {
            return new short[1];

        }
        short[] result = new short[partitionEnd - partitionStart + 1];
        System.arraycopy(analog, partitionStart, result, 0, result.length);
        return result;
    }

    // Get all signals from start to end.
    public static boolean[] getPartition(boolean[] digital, int partitionStart, int partitionEnd) {
        if (digital == null) {
            return new boolean[0];
        }
        if (partitionStart > partitionEnd) {
            return new boolean[0];

        }
        boolean[] result = new boolean[partitionEnd - partitionStart + 1];
        System.arraycopy(digital, partitionStart, result, 0, result.length);
        return result;
    }

    // For debugging view purpose.
    public static short[] invertAnalog(short[] analog) {
        if (analog == null) {
            return null;
        }
        short[] result = new short[analog.length];
        for (int i = 0; i < analog.length; i++) {
            result[i] = analog[i] >= DigiBattleConfig.voltageChangeThreshold ? (short) (Short.MAX_VALUE
                    * 0.1)
                    : Short.MAX_VALUE;
        }
        return result;
    }

    public static String getLSBBoolString(String hexStr) {
        if (hexStr == null) {
            return "";
        }
        try {
            String binary = new BigInteger(hexStr, 16).toString(2);
            String msbStr = (String.format("%16s", binary).replace(" ", "0"));
            return new StringBuilder(msbStr).reverse().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return hexStr;
        }
    }
}
