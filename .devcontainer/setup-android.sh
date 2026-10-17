#!/usr/bin/env bash
# Sahayak AI — Codespaces me Android build setup (free tier ke liye halka rakha)
set -e

echo "== 1/3: Android command-line tools download kar rahe hain =="
mkdir -p "$HOME/android-sdk/cmdline-tools"
cd /tmp
curl -fsSLo tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
unzip -q tools.zip -d cmdline-tools-tmp
mv cmdline-tools-tmp/cmdline-tools "$HOME/android-sdk/cmdline-tools/latest"
rm -rf tools.zip cmdline-tools-tmp

export ANDROID_HOME="$HOME/android-sdk"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"

echo "== 2/3: Android SDK packages install (10-15 min, progress dikhega) =="
yes | sdkmanager --licenses > /dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo ""
echo "== 3/3: local.properties likh rahe hain =="
cd "$OLDPWD"
echo "sdk.dir=$HOME/android-sdk" > local.properties
echo "local.properties likha gaya (gitignored hai)."

echo ""
echo "SETUP COMPLETE! Ab build karo:"
echo "  gradle assembleDebug"
echo "APK yahan banega: app/build/outputs/apk/debug/app-debug.apk"
