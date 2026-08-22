@echo off
setlocal
echo ===================================================
echo  BharatConnect - Native Android APK Build Tool
echo ===================================================

set "JAVA_HOME=C:\Program Files\Java\jdk-19"
set "ANDROID_HOME=C:\Users\Vipin\AppData\Local\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"

cd /d "%~dp0android_native"

echo.
echo [1/3] Verifying Gradle and Tooling...
call gradlew.bat --version
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Gradle initialization failed.
    exit /b %ERRORLEVEL%
)

echo.
echo [2/3] Compiling Jetpack Compose Native APK...
call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    exit /b %ERRORLEVEL%
)

echo.
echo [3/3] Build Successful!
echo APK Generated at:
echo %~dp0android_native\app\build\outputs\apk\debug\app-debug.apk
echo.

if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    echo Checking for connected Android devices via ADB...
    adb devices
)

echo ===================================================
pause
