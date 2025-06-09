package com.example.frameprocessor;

public class EdgeProcessor {
    static {
        System.loadLibrary("native-lib");
    }

    public native byte[] processFrame(byte[] input, int width, int height);
} 