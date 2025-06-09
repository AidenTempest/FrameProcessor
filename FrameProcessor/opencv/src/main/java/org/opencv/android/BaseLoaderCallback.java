package org.opencv.android;

import android.content.Context;

public abstract class BaseLoaderCallback implements LoaderCallbackInterface {
    protected Context mAppContext;

    public BaseLoaderCallback(Context context) {
        mAppContext = context;
    }

    @Override
    public void onManagerConnected(int status) {
        switch (status) {
            case LoaderCallbackInterface.SUCCESS: {
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public void onPackageInstall(int operation, InstallCallbackInterface callback) {
        // Implementation will be provided by native code
    }
} 