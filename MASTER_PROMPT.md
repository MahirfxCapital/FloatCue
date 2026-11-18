# FLOATCUE — THE SIGNATURE FLOATING TELEPROMPTER
## Award-winning Android product-development master prompt

**Product promise:** Your words. Your screen. Your flow.

Act as an expert Android platform engineer, product designer, motion and typography specialist, privacy engineer, localization architect and release QA engineer. Build a professional native Android teleprompter that floats over compatible apps so creators can read speeches while recording on camera, presenting, or streaming.

### 1. Product pillars
- A true user-granted Android application overlay using `TYPE_APPLICATION_OVERLAY` / `SYSTEM_ALERT_WINDOW`, **not** a PWA or website wrapper.
- Movable, resizable, collapsible, transparent reading window. Keep other app controls reachable outside the bounds of the overlay.
- Frame-smooth elapsed-time scrolling with pause, resume, restart, customizable speed and font size. Add countdown, bookmarks and reading progress in subsequent releases.
- Multiple offline scripts with paste, typing, editing, share-to-import and .txt import. Add undo, duplicate, pin, search, sort and rich editor in later milestones.
- Unicode, Ethiopic script, Afaan Oromo, English, Arabic, RTL and mixed-script support; UI translations should only be claimed after translation resources ship.
- No mandatory account, internet, ads, camera, microphone, accessibility service, screen capture or invasive permissions for the core product.
- Clear disclosures: some camera and social-media apps block overlays, especially protected screens. **Never bypass** system or platform protections.

### 2. Signature design
Brand: **FloatCue**. Near-black graphite `#10121B`, electric violet `#9269FC`, pearl `#F8F8FF`, mist gray `#AEB4CD`. Calm, premium hierarchy, comfortable touch targets, high-quality type, restrained motion, dark and light mode parity.
Home headline: **Speak naturally. Read beautifully.**
The editor should be distraction-free with a large script field, quick paste, import, save and library functions. Overlay: rounded translucent reading card with top drag handle, compact playback strip, subtle resize grip and one-tap collapse. Bubble must be draggable and tap-to-restore.

### 3. Native architecture
- Modern Android with explicit "Display over other apps" special-access onboarding.
- Foreground `Service` declared using a legitimate current service type and visible notification, started after user action.
- `FLAG_NOT_FOCUSABLE` and conservatively bounded windows; touch-through **outside** the overlay, never an unsafe bypass inside it.
- Persistent local script/settings storage, lifecycle cleanup, rotation and screen-inset handling.
- Time-based scrolling rather than word jumps. Restore scroll position on pause.
- No network permission in the offline MVP. Never monitor other apps or log user scripts.
- Android API 26+; use Java/native views for the zero-dependency starter. Kotlin + Compose/MVVM/Room/DataStore can follow after functional validation.
- Overlay text is generally not included in direct camera recording, but **can appear in screen recordings or screen sharing**. Verify with each app/device.

### 4. MVP functions
Create, edit, save and delete multiple scripts. Paste from clipboard only after user action; import .txt through Android document picker and import shared text. Adjustable scroll speed, text size, opacity, play/pause, restart, drag/resize/minimize/restore and mirrored reading option. Explicit start/stop. A GitHub Actions debug APK build with clear outcome.

### 5. Phase 2 — signature creator edition
Full editorial UI polish, history, autosave, rich paragraph controls, script search/favorites, countdown, reading progress and per-script presets. Proper localized app interface in Amharic, Afaan Oromo and English. DOCX/PDF text extraction via system picker (no unrestricted storage permission). Optional Bluetooth remote and landscape layouts.

### 6. Phase 3 — professional edition
Optional offline speech-following **only with explicit microphone permission and user-initiated mode**, with user-facing warning about competition for the microphone with camera apps. Optional encrypted cloud sync, script export, creator profiles and advanced accessibility. No surprise paywalls for the current core experience.

### 7. Quality gates
- Install / upgrade on Android 12, 14, 15 and 16. No repeated uninstall to update.
- Test camera, Facebook Reels, TikTok, Instagram and YouTube individually; report exactly where overlays are blocked.
- Long scripts, screen rotation, large fonts, RTL/Ethiopic punctuation, background/foreground service changes, process death, and no camera crashes.
- Verify the overlay closes cleanly, does not retain accidental screen capture permissions, and preserves scripts after upgrades.
- Test accessibility, content descriptions, contrast, minimum touch targets, 500-word scrolling and performance.
- GitHub Action must pass before saying an APK was built. Device testing and production signing are separate release gates. Never commit signing keys.

**Definition of done:** real functional interactions, no deceptive compatibility claims, no permission hacks, no fake release statuses, and an independently installable Android APK after CI and QA succeed.
