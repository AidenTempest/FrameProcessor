#include <jni.h>
#include <opencv2/opencv.hpp>

using namespace cv;

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_frameprocessor_MainActivity_processFrame(JNIEnv *env, jobject thiz,
                                                        jlong input_mat_addr,
                                                        jlong output_mat_addr) {
    // Get the input and output matrices
    Mat &inputMat = *(Mat *) input_mat_addr;
    Mat &outputMat = *(Mat *) output_mat_addr;

    // Convert to grayscale
    Mat gray;
    cvtColor(inputMat, gray, COLOR_BGR2GRAY);

    // Apply Gaussian blur to reduce noise
    GaussianBlur(gray, gray, Size(5, 5), 0);

    // Apply Canny edge detection
    Canny(gray, outputMat, 50, 150);

    // Convert back to BGR for display
    cvtColor(outputMat, outputMat, COLOR_GRAY2BGR);
}

} // extern "C" 