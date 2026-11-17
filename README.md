# Dcam

A retro camera app for Android. Dark UI, yellow accents, one big **zap** button.

## Download

Latest APK (always the newest build from `main`):

**https://github.com/divakardznsyk/Dcam/releases/download/latest/Dcam.apk**

Open it on your phone and install (allow "install unknown apps" when asked). New builds install as updates over old ones.

## Dcam 2.1

- **13 films, each with its own illustrated camera**: NATURAL, GOLD 200, PORTRA 400, EKTAR 100, SUPERIA 400, TRI-X 400, CYANO, CINE 800T, POLA 600, DISPO 27, CCD 2003, SUNSET, MATTE. Every one has a clearly different colour. Import your own `.cube` LUTs too.
- **Live viewfinder**: swipe left and right to change film. Drag up and down for exposure. Vignette, grain, light leak, dust, date stamp and frame all show live while you shoot.
- **Film strip** under the viewfinder, and a full Films browser (grid icon).
- **Burst**: slider from 2 to 100 shots, gap of 1, 2, 3, 5 or 10 seconds. Tap the shutter again to stop.
- **Double exposure**: the first shot ghosts over the viewfinder. Three blends (SOFT, MIX, LIGHTEN), pick the first photo from your gallery, reset.
- **Develop**: pick a gallery photo and scroll through the films to see each one on it, then save.
- **Tune**: exposure, date stamp, light leaks, grain, frames, dust and scratches.
- **Settings**: RAW + JPEG, keep original, shutter sound, mirror selfies, grid.

Photos are saved to `Pictures/Dcam`. Film names are close to real film stocks but are original approximations, not official presets.

## Build

Every push to `main` runs the **Build APK** GitHub Action, which builds a signed release APK and updates the download link above.

Locally: open in Android Studio (JDK 17), or with Gradle 8.11+ run `gradle assembleRelease`.

The signing key `app/dcam-release.jks` is committed on purpose so updates install cleanly. Replace it with a private key before publishing to the Play Store.

## Project layout

```
app/src/main/java/com/dcam/app/
  MainActivity.kt           entry point
  ui/                       Theme, components, CameraScreen, FilmsSheet, TuneSheet, SettingsScreen
  DcamViewModel.kt          state, burst loop, timer, double exposure, LUT import, edit
  camera/CameraEngine.kt    CameraX preview, zoom, focus, flash, exposure, RAW+JPEG capture
  camera/BitmapUtil.kt      memory-safe photo decoding
  camera/ImageSaver.kt      saves JPEGs to the gallery
  ui/CameraArt.kt           the illustrated cameras
  ui/LiveOverlay.kt         live vignette, grain, leak, date, frame
  filters/Look.kt           films (add new ones in BuiltInLooks)
  filters/Effects.kt        effect options
  filters/LookProcessor.kt  render pipeline: crop, look, leak, dust, date, frame
  filters/CubeLut.kt        .cube parser
```

## Ideas for V2

Video with looks, live preview for LUT looks, more frames and light leaks, app icon, in-app gallery.
