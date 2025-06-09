cmake_minimum_required(VERSION 3.4.1)

# Add edge detector library
add_library(edge_detector SHARED
            edge_detector.cpp)

# Include OpenCV headers
include_directories(${CMAKE_SOURCE_DIR}/../../../../opencv-android-sdk/sdk/native/jni/include)

# Link OpenCV libraries
target_link_libraries(edge_detector
                     ${CMAKE_SOURCE_DIR}/../../../../opencv-android-sdk/sdk/native/libs/${ANDROID_ABI}/libopencv_java4.so
                     android
                     log) 