package com.example.innocentevil.mediaprofiler.media;

import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.innocentevil.mediaprofiler.async.AbsAsyncMultiTask;
import com.example.innocentevil.mediaprofiler.async.ThreadedTaskListener;

import java.lang.ref.WeakReference;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by innocentevil on 17. 5. 21.
 */

public class FrequencyExtractor extends AbsAsyncMultiTask {


    private static class Tone {
        private static float PCM_MAX_16B = (1 << 15);
        private int sin[];
        private int cos[];
        private int freq;
        private float normalizeFactor;

        private Tone(int freq, int sampleRate, int maxSize) {
            double unitTime = 1.0 / (double) sampleRate;
            this.freq = freq;
            sin = new int[maxSize];
            cos = new int[maxSize];
            normalizeFactor = (PCM_MAX_16B * PCM_MAX_16B) / 4;
            for (int i = 0; i < maxSize; i++) {
                sin[i] = (int)(PCM_MAX_16B * ((Math.sin(2 * Math.PI * freq * unitTime * i) / 2.0)));
                cos[i] = (int)(PCM_MAX_16B * ((Math.cos(2 * Math.PI * freq * unitTime * i) / 2.0)));
            }
        }

        private int real(short[] buffer, int size) {
            int real = 0;
            for (int i = 0; i < size; i++) {
                real += (buffer[i] * cos[i]);
            }
            return real;
        }

        private int image(short[] buffer, int size) {
            int img = 0;
            for (int i = 0; i < size; i++) {
                img += (buffer[i] * sin[i]);
            }
            return img;
        }

        private void dot(short[] buffer, int size, Point result) {
            int img, real;
            img = real = 0;
            for (int i = 0; i < size; i++) {
                real += (buffer[i] * cos[i]);
                img += (buffer[i] * sin[i]);
            }
            result.set(real, img);
        }

        private float abs(short[] buffer, int size) {
            /*
             * possible max. value happens when the frequency are matched
             * pcm value
             */
            if(size <= 0) {
                return 0;
            }
            double img, real;
            img = real = 0;
            for (int i = 0; i < size; i++) {
                real += (buffer[i] * cos[i]);
                img += (buffer[i] * sin[i]);
            }
            real /= (normalizeFactor * size);
            img /= (normalizeFactor * size);
            return (float) Math.sqrt(real * real + img * img);
        }

        public int getFreq() {
            return freq;
        }
    }

    public interface AsyncThreadedTaskListener extends ThreadedTaskListener {
        @Override
        void onThreadedTaskResultAvailable(int taskId, int threadId, Bundle param);

        @Override
        void onThreadedTaskStop(int taskId, int threadId);

        @Override
        void onThreadedTaskStart(int taskId, int threadId);

        @Override
        void onThreadedTaskProgressUpdate(int taskId, int threadId, float progress);

        void onFreqExtracted(float[] mag);
    }

    protected static String TAG = FrequencyExtractor.class.getCanonicalName();
    private static final int QUEUE_SIZE = 10;

    private WeakReference<AsyncThreadedTaskListener> wrCallback;
    private float[] mags;
    private Tone[] mTones;
    private ArrayBlockingQueue<ShortBuffer> emptyQueue;
    private ArrayBlockingQueue<ShortBuffer> readyQueue;


    public FrequencyExtractor(int taskId,int sampleRate, int maxSize, int ...freqs) {
        super(taskId, 1);
        mTones = new Tone[freqs.length];
        mags = new float[freqs.length];
        wrCallback = new WeakReference<AsyncThreadedTaskListener>(null);
        Arrays.fill(mags, 0);
        for (int i = 0; i < mTones.length; i++) {
            mTones[i] = new Tone(freqs[i], sampleRate, maxSize);
        }
        emptyQueue = new ArrayBlockingQueue<ShortBuffer>(QUEUE_SIZE);
        readyQueue = new ArrayBlockingQueue<ShortBuffer>(QUEUE_SIZE);

        for (int i = 0; i < QUEUE_SIZE; i++) {
            emptyQueue.add(ShortBuffer.allocate(maxSize));
        }

    }

    public void setCallback(AsyncThreadedTaskListener callback) {
        super.setThreadedTaskListener(callback);
        wrCallback = new WeakReference<AsyncThreadedTaskListener>(callback);
    }

    @Nullable
    private ShortBuffer dequeueInputQueue() {
        try {
            return emptyQueue.poll(1000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getLocalizedMessage());
            return null;
        }
    }

    public void write(short[] pcm, int size) {
        ShortBuffer buffer = dequeueInputQueue();
        if(buffer == null) {
            return;
        }

        buffer.position(0);
        buffer.put(pcm, 0, size);
        queueOutputBuffer(buffer);
    }

    private void queueOutputBuffer(ShortBuffer buffer) {
        try {
            readyQueue.offer(buffer, 1000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    @Override
    public void onTaskResultAvailable(Bundle result) {

    }

    @Override
    protected boolean doSetup(int threadId, Bundle param) {
        return true;
    }

    @Override
    protected void doCleanup(int threadId) {

    }

    @Override
    protected long doYield(int threadId) {
        return 0;
    }

    @Override
    protected boolean doJob(int threadId) {

        ShortBuffer buffer = dequeueOutputBuffer();
        if (buffer == null) {
            return true;
        }
        int pos = buffer.position();
        buffer.position(0);
        for (int i = 0; i < mTones.length; i++) {
            mags[i] = mTones[i].abs(buffer.array(), pos);
        }
        wrCallback.get().onFreqExtracted(mags);
        buffer.position(0);
        queueInputBuffer(buffer);
        return false;
    }

    private void queueInputBuffer(ShortBuffer buffer) {
        try {
            emptyQueue.offer(buffer, 1000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG,e.getLocalizedMessage());
        }
    }

    private ShortBuffer dequeueOutputBuffer() {
        try {
            return readyQueue.poll(1000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return null;
    }
}
