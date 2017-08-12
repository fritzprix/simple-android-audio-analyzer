package com.example.innocentevil.mediaprofiler.async;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.security.InvalidParameterException;

/**
 * Created by innocentevil on 17. 5. 9.
 */

public abstract class AbsAsyncTask implements Asynchronous, Runnable, Handler.Callback {



    protected static String TAG = AbsAsyncTask.class.getCanonicalName();

    private static final int MSG_ID_START = 0x01;
    private static final int MSG_ID_STOP = 0x02;
    private static final int MSG_ID_UPDATE = 0x03;
    private static final int MSG_ID_RESUME = 0x04;
    private static final int MSG_ID_PAUSE = 0x05;
    private static final int MSG_ID_RESULT_AVAILABLE = 0x06;

    private static final int STATE_INIT = 1;
    private static final int STATE_START = (1 << 2);
    private static final int STATE_RUN = (1 << 4);
    private static final int STATE_PAUSE = (1 << 5);
    private static final int STATE_FIN = 0;
    private int mState;

    private Thread asyncWorker;
    private Bundle initParam;
    private volatile boolean done;
    private Handler mainHandler;
    private float mProgress;
    private int taskId;
    private WeakReference<TaskListener> wrCallback;

    public AbsAsyncTask(int taskId) {
        mainHandler = new Handler(Looper.getMainLooper(), this);
        this.taskId = taskId;
        mProgress = 0.0f;
        mState = STATE_INIT;
        done = false;
        wrCallback = new WeakReference<TaskListener>(null);
    }


    @Override
    public synchronized void start() {
        if(mState >= STATE_START) {
            throw new IllegalStateException("Task has already started");
        }
        asyncWorker = new Thread(this, this.getClass().getCanonicalName());
        mState = STATE_START;
        asyncWorker.start();
        mProgress = 0.0f;
    }

    public void setTaskListener(TaskListener callback){
        wrCallback = new WeakReference<TaskListener>(callback);
    }

    @Override
    public synchronized void stop() throws InterruptedException {
        if(mState < STATE_START) {
            throw new IllegalStateException("Task has not started yet");
        }
        done = true;
        mState = STATE_FIN;
    }

    @Override
    public synchronized void pause() {
        if(mState > STATE_START) {
            return;
        }
        mState = STATE_PAUSE;
        mainHandler.sendEmptyMessage(MSG_ID_PAUSE);
    }

    @Override
    public synchronized void resume() {
        if(mState  != STATE_PAUSE) {
            return;
        }
        mState = STATE_RUN;
        mainHandler.sendEmptyMessage(MSG_ID_RESUME);
    }

    @Override
    public synchronized void reset() {
        mState = STATE_INIT;
    }

    @Override
    public void run() {
        long yield;
        mState = STATE_RUN;
        mainHandler.sendEmptyMessage(MSG_ID_START);
        done = !doSetup(initParam);
        try {
            while (!done) {
                if(mState == STATE_RUN) {
                    if (doJob(initParam)) {
                        done = true;
                    }
                } else {
                    Log.e(TAG, "Pause");
                }
                if ((yield = doYield()) > 0L) {
                    Thread.sleep(yield);
                } else {
                    Thread.yield();
                }
            }
        } catch (InterruptedException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } finally {
            mState = STATE_FIN;
            mainHandler.sendEmptyMessage(MSG_ID_STOP);
            doCleanup();
        }
    }

    protected void setInitParam(Bundle param) {
        initParam = new Bundle(param);
    }

    protected void updateProgress(float progress) {
        if((progress > 1.0f) && (progress < 0.0f)) {
            throw new InvalidParameterException("progress should be value from 0.0 to 1.0");
        }
        mProgress = progress;
        Message msg = Message.obtain();
        msg.what = MSG_ID_UPDATE;
        msg.arg1 = Float.floatToIntBits(progress);
        mainHandler.sendMessage(msg);
    }

    protected void setResult(Bundle result) {
        Message msg = Message.obtain();
        msg.setData(result);
        msg.what = MSG_ID_RESULT_AVAILABLE;
        mainHandler.sendMessage(msg);
    }

    protected int getTaskId() {
        return taskId;
    }

    @Override
    final public boolean handleMessage(Message msg) {
        TaskListener callback = wrCallback.get();
        if(callback == null) {
            return false;
        }
        switch (msg.what) {
            case MSG_ID_START:
                wrCallback.get().onTaskStart(taskId);
                return true;
            case MSG_ID_STOP:
                wrCallback.get().onTaskStop(taskId);
                return true;
            case MSG_ID_UPDATE:
                wrCallback.get().onTaskProgressUpdate(taskId, mProgress);
                return true;
            case MSG_ID_RESULT_AVAILABLE:
                wrCallback.get().onTaskResultAvailable(msg.getData());
                return true;
        }
        return false;
    }

    protected abstract boolean doSetup(Bundle param);

    protected abstract void doCleanup();

    protected abstract long doYield();

    protected abstract boolean doJob(Bundle param);
}
