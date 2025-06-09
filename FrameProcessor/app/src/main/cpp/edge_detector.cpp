#include <jni.h>
#include <opencv2/opencv.hpp>

using namespace cv;

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_frameprocessor_MainActivity_processFrame(JNIEnv *env, jobject thiz,
                                                          jlong input_mat_addr,
                                                          jlong output_mat_addr) {
    Mat &inputMat = *(Mat *) input_mat_addr;
    Mat &outputMat = *(Mat *) output_mat_addr;

    Mat gray;
    cvtColor(inputMat, gray, COLOR_RGBA2GRAY);

    GaussianBlur(gray, gray, Size(5, 5), 0);

    Canny(gray, outputMat, 50, 150);

    Mat dilated;
    Mat kernel = getStructuringElement(MORPH_RECT, Size(3, 3));
    dilate(outputMat, dilated, kernel);

    Mat bgrResult;
    cvtColor(dilated, bgrResult, COLOR_GRAY2BGR);
    bgrResult.setTo(Scalar(255, 255, 255), dilated);

    cvtColor(bgrResult, outputMat, COLOR_BGR2RGBA);
}

} // extern "C" 