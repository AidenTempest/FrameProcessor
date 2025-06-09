# Real-Time Edge Detection Viewer

This Android app captures live camera feed, processes frames using OpenCV (C++) via JNI, and renders the output using OpenGL ES 2.0.

## Prerequisites

- Android Studio
- NDK and CMake (enabled in Android Studio)
- OpenCV SDK for Android (extracted to `OpenCV-android-sdk/` in the project root)

## Building the App

1. Open the project in Android Studio.
2. Sync Gradle files.
3. Build the project.

## Running the App

1. Connect an Android device or use an emulator.
2. Run the app from Android Studio.

## Testing

- The app should display the camera feed with edge detection applied in real-time.
- Ensure smooth performance (10-15 FPS minimum).

## Project Structure

- `app/`: Java/Kotlin code
- `jni/`: C++ OpenCV processing
- `gl/`: OpenGL renderer classes
- `CMakeLists.txt`: Build native code
- `build.gradle`: Configure OpenCV and NDK 