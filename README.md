# Dcam

A simple retro camera app for Android. Dark UI, yellow accents, one big **zap** button.

## Features

- **Retro looks** built in: ZERO (none), GOLD, SOFT, NOIR, INSTANT, DISPO, TUNGSTEN, FADE.
  Each look combines colour grading, faded blacks, film grain and vignette. These are original approximations inspired by classic film stocks and cameras, not official presets.
- **Import your own looks**: tap `+ LUT` in the LOOK row and pick a 3D `.cube` file. It is saved inside the app and appears in the list next time.
- **Burst mode**: turn BURST on, then pick how many photos (2 to 30) and the gap between them (100 to 1000 ms). One tap on zap takes them all.
- **Double exposure**: take two shots, they are screen-blended into one picture.
- 1x / 2x zoom, front/back camera flip, live tinted preview.
- Photos are saved to `Pictures/Dcam`.

## Get the APK (no Android Studio needed)

Every push to `main` runs the **Build APK** GitHub Action. Open the Actions tab, pick the latest run, and download `dcam-debug-apk`. Unzip it and install `app-debug.apk` on your phone (allow installs from unknown sources).

## Build locally

Open the folder in Android Studio (Koala or newer, JDK 17) and run the `app` configuration. Or with Gradle 8.9+ installed: `gradle assembleDebug`.

## Project layout

```
app/src/main/java/com/dcam/app/
  MainActivity.kt        entry point
  DcamScreen.kt          Compose UI (preview, panel, zap button)
  DcamViewModel.kt       app state, burst loop, double exposure, LUT import
  camera/CameraEngine.kt CameraX preview, zoom, flip, capture
  camera/ImageSaver.kt   saves JPEGs to the gallery
  filters/Look.kt        the Look model and the built-in looks
  filters/LookProcessor.kt  applies a look (colour, grain, vignette, LUT)
  filters/CubeLut.kt     .cube parser with trilinear sampling
```

## Add a new built-in look

Add a line to `BuiltInLooks.all` in `filters/Look.kt`. Tweak `saturation`, `contrast`, `r/g/b` gains, `fade`, `grain` and `vignette`.

## Not done yet

- RAW capture (needs Camera2 DNG support)
- Live preview for LUT looks (they preview unfiltered, the saved photo is correct)
- App icon
