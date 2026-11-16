# Dcam

A retro camera app for Android. Dark UI, yellow accents, one big **zap** button.

## Download

Latest APK (always the newest build from `main`):

**https://github.com/divakardznsyk/Dcam/releases/download/latest/Dcam.apk**

Open it on your phone and install (allow "install unknown apps" when asked). New builds install as updates over old ones.

## Features (V1)

**Looks** (tap the look name at the top to show or hide the control panel)
- 20 built-in looks: GOLD 200, PORTRA 400, SUPERIA 400, EKTAR 100, VELVIA 50, TRI-X 400, HP5 400, CINE 800T, TEAL ORANGE, MIDNIGHT, POLA 600, SX FADE, DISPO 27, DISPO GREEN, FLASH, CCD 2003, DIGI 99, SUNSET, SEPIA, FADE (plus ZERO for no filter)
- Import your own 3D `.cube` LUTs with `+ LUT`
- Live tinted preview

**FX tab**
- Date stamp (orange or white, retro `'26 11 16` style)
- Light leaks: WARM, SUN, RED, COOL
- Dust and scratches
- Grain level: OFF / LOW / MED / HIGH
- Frames: POLAROID, FILM strip, BLACK, WHITE
- Double exposure

**SHOOT tab**
- Ratio 4:3 / 1:1 / 16:9
- Flash OFF / AUTO / ON
- Timer 3s / 10s
- Grid, exposure compensation
- **Burst**: choose 2 to 30 photos and the gap between them (100 to 1000 ms)

**Everything else**
- Tap to focus, pinch to zoom, 1x / 2x / 3x, front/back flip
- **EDIT**: pick a photo from your gallery and apply the current look and effects
- Last-photo thumbnail opens in your gallery
- Settings: **RAW + JPEG** (saves a `.dng` with every photo, if the phone supports it), save the untouched original too, shutter sound, mirror selfies, grid

Photos are saved to `Pictures/Dcam`.

Look names are close to real film stocks and cameras but they are original approximations, not official presets. Rename them before any Play Store release.

## Build

Every push to `main` runs the **Build APK** GitHub Action, which builds a signed release APK and updates the download link above.

Locally: open in Android Studio (JDK 17), or with Gradle 8.11+ run `gradle assembleRelease`.

The signing key `app/dcam-release.jks` is committed on purpose so updates install cleanly. Replace it with a private key before publishing to the Play Store.

## Project layout

```
app/src/main/java/com/dcam/app/
  MainActivity.kt           entry point
  DcamScreen.kt             Compose UI (preview, panel tabs, settings, zap)
  DcamViewModel.kt          state, burst loop, timer, double exposure, LUT import, edit
  camera/CameraEngine.kt    CameraX preview, zoom, focus, flash, exposure, RAW+JPEG capture
  camera/BitmapUtil.kt      memory-safe photo decoding
  camera/ImageSaver.kt      saves JPEGs to the gallery
  filters/Look.kt           looks (add new ones in BuiltInLooks)
  filters/Effects.kt        effect options
  filters/LookProcessor.kt  render pipeline: crop, look, leak, dust, date, frame
  filters/CubeLut.kt        .cube parser
```

## Ideas for V2

Video with looks, live preview for LUT looks, more frames and light leaks, app icon, in-app gallery.
