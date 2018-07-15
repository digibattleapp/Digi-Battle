package com.digibattle.app.fragment;

import android.util.Log;
import android.view.View;

import com.digibattle.app.encoder.DigimonPendulumEncoder;

public class Digimon20Pendulum extends Digimon20Common {
    private static final String TAG = "Digimon20Pendulum";

    public Digimon20Pendulum() {
        super(new DigimonPendulumEncoder());
    }

    @Override
    public void init(View view) {
        super.init(view);
        setPlayerNameViewVisibility(false);
    }

    @Override
    protected String[] buildBattleMessage(boolean isSender, boolean doublePlay,
            boolean noRealBattle) {
        final String[] message = new String[10];

        // TODO: Device version, battle power
        String msg0Prefix;
        if (isSender) {
            msg0Prefix = "8";
        } else {
            msg0Prefix = "0";
        }
        if (doublePlay) {
            message[0] = msg0Prefix + "27e";
        } else {
            message[0] = msg0Prefix + "07e";
        }
        try {
            String monsterIdHex = getMonster1IdHex();
            monsterIdHex = String.format("%3s", monsterIdHex).replace(" ", "0");
            monsterIdHex = monsterIdHex.substring(0, 3);
            message[1] = monsterIdHex + "e";
        } catch (Exception e) {
            Log.e(TAG, "Bad id", e);
            return null;
        }
        // TODO: Attack icon 1
        message[2] = "049e";
        // TODO: Attack icon 2
        message[3] = "048e";
        try {
            String monsterPowerHex = getMonster1PowerHex();
            monsterPowerHex = String.format("%2s", monsterPowerHex).replace(" ", "0");
            monsterPowerHex = monsterPowerHex.substring(0, 2);
            message[4] = "0" + monsterPowerHex + "e";
        } catch (Exception e) {
            Log.e(TAG, "Bad power", e);
            return null;
        }
        if (doublePlay) {
            try {
                String monsterIdHex = getMonster2IdHex();
                monsterIdHex = String.format("%3s", monsterIdHex).replace(" ", "0");
                monsterIdHex = monsterIdHex.substring(0, 3);
                message[5] = monsterIdHex + "e";
            } catch (Exception e) {
                Log.e(TAG, "Bad id", e);
                return null;
            }
            // TODO: Attack icon 1
            message[6] = "000e";
            // TODO: Attack icon 2
            message[7] = "000e";
            try {
                String monsterPowerHex = getMonster2PowerHex();
                monsterPowerHex = String.format("%2s", monsterPowerHex).replace(" ", "0");
                monsterPowerHex = monsterPowerHex.substring(0, 2);
                message[8] = "0" + monsterPowerHex + "e";
            } catch (Exception e) {
                Log.e(TAG, "Bad power", e);
                return null;
            }
        } else {
            message[5] = "000e";
            message[6] = "000e";
            message[7] = "000e";
            message[8] = "000e";
        }

        if (!noRealBattle) {
            message[9] = "0100";
            char checksum = getDigimonMessageHelper().get20ThChecksumChar(message);
            // TODO: More control on battle result
            String verdictMessage = getComputerWins() ? "0fe" : "f0e";
            message[9] = checksum + verdictMessage;
        } else {
            message[9] = "ff00";
        }
        return message;
    }

    @Override
    public String[] buildCopyMessage() {
        final String[] message = new String[10];
        message[0] = "0101";
        message[1] = "0101";
        message[2] = "817e";
        try {
            String monsterIdHex = getMonster1IdHex();
            monsterIdHex = String.format("%3s", monsterIdHex).replace(" ", "0");
            monsterIdHex = monsterIdHex.substring(0, 3);
            message[3] = monsterIdHex + "e";
        } catch (Exception e) {
            Log.e(TAG, "Bad id", e);
            return null;
        }
        message[4] = "000e";
        message[5] = "000e";
        message[6] = "000e";
        message[7] = "000e";
        message[8] = "000e";
        message[9] = "000e";
        char checksum = getDigimonMessageHelper().get20ThChecksumChar(message);
        message[9] = checksum + "00e";
        return message;
    }

    @Override
    protected String getOppositeInfo(String[] messages, boolean isSender) {
        int offsetIndex = isSender ? 1 : 0;
        String info = "Monster id: " + extractLSBValue(messages[2 + offsetIndex], 2, 10) + "\n"
                + "Monster power: " + extractLSBValue(messages[8 + offsetIndex], 4, 12) + "\n"
                + "Monster2 id: " + extractLSBValue(messages[10 + offsetIndex], 2, 10) + "\n"
                + "Monster2 power: " + extractLSBValue(messages[16 + offsetIndex], 4, 12) + "\n"
                + "Battle power: " + extractLSBValue(messages[0 + offsetIndex], 2, 6) + "\n"
                + "Device version:" + extractLSBValue(messages[4 + offsetIndex], 8, 12) + "\n";
        return info;
    }
}
