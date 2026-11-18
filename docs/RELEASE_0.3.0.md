# FloatCue 3.0 — Creator Experience Release Gate

Version: **0.3.0** (Android package `com.hisabtech.floatcue`, versionCode **3**).

## Ten implemented upgrade tracks

1. **Smart creator dashboard** — launch/stop controls, overlay permission status, library shortcut and quick new-script action.
2. **Cinematic themes** — Midnight, Studio violet, and Paper reading window with appropriate text contrast.
3. **Focus line** — optional eye-level horizontal guide, with vertical position adjustable from Style.
4. **Countdown** — optional three-second lead-in before initial playback or restart.
5. **Progress and ETA** — percent-complete indicator and estimated remaining scrolling time.
6. **Autosave** — approximately 850 ms debounce while typing and lifecycle save on pause/back.
7. **Portable backups** — TXT export for one script; JSON backup/restore for the full library with validation, explicit overwrite confirmation, and previous-copy retention.
8. **Accessible floating controls** — enlarged touch targets, clearer controls and tap-to-restore floating bubble, with notification Stop action.
9. **International reading support** — automatic, forced LTR and forced RTL text-direction options, font scaling, support for Unicode and Ethiopic text.
10. **Performance and lifecycle** — avoid resetting the reading text on every style adjustment, persist per-script progress, elapsed-time animation, screen bounds and configuration-change clamps.

## Verification required

- [ ] Android GitHub Actions `assembleDebug` succeeds.
- [ ] The `FloatCue-debug-apk` artifact is uploaded and inspected as an APK.
- [ ] Test downgrade and upgrade paths carefully: CI debug signatures may differ across runs. **Never uninstall a version with unsaved scripts just to upgrade.**
- [ ] Confirm script text is preserved across restarts, file picker, rotation and backgrounding.
- [ ] Verify countdown cancel, play/pause/restart, scroll-to-end, manual gestures and progress restoration.
- [ ] Verify all 3 theme contrast modes and focus-position slider on multiple screens.
- [ ] Test Arabic RTL + Ethiopic and mixed-language reading with real scripts.
- [ ] Verify TXT export and JSON restore, including malformed and oversized JSON.
- [ ] Test camera overlay behavior in device native camera, TikTok, Facebook and Instagram — no universal compatibility promises.
- [ ] Validate accessibility target sizes and talkback content descriptions in a device audit.
- [ ] Complete production release signing and stable upgrade process before calling this a production release.

## Privacy/security

The app does not request network, camera, microphone or accessibility-service permissions. Exported TXT and JSON contain user-authored text and become user-managed files after saving. Keep those backups private. No platform protection bypass.
