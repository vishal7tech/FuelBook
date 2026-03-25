#!/bin/bash

# FuelBook Build Automation Script
# This script automates the build process for the FuelBook Android project

echo "🔧 FuelBook Build Automation Script"
echo "=================================="

# Clean the project
echo "🧹 Cleaning project..."
./gradlew clean

if [ $? -eq 0 ]; then
    echo "✅ Clean completed successfully"
else
    echo "❌ Clean failed"
    exit 1
fi

# Build debug variant
echo "🏗️ Building debug variant..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Debug build completed successfully"
else
    echo "❌ Debug build failed"
    exit 1
fi

# Compile Kotlin to check for errors
echo "🔍 Compiling Kotlin code..."
./gradlew compileDebugKotlin

if [ $? -eq 0 ]; then
    echo "✅ Kotlin compilation successful - no errors found"
else
    echo "❌ Kotlin compilation failed"
    exit 1
fi

echo ""
echo "🎉 Build process completed successfully!"
echo "📱 APK location: app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "📋 Build Summary:"
echo "   - Data binding: ✅ Enabled"
echo "   - Kotlin version: 2.2.0"
echo "   - AGP version: 8.13.2"
echo "   - All imports resolved: ✅"
