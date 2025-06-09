cmake_minimum_required(VERSION 3.4.1)

project(FrameProcessor)

# Set OpenCV SDK path
set(OpenCV_DIR "E:/Programs/OpenCV-android-sdk/sdk/native/jni")
find_package(OpenCV REQUIRED)

# Add native library
add_library(native-lib SHARED
            jni/edge_processor.cpp)

# Link OpenCV libraries
target_link_libraries(native-lib
                      ${OpenCV_LIBS}
                      android
                      log) 