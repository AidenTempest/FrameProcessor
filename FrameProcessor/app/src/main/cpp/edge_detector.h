#ifndef EDGE_DETECTOR_H
#define EDGE_DETECTOR_H

#include <opencv2/opencv.hpp>

class EdgeDetector {
public:
    EdgeDetector();
    ~EdgeDetector();

    // Process a frame and return the edge-detected result
    cv::Mat processFrame(const cv::Mat& input);

private:
    cv::Mat gray;
    cv::Mat edges;
    cv::Mat result;
};

#endif // EDGE_DETECTOR_H 