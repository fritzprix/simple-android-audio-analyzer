package com.example.innocentevil.mediaprofiler.renderer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.example.innocentevil.mediaprofiler.async.AbsAsyncTask;

/**
 * Created by innocentevil on 17. 5. 23.
 */

public class FreqRenderer extends AbsAsyncTask implements SurfaceHolder.Callback {

    private Surface mSurface;
    private int red, green, blue;
    public FreqRenderer(int taskId) {
        super(taskId);
        red = green = blue = 0;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurface = holder.getSurface();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            stop();
        } catch (InterruptedException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    @Override
    protected boolean doSetup(Bundle param) {
        return true;
    }

    @Override
    protected void doCleanup() {

    }

    @Override
    protected long doYield() {
        return 10L;
    }

    public void setColor(int r, int g, int b) {
        r &= 255;
        g &= 255;
        b &= 255;
//        Log.e(TAG , String.format(Locale.getDefault(), "%d %d %d" , r,g,b));
        red = r;
        green = g;
        blue = b;
    }

    @Override
    protected boolean doJob(Bundle param) {
        Canvas c = mSurface.lockCanvas(null);
        c.drawColor(Color.rgb(red, green, blue));
        mSurface.unlockCanvasAndPost(c);
        return false;
    }
}
