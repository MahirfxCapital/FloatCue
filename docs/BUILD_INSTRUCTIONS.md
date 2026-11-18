# Build-validation checklist

This branch exercises the GitHub Actions **Android APK** workflow from a pull request. It must compile before merging.

1. Verify GitHub Actions job concludes `success`.
2. Download `FloatCue-debug-apk` from the workflow run's Artifacts section.
3. Verify that `app-debug.apk` exists and installs on a compatible Android device.
4. Check script saving, system overlay permission and window dragging/resizing.
5. Verify Android 14/15/16 foreground-service behavior; TikTok/Facebook camera overlay support varies by app/device.
6. After debug validation, plan a secure, versioned, signed release. Never commit keystore material.

**Security reminder:** Set the repository visibility to **private** in GitHub Settings → Danger Zone. The GitHub connector does not expose the visibility mutation, and this file does not change that setting.
