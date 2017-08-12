package com.example.innocentevil.mediaprofiler;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.example.innocentevil.mediaprofiler.async.ThreadedTaskListener;
import com.example.innocentevil.mediaprofiler.media.FrequencyExtractor;
import com.example.innocentevil.mediaprofiler.media.MediaProfiler;
import com.example.innocentevil.mediaprofiler.renderer.SimpleColorRenderer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ThreadedTaskListener, MediaProfiler.AsyncThreadedTaskListener, FrequencyExtractor.AsyncThreadedTaskListener {

    private static final int TASK_ID_MEDIA_PROFILER = 0x01;
    private static final int REQUEST_PICK_AUDIO = 0x03;
    private static final int TASK_ID_FREQ_EXTRACT = 0x04;

    protected static String TAG = MainActivity.class.getCanonicalName();
    private Button mediaProfileActionBtn;
    private MediaProfiler mediaProfiler;
    private FrequencyExtractor mFrequencyExtractor;
    private SimpleColorRenderer mColorRenderer;
    private SurfaceView mSurfaceView;
    private boolean mediaProfileStarted;
    private short[] left;
    private short[] right;
    private static final int[] FREQ = {
            40,
            60,
            70,
            112,
            138,
            167,
            200,
            249,
            310,
            371,
            553,
            824,
            1000,
            1100,
            1228,
            1492,
            1620,
            1830,
            2200,
            2400,
            2727,
            3100,
            3450,
            3700,
            4063,
            5000,
            6054,
            7500,
            9020,
            13440,
            15020,
            18500,

       /*
            50,
            165,
            280,
            395,
            510,
            625,
            740,
            855,
            970,
            1085,
            1200,
            1315,
            1430,
            1545,
            1660,
            1775,
            1890,
            2005,
            2120,
            2235,
            2350,
            2465,
            2580,
            2695,
            2810,
            2925,
            3040,
            3155,
            3270,
            3385,
            3500,
            3615,
            3730,
            3845,
            3960,
            4075,
            4190,
            4305,
            4420,
            4535,
            4650,
            4765,
            4880,
            4995,
            5110,
            5225,
            */
    };

    private int channelCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mediaProfileStarted = false;
        mediaProfileActionBtn = (Button) findViewById(R.id.media_profile_action_btn);
        mediaProfileActionBtn.setOnClickListener(this);

        mediaProfiler = new MediaProfiler(TASK_ID_MEDIA_PROFILER);
        mColorRenderer = new SimpleColorRenderer(30);
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
    public void onThreadedTaskProgressUpdate(int taskId, int threadId, float progress) {
        switch (taskId) {
            case TASK_ID_MEDIA_PROFILER:
                break;
        }
    }

    @Override
    public void onFreqExtracted(float[] mag) {
//        mColorRenderer.setColor((int) ((mag[0] + mag[1] + mag[2] + mag[3] + mag[4] + mag[5] + mag[6] + mag[7]) * 355.0f),
//                (int) ((mag[8] + mag[9] + mag[10] + mag[11] + mag[12] + mag[13] + mag[14] + mag[15] + mag[16] + mag[17] + mag[18] + mag[19] + mag[20] + mag[21] + mag[22] + mag[23] + mag[24]) * 550.0f),
//                (int) ((mag[25] + mag[26] + mag[27] + mag[28] + mag[29] + mag[30] + mag[31] + mag[32] + mag[33] + mag[34] + mag[35] + mag[36] + mag[37] + mag[38] + mag[39] + mag[40]) * 550.0f));
                mColorRenderer.setColor((int) ((mag[0] + mag[1] + mag[2] + mag[3] + mag[4] + mag[5] + mag[6] + mag[7] + mag[8]) * 300.0f),
                (int) ((mag[8] + mag[9] + mag[10] + mag[11] + mag[12] + mag[13] + mag[14] + mag[15] + mag[16] + mag[17]) * 550.0f),
                (int) ((mag[17] + mag[18] + mag[19] + mag[20] + mag[21] + mag[22] + mag[23] + mag[24] + mag[25] + mag[26] + mag[27] + mag[28] + mag[29] + mag[30] + mag[31]) * 550.0f));
    }

    @Override
    public void onThreadedTaskStart(int taskId, int threadId) {
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
    public void onThreadedTaskStop(int taskId, int threadId) {
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
    public void onThreadedTaskResultAvailable(int taskId, int threadId, Bundle param) {

    }

    @Override
    public void onFormatUpdate(MediaFormat format) {
        channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
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
        if(channelCount > 1) {
            for (int i = 0; i < size >> 1; i++) {
                left[i] = left[i << 1];
                right[i] = left[(i << 1) + 1];
            }
            mFrequencyExtractor.write(left, size);
        } else {
            mFrequencyExtractor.write(left, size);
        }
    }
}
