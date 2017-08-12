package com.example.innocentevil.mediaprofiler.renderer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;

import java.util.Locale;

/**
 * Created by innocentevil on 17. 5. 28.
 */

public class SimpleColorRenderer extends BaseRenderer {

    private volatile int red;
    private volatile int green;
    private volatile int blue;

    public SimpleColorRenderer(int fps) {
        super(fps);
    }

    public void setColor(int r, int g, int b) {
        red =  r > 255 ? 255 : r;
        green =  g > 255 ? 255 : g;
        blue =   b > 255 ? 255 : b;
//        Log.e(TAG, String.format(Locale.getDefault(),
//                "R : %d, G : %d , B : %d",
//                r, g, b));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.rgb(red, green, blue));
    }

    @Override
    protected void onCanvasChanged(int format, int width, int height) {

    }
}
