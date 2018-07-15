package com.digibattle.app.fragment;

import android.util.Log;
import android.view.View;

import com.digibattle.app.encoder.DigimonOriginalEncoder;

public class Digimon20Original extends Digimon20Common {

    private static final String TAG = "Digimon20Original";

    public Digimon20Original() {
        super(new DigimonOriginalEncoder());
    }

    @Override
    public void init(View view) {
        super.init(view);
        setPlayerNameViewVisibility(true);
    }

    @Override
    protected String[] buildBattleMessage(boolean isSender, boolean doublePlay,
            boolean noRealBattle) {
        final String[] message = new String[noRealBattle ? 9 : 10];
        message[0] = getPlayerId2Hex() + getPlayerId1Hex();
        message[1] = getPlayerId4Hex() + getPlayerId3Hex();
        // TODO: Device version
        if (isSender) {
            message[2] = "8";
        } else {
            message[2] = "0";
        }
        if (doublePlay) {
            message[2] = message[2] + "22e";
        } else {
            message[2] = message[2] + "02e";
        }
        try {
            String monsterIdHex = getMonster1IdHex();
            monsterIdHex = String.format("%3s", monsterIdHex).replace(" ", "0");
            monsterIdHex = monsterIdHex.substring(0, 3);
            message[3] = monsterIdHex + "e";
        } catch (Exception e) {
            Log.e(TAG, "Bad id", e);
            return null;
        }
        // TODO: Attack icon
        message[4] = "000e";
        try {
            String monsterPowerHex = getMonster1PowerHex();
            monsterPowerHex = String.format("%2s", monsterPowerHex).replace(" ", "0");
            monsterPowerHex = monsterPowerHex.substring(0, 2);
            message[5] = "0" + monsterPowerHex + "e";
        } catch (Exception e) {
            Log.e(TAG, "Bad power", e);
            return null;
        }
        if (doublePlay) {
            try {
                String monsterIdHex = getMonster2IdHex();
                monsterIdHex = String.format("%3s", monsterIdHex).replace(" ", "0");
                monsterIdHex = monsterIdHex.substring(0, 3);
                message[6] = monsterIdHex + "e";
            } catch (Exception e) {
                Log.e(TAG, "Bad id", e);
                return null;
            }
            // TODO: Attack icon
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
            message[6] = "000e";
            message[7] = "000e";
            message[8] = "000e";
        }
        if (!noRealBattle) {
            // TODO: More control on battle result
            String verdictMessage = getComputerWins() ? "0fe" : "f0e";
            message[9] = "0" + verdictMessage;
            char checksum = getDigimonMessageHelper().get20ThChecksumChar(message);
            message[9] = checksum + verdictMessage;
        }
        return message;
    }

    @Override
    public String[] buildCopyMessage() {
        final String[] message = new String[10];
        message[0] = "0000";
        message[1] = "0000";
        message[2] = "812e";
        try {
            String monsterIdHex = getMonster1IdHex();
            monsterIdHex = String.format("%3s", monsterIdHex).replace(" ", "0");
            monsterIdHex = monsterIdHex.substring(0, 3);
            message[3] = monsterIdHex + "e";
        } catch (Exception e) {
            Log.e(TAG, "Bad id", e);
            return null;
        }
        message[4] = "959e";
        message[5] = "00fe";
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
        String info = "Monster id: " + extractLSBValue(messages[6 + offsetIndex], 0, 10) + "\n"
                + "Monster power: " + extractLSBValue(messages[10 + offsetIndex], 4, 12) + "\n"
                + "Monster2 id: " + extractLSBValue(messages[12 + offsetIndex], 0, 10) + "\n"
                + "Monster2 power: " + extractLSBValue(messages[16 + offsetIndex], 4, 12) + "\n"
                + "Battle power: " + extractLSBValue(messages[4 + offsetIndex], 1, 6) + "\n"
                + "Device version:" + extractLSBValue(messages[4 + offsetIndex], 10, 12) + "\n"
                + "Username:"
                + extractLSBValue(messages[0 + offsetIndex], 8, 16) + " "
                + extractLSBValue(messages[0 + offsetIndex], 0, 8) + " "
                + extractLSBValue(messages[2 + offsetIndex], 8, 16) + " "
                + extractLSBValue(messages[2 + offsetIndex], 0, 8);
        return info;
    }
}
