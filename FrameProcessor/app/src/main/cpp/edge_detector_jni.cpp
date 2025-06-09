#include <jni.h>
#include <opencv2/opencv.hpp>
#include "edge_detector.h"

using namespace cv;

extern "C" {

// Global pointer to our edge detector
static EdgeDetector* detector = nullptr;

JNIEXPORT jlong JNICALL
Java_com_example_frameprocessor_EdgeDetector_nativeCreate(JNIEnv* env, jobject thiz) {
    if (detector == nullptr) {
        detector = new EdgeDetector();
    }
    return reinterpret_cast<jlong>(detector);
}

JNIEXPORT void JNICALL
Java_com_example_frameprocessor_EdgeDetector_nativeDestroy(JNIEnv* env, jobject thiz) {
    if (detector != nullptr) {
        delete detector;
        detector = nullptr;
    }
}

JNIEXPORT void JNICALL
Java_com_example_frameprocessor_EdgeDetector_nativeProcessFrame(
        JNIEnv* env, jobject thiz, jlong input_mat_addr, jlong output_mat_addr) {
    
    if (detector == nullptr) return;
    
    // Get the input and output matrices
    Mat& input = *(Mat*)input_mat_addr;
    Mat& output = *(Mat*)output_mat_addr;
    
    // Process the frame
    output = detector->processFrame(input);
}

} // extern "C" 