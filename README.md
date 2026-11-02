# DailyFocus

DailyFocus is a native Android app for daily vocabulary practice, quick personal reflection, and a lightweight daily routine. It keeps everything on-device, works offline, and helps users focus on one small set of tasks each day.

## Overview

The app combines a rotating daily vocabulary lesson with four saved activity prompts. Users can review the day’s content, edit their activities, and optionally enable a lock-screen reminder experience that surfaces the daily focus screen when they first wake their device. it also supports witdget display of the vocabulary so that user doesn't have to struggle with opening of the app all the time. beecause one can see each vocabulary from the witdget.

## Features

### Daily vocabulary

- Offline vocabulary practice with a bundled local library
- A rotating daily selection that stays fixed for the current local date
- No internet access required

### Personal focus activities

- Four editable activity descriptions stored locally
- A dedicated edit screen to update them at any time
- Shared values across the app’s main and daily views

### Read-only daily focus screen

- Displays three vocabulary cards and all four activities
- Keeps the layout clean and distraction-free
- Designed for quick daily review without editing controls

### Optional lock-screen focus

- Best-effort reminder that tries to show today’s focus when the device wakes
- Designed to appear before unlock, without bypassing device security
- Uses Android platform constraints, permissions, and device policies

### Home-screen widget

- Shows the current day’s vocabulary card set
- Opens the app from the widget
- Works offline and reads the same daily selection as the app

## Project details

- Language: Kotlin
- Platform: Android
- minSdk: 26
- compileSdk: 35
- targetSdk: 35
- Storage model: local device storage only
- Network access: not required

## Getting started

### Prerequisites

- Android Studio
- Android SDK and Gradle sync configured
- Java JDK bundled with Android Studio or a compatible installed version

### Build the app

Open the project in Android Studio and let Gradle sync, or build it from PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleDebug
```

The debug APK is generated here:

```text
app\build\outputs\apk\debug\app-debug.apk
```

### Build a release APK

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleRelease
```

Release builds are unsigned until a signing key is configured.

## Install on a device

1. Enable Developer options on the Android device.
2. Turn on USB debugging.
3. Connect the device to the computer with a USB cable and unlock it.
4. Accept the USB debugging prompt.
5. Install the APK:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

If you do not want to use USB debugging, copy the APK to the phone, open it from the Files app, and approve the installation prompt.

## First-use flow

1. Open DailyFocus.
2. Review the four saved activity entries on the main screen.
3. Tap Edit Activities to update them and save the changes.
4. Open the daily focus view to review the day’s vocabulary and tasks.
5. Optional: enable Lock Screen Focus and grant the requested permissions.

## Permissions and platform notes

The optional reminder feature may request:

- Display over other apps
- Notification permission
- Foreground service visibility
- Boot permission for best-effort restoration

These permissions are used to support the lock-screen reminder flow and do not enable online services or network access.

Android behavior around background activity launches varies by OS version, manufacturer, and battery management policy. The app deliberately avoids unsupported patterns such as floating overlays and full-screen alarm intents. Because of those platform constraints, the lock-screen reminder should be treated as best-effort and validated on real hardware.

## Testing

The project includes unit tests for:

- daily vocabulary rotation logic
- activity description save/reload behavior
- vocabulary content validation

Several tests use an injected date provider and in-memory storage so the daily rotation logic can be verified without altering the installed app’s stored data.

## Limitations

- No online dictionary integration
- No downloaded-word cache
- No remote sync or account system
- Lock-screen behavior depends on device policy and Android restrictions
- Final verification should be done on a real device

## Contributing

This project is a local, privacy-focused daily utility app. Changes should stay aligned with the app’s goals: offline reliability, clear daily focus, and Android compatibility.

## License

This project does not currently include a license file. If you plan to distribute or reuse it publicly, add a suitable open-source license before doing so.

# caution 
for anyone seeing this on GitHub, This app is 100% vibe-coded with AI.
i built it just to solve my problem with vocabulary. and it is a perfect one. if you are using android, you may have to give it a try and give me feedback. thank you 