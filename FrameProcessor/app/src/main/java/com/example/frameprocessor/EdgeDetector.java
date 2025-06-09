package com.example.frameprocessor;

import org.opencv.core.Mat;

public class EdgeDetector {
    static {
        System.loadLibrary("edge_detector");
    }

    private long nativeObj;

    public EdgeDetector() {
        nativeObj = nativeCreate();
    }

    public void processFrame(Mat input, Mat output) {
        nativeProcessFrame(nativeObj, input.getNativeObjAddr(), output.getNativeObjAddr());
    }

    public void release() {
        if (nativeObj != 0) {
            nativeDestroy();
            nativeObj = 0;
        }
    }

    private native long nativeCreate();
    private native void nativeDestroy();
    private native void nativeProcessFrame(long nativeObj, long input_mat_addr, long output_mat_addr);
} 