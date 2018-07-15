package com.digibattle.app.view;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceView;

import java.util.LinkedList;

/**
 * A view that displays audio data on the screen as a waveform.
 */
public class WaveformView extends SurfaceView {

    // The number of buffer frames to keep around (for a nice fade-out visualization).
    private static final int HISTORY_SIZE = 1;

    // To make quieter sounds still show up well on the display, we use +/- 8192 as the amplitude
    // that reaches the top/bottom of the view instead of +/- 32767. Any samples that have
    // magnitude higher than this limit will simply be clipped during drawing.
    private static final float MAX_AMPLITUDE_TO_DRAW = 32767f * 1.1f;

    // The queue that will hold historical audio data.
    private final LinkedList<short[]> mAudioData;

    private final LinkedList<boolean[]> mDigitalData;

    private int[] mMarkerPosition;

    private final Paint mPaint;

    public WaveformView(Context context) {
        this(context, null, 0);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mAudioData = new LinkedList<>();
        mDigitalData = new LinkedList<>();

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(0);
        mPaint.setAntiAlias(true);
    }

    public synchronized void updateDigitalData(boolean[] buffer) {
        if (buffer == null) {
            buffer = new boolean[1];
        }
        boolean[] newBuffer;

        // We want to keep a small amount of history in the view to provide a nice fading effect.
        // We use a linked list that we treat as a queue for this.
        if (mDigitalData.size() == HISTORY_SIZE) {
            newBuffer = mDigitalData.removeFirst();
            newBuffer = buffer.clone();
        } else {
            newBuffer = buffer.clone();
        }

        mDigitalData.addLast(newBuffer);

        // Update the display.
        Canvas canvas = getHolder().lockCanvas();
        if (canvas != null) {
            drawWaveform(canvas);
            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    /**
     * Updates the waveform view with a new "frame" of samples and renders it. The new frame gets
     * added to the front of the rendering queue, pushing the previous frames back, causing them to
     * be faded out visually.
     *
     * @param buffer the most recent buffer of audio samples
     */
    public synchronized void updateAudioData(short[] buffer) {
        if (buffer == null) {
            buffer = new short[1];
        }
        short[] newBuffer;

        // We want to keep a small amount of history in the view to provide a nice fading effect.
        // We use a linked list that we treat as a queue for this.
        if (mAudioData.size() == HISTORY_SIZE) {
            newBuffer = mAudioData.removeFirst();
            newBuffer = buffer.clone();
        } else {
            newBuffer = buffer.clone();
        }

        mAudioData.addLast(newBuffer);

        // Update the display.
        Canvas canvas = getHolder().lockCanvas();
        if (canvas != null) {
            drawWaveform(canvas);
            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    /**
     * Repaints the view's surface.
     *
     * @param canvas the {@link Canvas} object on which to draw
     */
    private void drawWaveform(Canvas canvas) {
        // Clear the screen each time because SurfaceView won't do this for us.
        canvas.drawColor(Color.BLACK);

        float width = getWidth();
        float height = getHeight();
        float centerY = height / 2;

        // We draw the history from oldest to newest so that the older audio data is further back
        // and darker than the most recent data.
        int colorDelta = 255;
        int brightness = colorDelta;

        for (short[] buffer : mAudioData) {
            if (buffer == null || buffer.length == 0) {
                continue;
            }
            mPaint.setColor(Color.WHITE);

            float lastX = -1;
            float lastY = -1;

            // For efficiency, we don't draw all of the samples in the buffer, but only the ones
            // that align with pixel boundaries.
            for (int x = 0; x < width; x++) {
                int index = (int) ((x / width) * buffer.length);
                short sample = buffer[index];
                float y = (-sample / MAX_AMPLITUDE_TO_DRAW) * centerY + centerY;

                if (lastX != -1) {
                    canvas.drawLine(lastX, lastY, x, y, mPaint);
                }

                lastX = x;
                lastY = y;
            }
            brightness += colorDelta;
        }

        if (mDigitalData != null) {
            for (boolean[] buffer : mDigitalData) {
                if (buffer == null || buffer.length == 0) {
                    continue;
                }
                mPaint.setColor(Color.GREEN);

                float lastX = -1;
                float lastY = -1;

                // For efficiency, we don't draw all of the samples in the buffer, but only the ones
                // that align with pixel boundaries.
                for (int x = 0; x < width; x++) {
                    int index = (int) ((x / width) * buffer.length);
                    short sample = buffer[index] ? Short.MAX_VALUE : Short.MIN_VALUE;
                    float y = (-sample / MAX_AMPLITUDE_TO_DRAW) * centerY + centerY;

                    if (lastX != -1) {
                        canvas.drawLine(lastX, lastY, x, y, mPaint);
                    }

                    lastX = x;
                    lastY = y;
                }
                brightness += colorDelta;
            }
        }

        if (mMarkerPosition != null) {
            mPaint.setColor(Color.RED);
            for (int p : mMarkerPosition) {
                canvas.drawLine(width * p / mAudioData.get(0).length, 0,
                        width * p / mAudioData.get(0).length, height, mPaint);
            }
        }
    }


    public void setSamples(short[] samples, boolean[] digitalSamples) {
        updateAudioData(samples);
        updateDigitalData(digitalSamples);
    }

    public void setMarkerPosition(int[] markerPosition) {
        mMarkerPosition = markerPosition;
    }
}