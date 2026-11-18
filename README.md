# FloatCue 🎙️ — Floating Teleprompter for Android

FloatCue is a native, offline-first Android teleprompter designed to float over compatible camera and social media apps. Read scripts confidently while recording without switching apps.

## Features (v0.1.0 starter)
- Movable, resizable, collapsible floating script window
- Smooth scrolling with pause/play, restart and speed control
- Adjustable font size, background opacity, optional mirrored text
- Write, copy/paste, share-to-import and import .txt scripts
- Save several scripts locally, with a script library
- Unicode support, including Amharic, Afaan Oromo, English and Arabic
- No account, internet, ads, tracking, camera permission or screen-recording permission

## Install / build
Requires Android 8+ (API 26). Android Studio with Android SDK 35 and Java 17, or use GitHub Actions: open the **Android APK** workflow under Actions; after a successful run, download the **FloatCue-debug-apk** artifact. This is a debug build, not a signed production release.

When you tap **Start floating teleprompter**, Android will ask for **Display over other apps** permission. Turn it on, reopen FloatCue, then start the overlay and open a compatible camera/social app. Some apps or protected screens block overlays; FloatCue cannot override them.

## Structure
`app/src/main/java/com/hisabtech/floatcue/` — `MainActivity.java`, `ScriptStore.java`, `OverlayService.java`
`app/src/main/AndroidManifest.xml` — overlay and foreground-service declarations
`.github/workflows/android.yml` — Gradle APK build and artifact

## Safety and privacy
Scripts are stored locally. Overlay services are user controlled and show a persistent notification. FloatCue does not intercept keystrokes or other apps' content. Android may block this overlay in some apps or camera recording modes.

## Next milestone
Accessibility testing, rotation/insets polish, RTL and Ethiopian typography checks, user-tunable themes, explicit compatibility matrix on actual devices, signed APK packaging and release QA.

© FloatCue contributors.
