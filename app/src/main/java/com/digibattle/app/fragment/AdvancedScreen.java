package com.digibattle.app.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.digibattle.app.DigiBattleSharedPrefs;
import com.digibattle.app.DigimonMessageHelper;
import com.digibattle.app.R;
import com.digibattle.app.SignalProcessor;
import com.digibattle.app.SignalUtils;
import com.digibattle.app.view.WaveformView;

import java.util.Arrays;

public class AdvancedScreen extends Fragment {

    private EditText[] mInputMsgEditText = new EditText[SignalProcessor.MAX_PARTITIONS_NUMBER / 2];
    private Button mSendMsgBtn;
    private Button mReplyMsgBtn;
    private Button mClearScreenBtn;
    private Button mPingBtn;
    private Button mStopBtn;
    private TextView mAdvancedStatusTextView;
    private WaveformView mRawAnalogWaveformView;
    private TextView[] mPartitionTextView = new TextView[SignalProcessor.MAX_PARTITIONS_NUMBER];
    private WaveformView[] mPartitionWaveformView =
            new WaveformView[SignalProcessor.MAX_PARTITIONS_NUMBER];
    private DigimonMessageHelper mDigimonMessageHelper;

    public AdvancedScreen() {
        // Required empty public constructor
    }

    private void restoreSavedMessage() {
        String savedMessage = DigiBattleSharedPrefs.getInstance(
                getContext()).getLastAdvancedMessages();
        for (int i = 0; i < mInputMsgEditText.length; i++) {
            mInputMsgEditText[i].setText(savedMessage.substring(i * 4, (i + 1) * 4));
        }
    }

    public void setDigimonMessageHelper(DigimonMessageHelper digimonMessageHelper) {
        mDigimonMessageHelper = digimonMessageHelper;
    }

    private class MessageListener implements Runnable {
        private String mMessage;

        private MessageListener(String message) {
            mMessage = message;
        }

        @Override
        public void run() {
            setAdvancedStatus(mMessage);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_digimon_advanced, container, false);
        mSendMsgBtn = view.findViewById(R.id.send_custom_msg_button);
        mSendMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    public void run() {
                        sendMsg();
                    }
                }.start();
            }
        });
        mReplyMsgBtn = view.findViewById(R.id.reply_custom_msg_button);
        mReplyMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    public void run() {
                        replyMsg();
                    }
                }.start();
            }
        });
        mClearScreenBtn = view.findViewById(R.id.clear_screen_button);
        mPingBtn = view.findViewById(R.id.ping_button);
        mRawAnalogWaveformView = view.findViewById(R.id.raw_analog_signal);
        mAdvancedStatusTextView = view.findViewById(R.id.advance_status);
        mStopBtn = view.findViewById(R.id.advanced_stop_btn);
        LinearLayout waveformLinearLayout = view.findViewById(R.id.waveform_linear_layout);
        for (int i = 0; i < SignalProcessor.MAX_PARTITIONS_NUMBER; i++) {
            TextView textView = new TextView(getContext());
            textView.setText("Partition " + (i + 1) + ":");
            waveformLinearLayout.addView(textView);
            mPartitionTextView[i] = textView;
            WaveformView waveformView = new WaveformView(getContext());
            waveformLinearLayout.addView(waveformView,
                    getResources().getDimensionPixelSize(R.dimen.waveform_width),
                    getResources().getDimensionPixelSize(R.dimen.waveform_height));
            mPartitionWaveformView[i] = waveformView;
        }
        LinearLayout messageInputLinearLayout = view.findViewById(R.id.message_input_layout);
        LinearLayout currentInputLinearLayout = null;
        for (int i = 0; i < SignalProcessor.MAX_PARTITIONS_NUMBER / 2; i++) {
            if (i % 5 == 0) {
                currentInputLinearLayout = new LinearLayout(getContext());
                currentInputLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
                messageInputLinearLayout.addView(currentInputLinearLayout);
            }
            EditText msgInputEditText = new EditText(getContext());
            msgInputEditText.setText("0000");
            currentInputLinearLayout.addView(msgInputEditText);
            mInputMsgEditText[i] = msgInputEditText;
        }
        mClearScreenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResult(null);
            }
        });
        mPingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    public void run() {
                        ping();
                    }
                }.start();
            }
        });
        mStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });

        restoreSavedMessage();
        return view;
    }

    private String[] getAllMsg() {
        int numberOfPartitions = 0;
        for (int i = 0; i < SignalProcessor.MAX_PARTITIONS_NUMBER / 2; i++) {
            if (TextUtils.isEmpty(mInputMsgEditText[i].getText().toString())) {
                break;
            }
            numberOfPartitions = i + 1;
        }
        String[] result = new String[numberOfPartitions];
        for (int i = 0; i < SignalProcessor.MAX_PARTITIONS_NUMBER / 2; i++) {
            result[i] = mInputMsgEditText[i].getText().toString();
        }
        return result;
    }

    private void sendMsg() {
        saveAdvancedMessages();
        String[] message = getAllMsg();
        DigimonMessageHelper.DigimonMessageResult result = mDigimonMessageHelper.sendDigimonMessage(
                message, new MessageListener("Sending signal..."));
        showResult(result);
    }

    private void replyMsg() {
        saveAdvancedMessages();
        String[] message = getAllMsg();
        DigimonMessageHelper.DigimonMessageResult result =
                mDigimonMessageHelper.replyDigimonMessage(message,
                        new MessageListener("Waiting signal..."),
                        new MessageListener("Replying..."));
        showResult(result);
    }

    private void saveAdvancedMessages() {
        String result = "";
        String[] allMsg = getAllMsg();
        for (String msg : allMsg) {
            if (TextUtils.isEmpty(msg)) {
                break;
            }
            result = result + msg;
        }
        DigiBattleSharedPrefs.getInstance(getContext()).setLastAdvancedMessages(result);
    }

    private void showResult(final DigimonMessageHelper.DigimonMessageResult result) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (result == null || result instanceof DigimonMessageHelper.ErrorResult) {
                    for (int i = 0; i < SignalProcessor.MAX_PARTITIONS_NUMBER; i++) {
                        mPartitionWaveformView[i].setSamples(null, null);
                        mPartitionWaveformView[i].setMarkerPosition(null);
                        mPartitionTextView[i].setText(null);
                    }
                    mRawAnalogWaveformView.setSamples(null, null);
                    mRawAnalogWaveformView.setMarkerPosition(null);
                    setAdvancedStatus("");
                    return;
                }
                for (int i = 0; i < SignalProcessor.MAX_PARTITIONS_NUMBER; i++) {
                    mPartitionTextView[i].setText("Partition " + i + ": " + result.hexMsg[i] + ", "
                            + SignalUtils.getLSBBoolString(result.hexMsg[i]));

                    short[] analogPartition = SignalUtils.getPartition(result.analogSignal,
                            result.partitionIndex[i][1], result.partitionIndex[i][2]);
                    boolean[] digiPartition = SignalUtils.getPartition(result.digitalSignal,
                            result.partitionIndex[i][1], result.partitionIndex[i][2]);
                    int[] markerPos = mDigimonMessageHelper.getMarkerPos(result.rate);

                    mPartitionWaveformView[i].setMarkerPosition(markerPos);
                    mPartitionWaveformView[i].setSamples(analogPartition, digiPartition);
                }
                // mRawAnalogWaveformView.setSamples(result.analogSignal, result.digitalSignal);
                mRawAnalogWaveformView.setSamples(result.analogSignal, null);
                int[] markerPos = new int[result.partitionIndex.length * 3];
                for (int j = 0; j < result.partitionIndex.length; j++) {
                    markerPos[j] = result.partitionIndex[j][0];
                    markerPos[j + 1] = result.partitionIndex[j][1];
                    markerPos[j + 2] = result.partitionIndex[j][2];
                }
                mRawAnalogWaveformView.setMarkerPosition(markerPos);
                String statusOutput = "";
                for (String s : result.hexMsg) {
                    statusOutput = statusOutput + " " + s;
                }
                setAdvancedStatus(statusOutput);
            }
        });
    }

    private void setAdvancedStatus(final String msg) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdvancedStatusTextView.setText(msg);
            }
        });
    }

    private void ping() {
        setAdvancedStatus("Start ping...");
        short[] pingPacket = new short[100];
        Arrays.fill(pingPacket, 0, pingPacket.length, Short.MAX_VALUE);
        SignalProcessor processor = new SignalProcessor();
        try {
            processor.sendAnalogSignal(new short[][]{pingPacket}, 48000, 1, 1, true, 0);
            boolean done = false;
            try {
                for (int i = 0; i < 30; i++) {
                    if (processor.isFinished() && processor.getRTT() > 1) {
                        done = true;
                        break;
                    }
                    Thread.sleep(100);
                }
            } catch (InterruptedException ignored) {
            }
            if (done) {
                long rtt = processor.getRTT();
                setAdvancedStatus("Ping result: " + rtt + "ms");
            } else {
                setAdvancedStatus("Ping failed :((((((");
            }
        } finally {
            processor.stop();
        }
    }

    private void stop() {
        mDigimonMessageHelper.stop();
    }

}
