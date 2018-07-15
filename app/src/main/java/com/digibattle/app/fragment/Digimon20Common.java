package com.digibattle.app.fragment;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.digibattle.app.DigimonMessageHelper;
import com.digibattle.app.R;
import com.digibattle.app.encoder.DigimonMessageEncoder;

import java.math.BigInteger;

public abstract class Digimon20Common extends Fragment {

    private static final String TAG = "Digimon20Common";
    private static DigimonMessageHelper mDigimonMessageHelper;

    private TextView mPlayerName1;
    private TextView mPlayerName2;
    private TextView mPlayerName3;
    private TextView mPlayerName4;
    private View mPlayerNameLayout;

    private TextView mMonster1IdTextView = null;
    private TextView mMonster1PowerTextView = null;
    private TextView mMonster2IdTextView = null;
    private TextView mMonster2PowerTextView = null;

    private TextView mMyOutputTextView = null;
    private TextView mSignalReceivedTextView = null;
    private TextView mOppositeInfoTextView = null;

    private CheckBox mComputerWinsCheckBox = null;
    private CheckBox mBattle2v2CheckBox = null;

    private Button mReplyButton = null;
    private Button mSendButton = null;
    private Button mSendCopyButton = null;
    private Button mReplyWithoutBattleButton = null;
    private Button mStopButton = null;

    private View mAdvanceScreenLayout = null;

    private boolean mRunning = false;

    public Digimon20Common(DigimonMessageEncoder encoder) {
        mDigimonMessageHelper = new DigimonMessageHelper(encoder);
    }

    private class MessageListener implements Runnable {
        private String mMessage;

        private MessageListener(String message) {
            mMessage = message;
        }

        @Override
        public void run() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateOutput(new String[]{mMessage});
                }
            });
        }
    }


    protected abstract String[] buildBattleMessage(boolean isSender, boolean doublePlay,
            boolean noRealBattle);

    protected abstract String[] buildCopyMessage();

    protected abstract String getOppositeInfo(String[] messages, boolean isSender);

    protected DigimonMessageHelper getDigimonMessageHelper() {
        return mDigimonMessageHelper;
    }

    protected void updateOutput(final String[] output) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (output == null) {
                    mSignalReceivedTextView.setText("No output");
                    return;
                }
                String text = "";
                for (String s : output) {
                    text += s + " ";
                }
                mMyOutputTextView.setText(text);
            }
        });
    }

    protected void updateSignalReceived(final String[] signal) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (signal == null) {
                    mSignalReceivedTextView.setText("No signal");
                    return;
                }
                String text = "";
                for (String s : signal) {
                    text += s + " ";
                }
                mSignalReceivedTextView.setText(text);
            }
        });
    }

    protected boolean isValidResult(String[] messages) {
        if (messages == null) {
            return false;
        }
        for (String message : messages) {
            if (message.length() != 4) {
                return false;
            }
        }
        return true;
    }

    protected int extractLSBValue(String hexString, int startBoolIndex, int endBoolIndex) {
        String binary = new BigInteger(hexString, 16).toString(2);
        binary = (String.format("%16s", binary).replace(" ", "0"));
        String tmp = binary.substring(startBoolIndex, endBoolIndex);
        return Integer.parseInt(tmp, 2);
    }

    protected void updateOppositeInfo(final String info) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mOppositeInfoTextView.setText(info);
            }
        });
    }

    protected boolean getComputerWins() {
        return mComputerWinsCheckBox.isChecked();
    }

    protected boolean getIs2v2() {
        return mBattle2v2CheckBox.isChecked();
    }

    protected void init(View parent) {

        mMonster1IdTextView = parent.findViewById(R.id.monster_1_id);

        mMonster1PowerTextView = parent.findViewById(R.id.monster_1_power);

        mMonster2IdTextView = parent.findViewById(R.id.monster_2_id);

        mMonster2PowerTextView = parent.findViewById(R.id.monster_2_power);


        mComputerWinsCheckBox = parent.findViewById(R.id.computer_wins_checkbox);
        mBattle2v2CheckBox = parent.findViewById(R.id.battle_2v2_checkbox);

        mMyOutputTextView = parent.findViewById(R.id.my_output);
        mSignalReceivedTextView = parent.findViewById(R.id.received_signal);
        mOppositeInfoTextView = parent.findViewById(R.id.opposite_info);

        mPlayerName1 = parent.findViewById(R.id.player_id_1);
        mPlayerName2 = parent.findViewById(R.id.player_id_2);
        mPlayerName3 = parent.findViewById(R.id.player_id_3);
        mPlayerName4 = parent.findViewById(R.id.player_id_4);
        mPlayerNameLayout = parent.findViewById(R.id.player_name_layout);

        mAdvanceScreenLayout = parent.findViewById(R.id.advance_screen_layout);

        mReplyButton = parent.findViewById(R.id.reply_btn);
        mReplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRunning = true;
                updateUI();
                new Thread() {
                    public void run() {
                        onReplyClicked();
                        mRunning = false;
                        updateUI();
                    }
                }.start();
            }
        });

        mSendButton = parent.findViewById(R.id.send_btn);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRunning = true;
                updateUI();
                new Thread() {
                    public void run() {
                        onSendClicked();
                        mRunning = false;
                        updateUI();
                    }
                }.start();
            }
        });

        mStopButton = parent.findViewById(R.id.stop_btn);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDigimonMessageHelper.stop();
            }
        });

        mSendCopyButton = parent.findViewById(R.id.send_copy_btn);
        mSendCopyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRunning = true;
                updateUI();
                new Thread() {
                    public void run() {
                        onSendCopyClicked();
                        mRunning = false;
                        updateUI();
                    }
                }.start();
            }
        });

        mReplyWithoutBattleButton = parent.findViewById(R.id.reply_without_battle_btn);
        mReplyWithoutBattleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRunning = true;
                updateUI();
                new Thread() {
                    public void run() {
                        onReplyWithoutBattleClicked();
                        mRunning = false;
                        updateUI();
                    }
                }.start();
            }
        });

        Button advancedButton = parent.findViewById(R.id.show_hide_advance_btn);
        advancedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAdvancedButtonClicked();
            }
        });

        FragmentManager fm = getChildFragmentManager();
        AdvancedScreen advanceScreen = (AdvancedScreen) fm.findFragmentById(
                R.id.advance_screen_fragment);
        advanceScreen.setDigimonMessageHelper(mDigimonMessageHelper);

        updateUI();
    }

    protected String getMonster1IdHex() {
        try {
            return Integer.toString(Integer.parseInt(mMonster1IdTextView.getText().toString()) * 4,
                    16);
        } catch (Exception e) {
            e.printStackTrace();
            return "10";
        }
    }

    protected String getMonster1PowerHex() {
        try {
            return Integer.toString(
                    Integer.parseInt(mMonster1PowerTextView.getText().toString()) * 4, 16);
        } catch (Exception e) {
            e.printStackTrace();
            return "10";
        }
    }

    protected String getMonster2IdHex() {
        try {
            return Integer.toString(Integer.parseInt(mMonster2IdTextView.getText().toString()) * 4,
                    16);
        } catch (Exception e) {
            e.printStackTrace();
            return "10";
        }
    }

    protected String getMonster2PowerHex() {
        try {
            return Integer.toString(
                    Integer.parseInt(mMonster2PowerTextView.getText().toString()) * 4, 16);
        } catch (Exception e) {
            e.printStackTrace();
            return "10";
        }
    }

    protected void updateUI() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSendCopyButton.setEnabled(!mRunning);
                mSendButton.setEnabled(!mRunning);
                mReplyButton.setEnabled(!mRunning);
                mStopButton.setEnabled(mRunning);
                mReplyWithoutBattleButton.setEnabled(!mRunning);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_digimon20_common, container, false);
        init(view);
        return view;
    }

    protected String getPlayerId1Hex() {
        return getPlayerIdHex(mPlayerName1);
    }

    protected String getPlayerId2Hex() {
        return getPlayerIdHex(mPlayerName2);
    }

    protected String getPlayerId3Hex() {
        return getPlayerIdHex(mPlayerName3);
    }

    protected String getPlayerId4Hex() {
        return getPlayerIdHex(mPlayerName4);
    }

    private String getPlayerIdHex(TextView textView) {
        String hex;
        try {
            hex = Integer.toString(Integer.parseInt(textView.getText().toString()), 16);
        } catch (Exception e) {
            hex = "10";
        }

        hex = String.format("%2s", hex).replace(" ", "0");
        hex = hex.substring(0, 2);
        return hex;
    }

    private int setMaxVolume() {
        AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        int currentVlaue = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxValue = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, maxValue, 0);
        return currentVlaue;
    }

    private void restoreVolume(int volume) {
        AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    public void onSendClicked() {
        clearAllResult();
        int currentVolume = setMaxVolume();
        updateOutput(new String[]{"Init..."});
        final String[] message = buildBattleMessage(true, getIs2v2(), false);
        String[] result = getDigimonMessageHelper().sendDigimonMessage(message,
                new MessageListener("Sending signal...")).hexMsg;
        updateOutput(message);
        updateSignalReceived(result);
        if (isValidResult(result)) {
            updateOppositeInfo(result, true);
        }
        restoreVolume(currentVolume);
    }

    public void onReplyClicked() {
        clearAllResult();
        int currentVolume = setMaxVolume();
        updateOutput(new String[]{"Init..."});
        final String[] message = buildBattleMessage(false, getIs2v2(), false);
        String[] result = getDigimonMessageHelper().replyDigimonMessage(message,
                new MessageListener("Waiting signal..."),
                new MessageListener("Replying...")).hexMsg;
        updateOutput(message);
        updateSignalReceived(result);
        if (isValidResult(result)) {
            updateOppositeInfo(result, false);
        }
        restoreVolume(currentVolume);
    }

    public void onReplyWithoutBattleClicked() {
        clearAllResult();
        int currentVolume = setMaxVolume();
        updateOutput(new String[]{"Init..."});
        final String[] message = buildBattleMessage(false, getIs2v2(), true);
        String[] result = getDigimonMessageHelper().replyDigimonMessage(message,
                new MessageListener("Waiting signal..."),
                new MessageListener("Replying...")).hexMsg;
        updateOutput(message);
        updateSignalReceived(result);
        if (isValidResult(result)) {
            updateOppositeInfo(result, false);
        }
        restoreVolume(currentVolume);
    }

    public void onSendCopyClicked() {
        clearAllResult();
        int currentVolume = setMaxVolume();
        updateOutput(new String[]{"Init..."});
        String[] message = buildCopyMessage();
        String[] result = getDigimonMessageHelper().sendDigimonMessage(message,
                new MessageListener("Sending signal...")).hexMsg;
        updateOutput(message);
        updateSignalReceived(result);
        restoreVolume(currentVolume);
    }

    private void clearAllResult() {
        updateOutput(null);
        updateSignalReceived(null);
        updateOppositeInfo(null, true);
    }

    private void updateOppositeInfo(String[] messages, boolean isSender) {
        if (messages == null) {
            updateOppositeInfo("");
            return;
        }
        String info = getOppositeInfo(messages, isSender);
        updateOppositeInfo(info);
    }

    protected void setPlayerNameViewVisibility(boolean visible) {
        mPlayerNameLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void onAdvancedButtonClicked() {
        boolean visibleNow = mAdvanceScreenLayout.getVisibility() == View.VISIBLE;
        mAdvanceScreenLayout.setVisibility(visibleNow ? View.GONE : View.VISIBLE);
    }

}
