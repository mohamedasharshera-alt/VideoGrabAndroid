# VideoGrab Android — MVP

Native Android Java application with an embedded browser, discovery of direct video file requests and HTML5 video URLs, link input, and Android DownloadManager downloads.

## Build an APK without a computer
1. Create a new GitHub repository and upload the project files (preserving folders, including `.github/workflows/build.yml`).
2. Open GitHub Actions > Build Android APK > Run workflow (or push to main).
3. When the run completes, download the `VideoGrab-debug-apk` artifact, unzip it, and install `app-debug.apk` on Android.

## Local build
Requires JDK 17, Android SDK 35 and Gradle 8.9. Run `gradle assembleDebug`.

## Limitations
Only direct HTTPS video files such as MP4/WebM are supported in this first build. HLS/DASH manifests, encrypted media, DRM, blob URLs, and protected/segmented streams are not supported. Some sites require headers or tokens that expire, so detection does not guarantee download success. This is a development build and is not yet Play Store ready.

## Notes
The app uses targetSdk 28 for compatibility with DownloadManager's legacy public Downloads API; this must be modernized before release to the Play Store. Only download media you are authorized to save.
