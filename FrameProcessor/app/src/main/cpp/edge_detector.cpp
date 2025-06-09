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

    // Step 1: Grayscale
    Mat gray;
    cvtColor(inputMat, gray, COLOR_RGBA2GRAY);

    // Step 2: CLAHE (Contrast Enhancement)
    Ptr<CLAHE> clahe = createCLAHE();
    clahe->setClipLimit(2.0);
    Mat enhanced;
    clahe->apply(gray, enhanced);

    // Step 3: Gaussian Blur
    GaussianBlur(enhanced, enhanced, Size(5, 5), 0);

    // Step 4: Canny with adaptive thresholding
    Mat flat = enhanced.reshape(1, 1);
    std::vector<uchar> vec = enhanced.isContinuous() ? flat : flat.clone();
    std::nth_element(vec.begin(), vec.begin() + vec.size()/2, vec.end());
    double medianVal = vec[vec.size()/2];
    double lower = std::max(0.0, 0.66 * medianVal);
    double upper = std::min(255.0, 1.33 * medianVal);
    Canny(enhanced, outputMat, lower, upper);

    // Step 5: Morphological Closing
    Mat kernel = getStructuringElement(MORPH_RECT, Size(3, 3));
    morphologyEx(outputMat, outputMat, MORPH_CLOSE, kernel);

    // Step 6: Convert to BGR then RGBA
    Mat bgrResult;
    cvtColor(outputMat, bgrResult, COLOR_GRAY2BGR);
    bgrResult.setTo(Scalar(255, 255, 255), outputMat);
    cvtColor(bgrResult, outputMat, COLOR_BGR2RGBA);
}


} // extern "C" 