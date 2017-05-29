package com.example.innocentevil.mediaprofiler.async;

import android.os.Bundle;

/**
 * Created by innocentevil on 17. 5. 9.
 */

interface Asynchronous {
    void start();
    void pause();
    void resume();
    void stop() throws InterruptedException;
    void reset();
}
