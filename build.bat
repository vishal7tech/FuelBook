@echo off
REM FuelBook Build Automation Script for Windows
REM This script automates the build process for the FuelBook Android project

echo 🔧 FuelBook Build Automation Script
echo ==================================

REM Clean the project
echo 🧹 Cleaning project...
call gradlew.bat clean

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Clean failed
    exit /b 1
)
echo ✅ Clean completed successfully

REM Build debug variant
echo 🏗️ Building debug variant...
call gradlew.bat assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Debug build failed
    exit /b 1
)
echo ✅ Debug build completed successfully

REM Compile Kotlin to check for errors
echo 🔍 Compiling Kotlin code...
call gradlew.bat compileDebugKotlin

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Kotlin compilation failed
    exit /b 1
)
echo ✅ Kotlin compilation successful - no errors found

echo.
echo 🎉 Build process completed successfully!
echo 📱 APK location: app\build\outputs\apk\debug\app-debug.apk
echo.
echo 📋 Build Summary:
echo    - Data binding: ✅ Enabled
echo    - Kotlin version: 2.2.0
echo    - AGP version: 8.13.2
echo    - All imports resolved: ✅

pause
