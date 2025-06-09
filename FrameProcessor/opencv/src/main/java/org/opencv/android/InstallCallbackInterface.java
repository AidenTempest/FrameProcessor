package org.opencv.android;

public interface InstallCallbackInterface {
    public static final int NEW_INSTALLATION = 0;
    public static final int UPDATE_INSTALLATION = 1;

    public void install();
    public void cancel();
    public void wait_install();
} 