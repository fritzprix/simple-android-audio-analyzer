package com.example.innocentevil.mediaprofiler.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Locale;

/**
 * Created by innocentevil on 17. 5. 28.
 */

public class PlayerService extends Service implements Handler.Callback {

    protected static final String TAG = PlayerService.class.getCanonicalName();
    private static final int MSG_INIT = 0x0101;
    private static final int MSG_SET_DATA = 0x0102;
    private static final int MSG_START = 0x0103;
    private static final int MSG_STOP = 0x0104;
    private static final int MSG_PAUSE = 0x0105;
    private static final int MSG_RESUME = 0x0106;
    private static final int MSG_GET_CURRENT_POSITION = 0x0107;
    private static final int MSG_SEEK_TO = 0x0108;
    private static final int MSG_GET_STATE = 0x0109;

    private static final int RESULT_OK = 0;
    private static final int RESULT_NOK = -1;
    private static final java.lang.String KEY_MIME = "mime";
    private static final String UNKNOWN_TYPE = "Unknown";
    private static final java.lang.String KEY_SRC_TYPE = "src-type";
    private static final int SRC_LOCAL_FILE = 1;
    private static final int SRC_REMOTE_URI = 2;
    private static final String KEY_SRC = "src";
    private static final String NOT_READY = "Player is not ready [Current : %s]";

    enum State {
        INIT("", 0),
        READY("", 1),
        PLAY("", 2),
        PAUSE("", 3),
        STOP("", 4);

        private final String name;
        private final int value;

        private State(String name, int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    private static final String[] STATE_TEXT = {
            "STATE_INIT",
            "STATE_READY"
    };

    private HandlerThread mHandlerThread;
    private Messenger mServerMessenger;
    private Messenger mClientMessenger;

    private State mState;

    PlayerService() {
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mState = State.INIT;
        mServerMessenger = new Messenger(new Handler(mHandlerThread.getLooper(), this));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mServerMessenger.getBinder();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_INIT:
                mClientMessenger = (Messenger) msg.obj;
                msg = Message.obtain();
                msg.what = RESULT_OK;
                mState = State.READY;
                try {
                    mClientMessenger.send(msg);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
                return true;
            case MSG_GET_STATE:
                if(mState.ordinal() < State.READY.ordinal()) {
                    Log.e(TAG, "Player is not initialized");
                    return true;
                }
                return true;
            case MSG_SET_DATA:
                if(mState.ordinal() < State.READY.ordinal()) {
                    Log.e(TAG, "Player is not initialized");
                    return true;
                }
                Bundle data = msg.getData();
                final String mime = data.getString(KEY_MIME, UNKNOWN_TYPE);
                final int srcType = data.getInt(KEY_SRC_TYPE, SRC_LOCAL_FILE);
                if(mime.equals(UNKNOWN_TYPE)) {
                    msg = Message.obtain();
                    msg.what = RESULT_NOK;
                    try {
                        mClientMessenger.send(msg);
                    } catch (RemoteException e) {
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                    return true;
                }
                switch (srcType) {
                    case SRC_LOCAL_FILE:
                        ParcelFileDescriptor pfd  = data.getParcelable(KEY_SRC);
                        break;
                    case SRC_REMOTE_URI:
                        break;
                }

                return true;
            case MSG_START:
                if(mState.ordinal() < State.READY.ordinal()) {
                    Log.e(TAG, String.format(Locale.getDefault(), NOT_READY, mState));
                }
                return true;
            case MSG_STOP:
                return true;
            case MSG_PAUSE:
                return true;
            case MSG_RESUME:
                return true;
            case MSG_GET_CURRENT_POSITION:
                return true;
            case MSG_SEEK_TO:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandlerThread.quitSafely();
    }
}
