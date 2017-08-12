package com.example.innocentevil.mediaprofiler.renderer;

import android.graphics.Canvas;
import android.util.Log;

import java.util.Locale;

/**
 * Created by innocentevil on 17. 7. 24.
 */

public class SpectrumRenderer extends BaseRenderer {

    private static final float MAX_BAR_HEIGHT = 1.5f;
    private float[] mSpectrumValues;
    private int mWidth;
    private int mHeight;
    private float uWidth;


    public SpectrumRenderer(int fps, int freqCnt) {
        super(fps);
        mSpectrumValues = new float[freqCnt];
    }

    public synchronized void setValue(float ...values) {
        System.arraycopy(values, 0, mSpectrumValues, 0, mSpectrumValues.length);
    }

    @Override
    protected void onDraw(Canvas canvas) {

    }

    @Override
    protected synchronized void onCanvasChanged(int format, int width, int height) {
        mWidth = width;
        mHeight = height;
        Log.e(TAG, String.format(Locale.getDefault(), "Width : %d / Height : %d" , width, height));
    }
}
