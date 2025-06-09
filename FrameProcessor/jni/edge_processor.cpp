#include <jni.h>
#include <opencv2/opencv.hpp>

extern "C" {

JNIEXPORT jbyteArray JNICALL
Java_com_example_app_EdgeProcessor_processFrame(JNIEnv *env, jobject, jbyteArray input, jint width, jint height) {
    // Convert jbyteArray to cv::Mat
    jbyte *inputData = env->GetByteArrayElements(input, nullptr);
    cv::Mat inputMat(height, width, CV_8UC4, inputData); // Assuming RGBA format

    // Convert to grayscale
    cv::Mat grayMat;
    cv::cvtColor(inputMat, grayMat, cv::COLOR_RGBA2GRAY);

    // Apply Canny edge detection
    cv::Mat edges;
    cv::Canny(grayMat, edges, 50, 150);

    // Convert back to RGBA
    cv::Mat outputMat;
    cv::cvtColor(edges, outputMat, cv::COLOR_GRAY2RGBA);

    // Release input data
    env->ReleaseByteArrayElements(input, inputData, JNI_ABORT);

    // Create output jbyteArray
    jbyteArray output = env->NewByteArray(outputMat.total() * outputMat.elemSize());
    env->SetByteArrayRegion(output, 0, outputMat.total() * outputMat.elemSize(), (jbyte *) outputMat.data);

    return output;
}

} // extern "C" 