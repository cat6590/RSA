package com.ricedotwho.rsa.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StopWatch {
    private long start = -1L;

    public void start() {
        start = System.currentTimeMillis();
    }

    public long stop() {
        if (start == -1) return -1;
        long elapsed = System.currentTimeMillis() - start;
        start = -1;
        return elapsed;
    }

    public long auto() {
        if (start == -1) {
            start();
            return -1;
        }
        return stop();
    }
}
