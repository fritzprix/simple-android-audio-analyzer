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
        red = 255 & r;
        green = 255 & g;
        blue = 255 & b;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.rgb(red, green, blue));
    }
}
