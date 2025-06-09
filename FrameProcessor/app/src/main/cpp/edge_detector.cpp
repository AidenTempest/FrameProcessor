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

    // Convert to grayscale (assuming RGBA input from Android Bitmap)
    Mat gray;
    cvtColor(inputMat, gray, COLOR_RGBA2GRAY);

    // Apply Gaussian blur to reduce noise
    GaussianBlur(gray, gray, Size(5, 5), 0);

    // Apply Canny edge detection with adjusted thresholds for clearer edges
    Canny(gray, outputMat, 50, 150);

    // Dilate the edges to make them more visible
    Mat dilated;
    Mat kernel = getStructuringElement(MORPH_RECT, Size(3, 3));
    dilate(outputMat, dilated, kernel);

    // Convert back to BGR and make edges more visible
    Mat bgrResult;
    cvtColor(dilated, bgrResult, COLOR_GRAY2BGR);
    bgrResult.setTo(Scalar(255, 255, 255), dilated);

    // Convert BGR to RGBA for OpenGL
    cvtColor(bgrResult, outputMat, COLOR_BGR2RGBA);
}

} // extern "C"