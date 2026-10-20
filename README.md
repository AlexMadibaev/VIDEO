# Seamless Loop TV

Minimal Android 6+ landscape video loop player. One video per screen. First launch prompts for a local video (USB/storage) using Android document picker. Long press or Back selects another. URI is remembered. Keeps screen awake and hides system bars.

Uses native MediaPlayer.setLooping(true) without recreating player between repeats to minimize visible gaps. **A fully frame-accurate gapless loop cannot be guaranteed on every device/codec.** For best results encode H.264 MP4 CFR (e.g. 25/30 fps) and make sure first/last frames join naturally.

## APK

Open repository **Actions > Build Android APK** and download artifact `SeamlessLoopTV-debug-apk` from successful run. Install included `app-debug.apk`. Android 6 (API 23) or newer.

Branch: `android-seamless-loop-player`.
