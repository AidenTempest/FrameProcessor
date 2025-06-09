#include <jni.h>
#include <opencv2/opencv.hpp>

extern "C" {
    JNIEXPORT jint JNICALL
    Java_com_example_app_MainActivity_processFrame(JNIEnv *env, jobject thiz, jlong matAddr) {
        cv::Mat &mat = *(cv::Mat *) matAddr;
        // Example: Convert to grayscale
        cv::cvtColor(mat, mat, cv::COLOR_RGBA2GRAY);
        return 0;
    }
} 