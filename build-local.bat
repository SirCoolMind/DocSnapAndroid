@echo off
rem Builds the debug APK in WSL, checks its permissions and ABIs, and opens its folder.
setlocal
set "WIN_DIR=%~dp0"
set "WIN_DIR_FWD=%WIN_DIR:\=/%"
for /f "delims=" %%i in ('wsl -d Ubuntu-24.04 -- wslpath -a "%WIN_DIR_FWD%"') do set "WSL_DIR=%%i"

wsl -d Ubuntu-24.04 -- bash -lc "cd '%WSL_DIR%' && export ANDROID_HOME=/opt/android-sdk JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew assembleDebug && bash scripts/check-apk.sh"
if errorlevel 1 (
    echo.
    echo Build or APK check failed.
    exit /b 1
)

explorer "%WIN_DIR%app\build\outputs\apk\debug"
endlocal
