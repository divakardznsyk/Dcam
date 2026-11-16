# Dcam

A retro camera app for Android. Dark UI, yellow accents, one big **zap** button.

## Download

Latest APK (always the newest build from `main`):

**https://github.com/divakardznsyk/Dcam/releases/download/latest/Dcam.apk**

Open it on your phone and install (allow "install unknown apps" when asked). New builds install as updates over old ones.

## Dcam 2.0

The camera screen is kept calm. Everything else lives in its own place.

- **Camera**: a clean viewfinder with flash, ratio, timer, zoom and flip on top of it. Tap to focus, pinch to zoom.
- **Films**: tap the square next to the shutter. A grid of 20 films, each shown on a sample scene so you see its real colour, sorted into COLOR, B&W, CINE, INSTANT, DIGI and MOOD. Import your own `.cube` LUTs too.
- **Tune**: exposure, date stamp, light leaks, grain, frames, dust and scratches, burst gap.
- **Shoot modes**: PHOTO, BURST (choose 2 to 30 shots with the + and - buttons) and DOUBLE (the first shot ghosts over the viewfinder so you can line up the second).
- **Settings**: RAW + JPEG (saves a .dng with every photo), keep the untouched original, shutter sound, mirror selfies, grid.
- **Develop**: the gallery icon at the top applies the current film to any photo you pick.

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
  filters/Look.kt           looks (add new ones in BuiltInLooks)
  filters/Effects.kt        effect options
  filters/LookProcessor.kt  render pipeline: crop, look, leak, dust, date, frame
  filters/CubeLut.kt        .cube parser
```

## Ideas for V2

Video with looks, live preview for LUT looks, more frames and light leaks, app icon, in-app gallery.
