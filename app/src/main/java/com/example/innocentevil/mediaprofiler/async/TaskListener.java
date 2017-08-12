package com.example.innocentevil.mediaprofiler.async;

import android.os.Bundle;

/**
 * Created by innocentevil on 17. 5. 21.
 */

public interface TaskListener {
    void onTaskProgressUpdate(int taskId, float progress);
    void onTaskStart(int taskId);
    void onTaskStop(int taskId);
    void onTaskResultAvailable(Bundle result);
}
