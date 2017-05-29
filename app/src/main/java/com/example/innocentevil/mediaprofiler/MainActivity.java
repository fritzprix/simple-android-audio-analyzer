package com.example.innocentevil.mediaprofiler;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.example.innocentevil.mediaprofiler.async.AbsAsyncMultiTask;
import com.example.innocentevil.mediaprofiler.async.Callback;
import com.example.innocentevil.mediaprofiler.media.FrequencyExtractor;
import com.example.innocentevil.mediaprofiler.media.MediaProfiler;
import com.example.innocentevil.mediaprofiler.renderer.FreqRenderer;
import com.example.innocentevil.mediaprofiler.renderer.SimpleColorRenderer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AbsAsyncMultiTask.Callback, MediaProfiler.Callback, FrequencyExtractor.Callback {

    private static final int TASK_ID_MEDIA_PROFILER = 0x01;
    private static final int REQUEST_PICK_AUDIO = 0x03;
    private static final int TASK_ID_FREQ_EXTRACT = 0x04;

    protected static String TAG = MainActivity.class.getCanonicalName();
    private Button mediaProfileActionBtn;
    private MediaProfiler mediaProfiler;
    private FrequencyExtractor mFrequencyExtractor;
//    private FreqRenderer mFreqRenderer;
    private SimpleColorRenderer mColorRenderer;
    private SurfaceView mSurfaceView;
    private boolean mediaProfileStarted;
    private short[] left;
    private short[] right;
    private static final int[] FREQ = {
            50,
            75,
            112,
            167,
            249,
            371,
            553,
            824,
            1228,
            1830,
            2727,
            4063,
            6054,
            9020,
            13440,
            20000
    };

    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mediaProfileStarted = false;
        mediaProfileActionBtn = (Button) findViewById(R.id.media_profile_action_btn);
        mediaProfileActionBtn.setOnClickListener(this);

        mediaProfiler = new MediaProfiler(TASK_ID_MEDIA_PROFILER);
        mColorRenderer = new SimpleColorRenderer(60);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView = (SurfaceView) findViewById(R.id.profile_presentation_sv);
        mColorRenderer.setSufaceView(mSurfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mColorRenderer.removeSurfaceView(mSurfaceView);
    }


    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.media_profile_action_btn:
                if(!mediaProfileStarted) {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType(MediaStore.Audio.Media.CONTENT_TYPE);
                    startActivityForResult(intent, REQUEST_PICK_AUDIO);
                } else {
                    try {
                        mediaProfiler.stop();
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PICK_AUDIO:
                if(resultCode == RESULT_OK) {
                    Uri sourceUri = data.getData();
                    try {
                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(sourceUri, "r");
                        if (pfd == null) {
                            return;
                        }
                        mediaProfiler.setMediaFile(pfd.getFileDescriptor(), true);
                        mediaProfiler.setCallback(this);
                        mediaProfiler.start();
                    } catch (IOException e) {
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                }
                break;
        }
    }


    @Override
    public void onProgressUpdate(int taskId, int threadId, float progress) {
        switch (taskId) {
            case TASK_ID_MEDIA_PROFILER:
                break;
        }
    }

    @Override
    public void onFreqExtracted(int... mag) {
        mColorRenderer.setColor(
                ((mag[0] >> 3) + (mag[1] >> 3) + (mag[2] >> 3) + (mag[3] >> 3) + (mag[4] >> 4)) >> 1,
                ((mag[4] >> 4) + (mag[5] >> 3) + (mag[6] >> 3) + (mag[7] >> 4)) >> 1,
                ((mag[7] >> 4) + (mag[8] >> 2) + (mag[9] >> 2) + (mag[10] >> 2) + (mag[11] >> 2) + (mag[12] >> 2) +(mag[13] >> 2) + (mag[14] >> 2) + (mag[15] >> 2)) >> 1);
    }

    @Override
    public void onStart(int taskId, int threadId) {
        Log.e(TAG, String.format(Locale.getDefault(), "On Start @ %d", taskId));
        switch (taskId) {
            case TASK_ID_MEDIA_PROFILER:
                mediaProfileStarted = true;
                mediaProfileActionBtn.setText(R.string.media_profile_action_label_stop);
                break;
            case TASK_ID_FREQ_EXTRACT:
                Log.e(TAG, "Freq Extractor Started");
                break;
        }
    }

    @Override
    public void onStop(int taskId, int threadId) {
        Log.e(TAG, String.format(Locale.getDefault(), "On Stop @ %d", taskId));
        switch (taskId) {
            case TASK_ID_MEDIA_PROFILER:
                if(threadId == 0) {
                    mediaProfileStarted = false;
                    mediaProfileActionBtn.setText(R.string.media_profile_action_label_start);
                    try {
                        mFrequencyExtractor.stop();
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                }
                break;
            case TASK_ID_FREQ_EXTRACT:
                Log.e(TAG, "Freq Extractor Stopped");
                break;
        }
    }

    @Override
    public void onResultAvailable(int taskId, int threadId, Bundle param) {

    }

    @Override
    public void onFormatUpdate(MediaFormat format) {
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int audioFormat = format.getInteger(MediaFormat.KEY_PCM_ENCODING);
        Log.e(TAG, String.format(Locale.getDefault(),
                "Sampling Rate : %d\n" +
                "PCM Encoding %d\n",
                sampleRate,
                audioFormat));
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, audioFormat);
        left = new short[bufferSize >> 1];
        right = new short[bufferSize >> 1];
        mFrequencyExtractor = new FrequencyExtractor(TASK_ID_FREQ_EXTRACT, sampleRate, bufferSize, FREQ);
        mFrequencyExtractor.setCallback(this);
        mFrequencyExtractor.start();
    }

    @Override
    public void onDataAvailable(ByteBuffer writeBuffer, int size, long presentationTimeUs) {
        writeBuffer.position(0);
        ShortBuffer pcmBuffer = writeBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        size >>= 1;
        pcmBuffer.get(left, 0, size);
        for (int i = 0; i < size >> 1; i++) {
            left[i] = left[i << 1];
            right[i] = left[(i << 1) + 1];
        }
        mFrequencyExtractor.write(left, size);
    }
}
