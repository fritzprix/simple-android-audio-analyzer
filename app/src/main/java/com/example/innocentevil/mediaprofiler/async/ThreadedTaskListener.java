package com.example.innocentevil.mediaprofiler.async;

import android.os.Bundle;

/**
 * Created by innocentevil on 17. 7. 23.
 */
public interface ThreadedTaskListener {
    void onThreadedTaskProgressUpdate(int taskId, int threadId, float progress);

    void onThreadedTaskStart(int taskId, int threadId);

    void onThreadedTaskStop(int taskId, int threadId);

    void onThreadedTaskResultAvailable(int taskId, int threadId, Bundle param);
}
