package com.example.innocentevil.mediaprofiler.renderer;

import com.example.innocentevil.mediaprofiler.async.AbsAsyncTask;
import com.example.innocentevil.mediaprofiler.async.Callback;

import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by innocentevil on 17. 5. 25.
 */

public abstract class BaseRenderer extends AbsAsyncTask implements SurfaceHolder.Callback, Callback {

    private long WAIT_PERIOD;
    private SurfaceHolder mSurfaceHolder;

    public BaseRenderer(int fps) {
        super(0);
        setCallback(this);
        double fInterval = 1000.0 / fps;
        WAIT_PERIOD = (long) fInterval;

    }

    public void setSufaceView(SurfaceView view) {
        view.getHolder().addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(TAG, "Surface Changed");
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG, "Surface Created");
        start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        holder.removeCallback(this);
        try {
            stop();
            Log.e(TAG, "Surface Destroyed");
        }catch (InterruptedException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    @Override
    protected boolean doJob(Bundle param) {
        if(mSurfaceHolder == null) {
            return true;
        }
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if(canvas == null) {
            return true;
        }
        onDraw(canvas);
        mSurfaceHolder.unlockCanvasAndPost(canvas);
        return false;
    }

    protected abstract void onDraw(Canvas canvas);

    @Override
    protected boolean doSetup(Bundle param) {
        return true;
    }

    @Override
    protected long doYield() {
        return WAIT_PERIOD;
    }

    @Override
    protected void doCleanup() {

    }

    @Override
    public void onProgressUpdate(int taskId, float progress) {

    }

    @Override
    public void onStart(int taskId) {

    }

    @Override
    public void onStop(int taskId) {

    }

    @Override
    public void onResultAvailable(Bundle result) {

    }

    public void removeSurfaceView(SurfaceView mSurfaceView) {
        mSurfaceView.getHolder().removeCallback(this);
    }
}
