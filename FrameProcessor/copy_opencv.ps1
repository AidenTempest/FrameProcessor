# Create directories
New-Item -ItemType Directory -Force -Path "opencv/src/main/java"
New-Item -ItemType Directory -Force -Path "opencv/src/main/jniLibs"
New-Item -ItemType Directory -Force -Path "opencv/src/main/res"

# Copy Java files
Copy-Item "E:/Programs/OpenCV-android-sdk/sdk/src/main/java/*" -Destination "opencv/src/main/java" -Recurse

# Copy native libraries
Copy-Item "E:/Programs/OpenCV-android-sdk/sdk/src/main/jniLibs/*" -Destination "opencv/src/main/jniLibs" -Recurse

# Copy resources
Copy-Item "E:/Programs/OpenCV-android-sdk/sdk/src/main/res/*" -Destination "opencv/src/main/res" -Recurse

Write-Host "OpenCV files copied successfully!" 