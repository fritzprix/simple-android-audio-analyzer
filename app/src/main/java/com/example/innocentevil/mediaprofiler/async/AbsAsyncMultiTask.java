package com.example.innocentevil.mediaprofiler.async;

import android.os.Bundle;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Created by innocentevil on 17. 5. 10.
 */

public abstract class AbsAsyncMultiTask implements Asynchronous, TaskListener {


    private class AsyncUnitTask extends AbsAsyncTask {
        private WeakReference<CyclicBarrier> wrBarrier;

        public AsyncUnitTask(int taskId, CyclicBarrier syncBarrier) {
            super(taskId);
            wrBarrier = new WeakReference<>(syncBarrier);
        }

        @Override
        protected boolean doSetup(Bundle param) {
            boolean setup = AbsAsyncMultiTask.this.doSetup(getTaskId(), param);
            try {
                wrBarrier.get().await();
            } catch (InterruptedException e) {
                Log.e(TAG, e.getLocalizedMessage());
                if(setup) {
                    doCleanup();
                }
            } catch (BrokenBarrierException e) {
                Log.e(TAG, e.getLocalizedMessage());
                if (setup) {
                    doCleanup();
                }
            }
            return setup;
        }

        @Override
        protected void doCleanup() {
            try {
                wrBarrier.get().await();
            } catch (InterruptedException e) {
                Log.e(TAG, e.getLocalizedMessage());
            } catch (BrokenBarrierException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
            AbsAsyncMultiTask.this.doCleanup(getTaskId());
        }

        @Override
        protected long doYield() {
            return AbsAsyncMultiTask.this.doYield(getTaskId());
        }

        @Override
        protected boolean doJob(Bundle param) {
            return AbsAsyncMultiTask.this.doJob(getTaskId());
        }
    }


    private LinkedList<AsyncUnitTask> asyncs;
    private int taskId;
    private WeakReference<ThreadedTaskListener> wrCallback;
    private CyclicBarrier mSyncBarrier;

    public AbsAsyncMultiTask(int taskId, int threadCount) {
        this.taskId = taskId;
        mSyncBarrier = new CyclicBarrier(threadCount);
        asyncs = new LinkedList<>();
        for (int i = 0; i < threadCount; i++) {
            AsyncUnitTask unitTask = new AsyncUnitTask(i, mSyncBarrier);
            unitTask.setTaskListener(this);
            asyncs.add(unitTask);
        }
        wrCallback = new WeakReference<ThreadedTaskListener>(null);
    }

    public void setThreadedTaskListener(ThreadedTaskListener asyncThreadedTaskListener){
        wrCallback = new WeakReference<ThreadedTaskListener>(asyncThreadedTaskListener);
    }


    @Override
    public synchronized void start() {
        for (AsyncUnitTask async : asyncs) {
            async.start();
        }
    }

    @Override
    public synchronized void pause() {
        for (AsyncUnitTask async : asyncs) {
            async.pause();
        }
    }

    @Override
    public synchronized void resume() {
        for (AsyncUnitTask async : asyncs) {
            async.resume();
        }
    }

    @Override
    public synchronized void stop() throws InterruptedException {
        for (AsyncUnitTask async : asyncs) {
            async.stop();
        }
    }

    @Override
    public synchronized void reset() {
        for (AsyncUnitTask async : asyncs) {
            async.reset();
        }
    }

    protected void setInitParam(int threadId, Bundle param) {
        asyncs.get(threadId).setInitParam(param);
    }

    protected int getTaskId() {
        return taskId;
    }

    @Override
    public void onTaskProgressUpdate(int taskId, float progress) {
        final ThreadedTaskListener asyncThreadedTaskListener = wrCallback.get();
        if(asyncThreadedTaskListener == null) {
            return;
        }
        asyncThreadedTaskListener.onThreadedTaskProgressUpdate(getTaskId(), taskId, progress);
    }

    @Override
    public void onTaskStart(int taskId) {
        final ThreadedTaskListener asyncThreadedTaskListener = wrCallback.get();
        if(asyncThreadedTaskListener == null) {
            return;
        }
        asyncThreadedTaskListener.onThreadedTaskStart(getTaskId(), taskId);
    }

    @Override
    public void onTaskStop(int taskId) {
        final ThreadedTaskListener asyncThreadedTaskListener = wrCallback.get();
        if(asyncThreadedTaskListener == null) {
            return;
        }
        asyncThreadedTaskListener.onThreadedTaskStop(getTaskId(), taskId);
    }

    protected abstract boolean doSetup(int threadId, Bundle param);

    protected abstract void doCleanup(int threadId);

    protected abstract long doYield(int threadId);

    protected abstract boolean doJob(int threadId);
}
