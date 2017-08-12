package com.example.innocentevil.mediaprofiler.media;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.example.innocentevil.mediaprofiler.async.AbsAsyncMultiTask;
import com.example.innocentevil.mediaprofiler.async.ThreadedTaskListener;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Locale;

import static android.content.ContentValues.TAG;

/**
 * Created by innocentevil on 17. 5. 9.
 */

public class MediaProfiler extends AbsAsyncMultiTask {

    private static final String KEY_MEDIA_FILED = "media-file-desc";
    private static final String KEY_IS_PLAYBACK = "media-is-play";
    private static final long TIMEOUT_DEFAULT = 1000L;
    private ByteBuffer writeBuffer;
    private MediaExtractor mediaExtractor;
    private AudioTrack mAudioTrack;
    private MediaCodec codec;
    private MediaFormat outFormat;
    private boolean isPlayback;
    private boolean isEos;
    private WeakReference<AsyncThreadedTaskListener> wrCallback;

    public interface AsyncThreadedTaskListener extends ThreadedTaskListener {
        void onThreadedTaskProgressUpdate(int taskId, int threadId, float progress);
        void onThreadedTaskStart(int taskId, int threadId);
        void onThreadedTaskStop(int taskId, int threadId);
        void onFormatUpdate(MediaFormat format);
        void onDataAvailable(ByteBuffer writeBuffer, int size, long presentationTimeUs);
    }

    public MediaProfiler(int taskId) {
        super(taskId, 2);
    }

    public void setCallback(AsyncThreadedTaskListener callback) {
        super.setThreadedTaskListener(callback);
        wrCallback = new WeakReference<AsyncThreadedTaskListener>(callback);
    }

    public void setMediaFile(FileDescriptor fileDescriptor, boolean playback) {
        Bundle param = new Bundle();
        try {
            ParcelFileDescriptor pfd = ParcelFileDescriptor.dup(fileDescriptor);
            param.putBoolean(KEY_IS_PLAYBACK, playback);
            param.putParcelable(KEY_MEDIA_FILED, pfd);
            setInitParam(0, param);
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }


    @Override
    protected boolean doSetup(int threadId, Bundle param) {
        switch (threadId) {
            case 0:
                ParcelFileDescriptor pfd = param.getParcelable(KEY_MEDIA_FILED);
                isPlayback = param.getBoolean(KEY_IS_PLAYBACK);
                if(pfd == null) {
                    return false;
                }
                FileDescriptor sourceFd = pfd.getFileDescriptor();
                mediaExtractor = new MediaExtractor();
                try {
                    mediaExtractor.setDataSource(sourceFd);
                    mediaExtractor.selectTrack(0);
                    MediaFormat format = mediaExtractor.getTrackFormat(0);
                    final String mimeType = format.getString(MediaFormat.KEY_MIME);
                    if(mimeType == null) {
                        return false;
                    }
                    codec = MediaCodec.createDecoderByType(mimeType);
                    codec.configure(format, null, null, 0);
                    codec.start();
                    outFormat = codec.getOutputFormat();
                    Log.e(TAG, codec.getName());
                    final int channelCount = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    isEos = false;
                    if(isPlayback) {
                        int channleOut = channelCount > 1? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
                        int sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                        int pcmDepth;
                        if(outFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            pcmDepth = outFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
                        } else {
                            pcmDepth = AudioFormat.ENCODING_PCM_16BIT;
                            outFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT);
                        }
                        // TODO : Only Stereo is supported, please add support for mono media audio track
                        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, pcmDepth);
                        Log.e(TAG, String.format(
                                Locale.getDefault(),
                                        "Sample Rate : %d \n" +
                                        "PCM Depth : %d\n" +
                                        "Min Buffer Size : %d\n",
                                sampleRate, pcmDepth, bufferSize));
                        writeBuffer = ByteBuffer.allocate(bufferSize);
                        writeBuffer.position(0);
                        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channleOut, pcmDepth, bufferSize, AudioTrack.MODE_STREAM);
                        mAudioTrack.play();
                    }
                    Log.e(TAG, String.format(Locale.getDefault(), "Setup @ %d", threadId));
                } catch (IOException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                    return false;
                }
                return true;
            case 1:
                Log.e(TAG, String.format(Locale.getDefault(), "Setup @ %d", threadId));
                return true;
        }
        return false;
    }

    @Override
    protected void doCleanup(int threadId) {
        Log.e(TAG, "Cleanup");
        switch (threadId) {
            case 0:
                codec.stop();
                codec.release();
                mediaExtractor.release();
                if(isPlayback) {
                    mAudioTrack.stop();
                    mAudioTrack.release();
                }
                break;
        }
    }

    @Override
    protected long doYield(int threadId) {
        return 0L;
    }

    @Override
    protected boolean doJob(int threadId) {
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH) {
            switch (threadId) {
                case 0:
                    ByteBuffer[] inBuffers = codec.getInputBuffers();
                    int ibIdx = codec.dequeueInputBuffer(TIMEOUT_DEFAULT);
                    int readSize;
                    if(ibIdx < 0) {
                        return false;
                    } else {
                        ByteBuffer iBuf = inBuffers[ibIdx];
                        if ((readSize = mediaExtractor.readSampleData(iBuf, 0)) < 0) {
                            isEos = true;
                            return true;
                        } else {
                            Log.d(TAG, String.format(Locale.getDefault(), "Read Size %d", readSize));
                            codec.queueInputBuffer(ibIdx, 0, readSize, mediaExtractor.getSampleTime(), 0);
                            mediaExtractor.advance();
                            return false;
                        }
                    }
                case 1:
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    ByteBuffer[] outBuffer = codec.getOutputBuffers();
                    int obIdx = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_DEFAULT);
                    if(obIdx < 0) {
                        Log.e(TAG, String.format(Locale.getDefault(),"Return code : %d", obIdx));
                        switch (obIdx) {
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                return false;
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                return isEos;
                        }
                    } else {
                        ByteBuffer oBuf = outBuffer[obIdx];
                        writeBuffer.position(0);
                        writeBuffer.put(oBuf);
                        wrCallback.get().onDataAvailable(writeBuffer, bufferInfo.size, bufferInfo.presentationTimeUs);
                        writeBuffer.position(0);
                        mAudioTrack.write(writeBuffer.array(), 0, bufferInfo.size);
                        codec.releaseOutputBuffer(obIdx, false);
                        return false;
                    }
            }
        } else {
            switch (threadId) {
                case 0:
                    ByteBuffer[] inBuffers = codec.getInputBuffers();
                    int ibIdx = codec.dequeueInputBuffer(TIMEOUT_DEFAULT);
                    int readSize;
                    if(ibIdx < 0) {
                        return false;
                    } else {
                        ByteBuffer iBuf = inBuffers[ibIdx];
                        if ((readSize = mediaExtractor.readSampleData(iBuf, 0)) < 0) {
                            isEos = true;
                            return true;
                        } else {
                            Log.d(TAG, String.format(Locale.getDefault(), "Read Size %d", readSize));
                            codec.queueInputBuffer(ibIdx, 0, readSize, mediaExtractor.getSampleTime(), 0);
                            mediaExtractor.advance();
                            return false;
                        }
                    }
                case 1:
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    ByteBuffer[] outBuffer = codec.getOutputBuffers();
                    int obIdx = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_DEFAULT);
                    if(obIdx < 0) {
                        Log.e(TAG, String.format(Locale.getDefault(),"Return code : %d", obIdx));
                        switch (obIdx) {
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                wrCallback.get().onFormatUpdate(codec.getOutputFormat());
                                return false;
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                return isEos;
                        }
                    } else {
                        ByteBuffer oBuf = outBuffer[obIdx];
                        writeBuffer.position(0);
                        writeBuffer.put(oBuf);
                        wrCallback.get().onDataAvailable(writeBuffer, bufferInfo.size, bufferInfo.presentationTimeUs);
                        writeBuffer.position(0);
                        mAudioTrack.write(writeBuffer, bufferInfo.size, AudioTrack.WRITE_BLOCKING);
                        codec.releaseOutputBuffer(obIdx, false);
                        return false;
                    }
            }
        }
        return true;
    }

    @Override
    public void onTaskResultAvailable(Bundle result) {

    }

}
