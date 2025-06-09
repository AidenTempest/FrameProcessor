package org.opencv.android;

public interface LoaderCallbackInterface {
    public static final int SUCCESS = 0;
    public static final int INIT_FAILED = 1;
    public static final int INSTALL_CANCELED = 2;
    public static final int MARKET_ERROR = 3;
    public static final int INCOMPATIBLE_MANAGER_VERSION = 4;
    public static final int INIT_FAILED_MANAGER_NOT_INITIALIZED = 5;

    public void onManagerConnected(int status);
    public void onPackageInstall(int operation, InstallCallbackInterface callback);
} 