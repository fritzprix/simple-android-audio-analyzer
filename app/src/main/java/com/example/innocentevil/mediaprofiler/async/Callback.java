package com.example.innocentevil.mediaprofiler.async;

import android.os.Bundle;

/**
 * Created by innocentevil on 17. 5. 21.
 */

public interface Callback {
    void onProgressUpdate(int taskId, float progress);
    void onStart(int taskId);
    void onStop(int taskId);
    void onResultAvailable(Bundle result);
}
