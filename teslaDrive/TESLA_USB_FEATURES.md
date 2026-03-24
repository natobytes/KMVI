# Tesla USB Drive Features

This document catalogs all features that can be configured and loaded onto a USB drive for Tesla vehicles. TeslaDrive aims to support creating and exporting each of these to a USB pen drive.

## Feature Summary

| # | Feature | Folder | Formats | Status in TeslaDrive |
|---|---------|--------|---------|---------------------|
| 1 | [Dashcam (TeslaCam)](#1-dashcam-teslacam) | `TeslaCam/` | MP4 (H.265) | Implemented (export) |
| 2 | [Sentry Mode](#2-sentry-mode) | `TeslaCam/SentryClips/` | MP4 (H.265) | Implemented (export) |
| 3 | [Boombox Sounds](#3-boombox-sounds) | `Boombox/` | MP3, WAV | Implemented (export) |
| 4 | [Custom Lock Chime](#4-custom-lock-chime) | Root (`LockChime.wav`) | WAV (24 kHz Mono PCM) | Not implemented |
| 5 | [Light Shows](#5-light-shows) | `LightShow/` | FSEQ + MP3/WAV | Implemented (export) |
| 6 | [Custom Wraps](#6-custom-wraps-vehicle-skins) | `Wraps/` | PNG | Not implemented |
| 7 | [Music Playback](#7-music-playback) | Any folder | FLAC, MP3, WAV, OGG, AIFF | Not implemented |
| 8 | [HomeLink Config](#8-homelink-configuration) | `HomeLink/` | Config files | Implemented (export) |

---

## 1. Dashcam (TeslaCam)

**What it is:** Continuous driving recording using built-in cameras (front, rear, left repeater, right repeater, left pillar, right pillar). Footage loops and overwrites unless manually saved.

**File format:** MP4 video (H.265/HEVC on HW3+ vehicles). Resolution 1280x960 per camera. Each camera produces a separate file per minute.

**Directory structure:**
```
TeslaCam/
  RecentClips/      # Rolling buffer, up to 60 minutes
  SavedClips/       # Manually saved clips (honk or tap)
  SentryClips/      # Security event recordings
  TrackMode/        # Track session recordings (if equipped)
```

**Limitations:**
- Minimum 64 GB USB drive
- Sustained write speed >= 4 MB/s
- RecentClips overwrites after 60 minutes
- Oldest SentryClips auto-deleted when storage runs low

**Supported models:** All Model S, 3, X, Y, Cybertruck (HW2.5+, software v9+).

**TeslaDrive status:** Export to TeslaCam folders is implemented. No creation/editing capability needed -- these are vehicle-generated files.

---

## 2. Sentry Mode

**What it is:** Security surveillance that records potential threats while parked. Uses the same USB drive and folder structure as Dashcam.

**File format:** Same as Dashcam (H.265 MP4, 1280x960).

**Directory structure:** `TeslaCam/SentryClips/` -- each event saves ~10 minutes (before + after trigger).

**TeslaDrive status:** Export path implemented. Same as Dashcam -- vehicle-generated files.

---

## 3. Boombox Sounds

**What it is:** Custom sounds played through the external Pedestrian Warning System (PWS) speaker. Can serve as a custom horn sound (Park only) or external audio playback.

**File format:** MP3 (recommended) or WAV.

**Directory structure:**
```
Boombox/
  sound1.mp3
  sound2.mp3
```

**Limitations:**
- Maximum 5 custom sounds (first 5 alphabetically by filename)
- Filenames: alphanumeric, periods, dashes, underscores only. No spaces
- Custom horn only works in Park (NHTSA regulation)
- Boombox disabled in Drive, Neutral, Reverse

**Supported models:** All vehicles with PWS speaker (September 2019+).

**TeslaDrive status:** Export path implemented. Could add: sound preview, filename validation, alphabetical ordering preview.

---

## 4. Custom Lock Chime

**What it is:** Custom sound that plays through the PWS speaker when the vehicle locks.

**File format:** WAV only. Recommended: 24 kHz Mono PCM.

**Directory structure:**
```
LockChime.wav    # Must be at USB root, NOT in any folder
```

**Limitations:**
- Must be named exactly `LockChime.wav` (case-sensitive)
- File size under 1 MB
- Duration up to 5 seconds
- Only 1 custom lock sound at a time

**Activation:** Toybox > Boombox > enable "Lock Sound" toggle > select "USB".

**Supported models:** All vehicles with PWS speaker (September 2019+). Requires 2023 Holiday Update or later.

**TeslaDrive status:** Not implemented. Needs: WAV file selection, automatic rename to `LockChime.wav`, placement at USB root (not in a subfolder), file size/duration validation.

---

## 5. Light Shows

**What it is:** Synchronized light and closure choreography set to music. Created with xLights software using Tesla's official configuration.

**File format:**
- Sequence: `.fseq` (V2 Uncompressed)
- Audio: `.mp3` or `.wav` (44.1 kHz sample rate required; 48 kHz will not sync)
- Both files must share the exact same base name (e.g., `MyShow.fseq` + `MyShow.mp3`)

**Directory structure:**
```
LightShow/
  MyShow.fseq
  MyShow.mp3
```

**Limitations:**
- Maximum show duration: 4 hours
- Frame interval: 15-100ms (20ms recommended)
- 46 controllable channels (lights, closures, horn)
- USB must NOT contain a `TeslaCam` folder at root level
- USB must NOT contain map or firmware update files
- Multiple shows supported per drive (v2023.44.25+)

**Model-specific channels:**
- Cybertruck: Front/Offroad/Rear Light Bars, Bed Lights
- Model S: Door Handles
- Model X: Falcon Wing Doors, Front Doors

**Supported models:** Model S (2021+), Model 3 (all), Model X (2021+), Model Y (all), Cybertruck. Requires software v11.0+.

**Official reference:** [github.com/teslamotors/light-show](https://github.com/teslamotors/light-show)

**TeslaDrive status:** Export path implemented. Could add: FSEQ+audio pair validation, sample rate check for audio files.

---

## 6. Custom Wraps (Vehicle Skins)

**What it is:** Custom PNG images that overlay onto the 3D vehicle model on the touchscreen, Tesla mobile app, and Apple Watch. Introduced in Tesla's 2025 Holiday Update via the Paint Shop feature in Toybox.

**File format:** PNG only.

**Directory structure:**
```
Wraps/
  design1.png
  design2.png
```

**Limitations:**
- Resolution: 512x512 to 1024x1024 pixels
- Maximum file size: 1 MB per image
- Up to 10 wrap images at a time
- Filenames: alphanumeric, underscores, dashes, spaces; 30-character max
- USB must NOT contain map or firmware update files

**Activation:** Toybox > Paint Shop > Wraps tab.

**Supported models:** Cybertruck, Model 3 (all variants), Model Y (all variants). Templates available at [github.com/teslamotors/custom-wraps](https://github.com/teslamotors/custom-wraps).

**TeslaDrive status:** Not implemented. Needs: PNG selection, resolution/size validation, preview, filename sanitization, `Wraps/` folder export.

---

## 7. Music Playback

**What it is:** Play audio files from a USB drive through the vehicle's sound system.

**File format:** FLAC, MP3, WAV, OGG, AIFF. Apple Lossless (ALAC) and DRM-protected files do NOT work.

**Directory structure:** No specific structure required. Tesla indexes all audio files regardless of folder hierarchy.

**Limitations:**
- Recommended to keep total music under ~30 GB for faster indexing
- Must use the glove box USB port (data-capable)

**Supported models:** All Tesla models.

**TeslaDrive status:** Not implemented. Could add: music file selection/export, format compatibility indicator.

---

## 8. HomeLink Configuration

**What it is:** Configuration files for the HomeLink garage door opener integration.

**Directory structure:**
```
HomeLink/
  <config files>
```

**TeslaDrive status:** Export path implemented. Limited use case -- most HomeLink setup is done through the vehicle UI.

---

## USB Drive Requirements

| Requirement | Specification |
|-------------|---------------|
| Format | exFAT (recommended), FAT32, ext3, ext4. **NTFS not supported** |
| Minimum size | 64 GB (for dashcam use) |
| Write speed | >= 4 MB/s sustained |
| USB standard | USB 2.0 compatible (USB 3.0 drives must support 2.0) |
| Data port | Glove box USB port (center console ports are charge-only on 2021+ vehicles) |

## Multi-Feature USB Considerations

Light Show and Custom Wraps USBs must NOT contain a `TeslaCam` folder. Practical setup:

**Two-drive approach:**
- Drive 1 (glove box): `TeslaCam/` + music files
- Drive 2 (hub or console): `LightShow/`, `Boombox/`, `Wraps/`, `LockChime.wav`

**Single-drive approach (no dashcam):**
```
USB root/
  Boombox/
  LightShow/
  Wraps/
  LockChime.wav
  Music/
```

TeslaDrive should warn users about folder conflicts when exporting mixed content.

---

## Features NOT Supported via USB

| Feature | Notes |
|---------|-------|
| Software updates | Tesla requires WiFi for OTA updates. USB install not supported |
| Custom turn signals | Changed via touchscreen settings, not USB files |
| Charging animations | Built into firmware, not loadable via USB |
| Custom UI themes | Not available |
| License plate frames | Physical accessory, not a USB feature |

---

## References

- [Tesla Owner's Manual - USB Drive Requirements](https://www.tesla.com/ownersmanual/model3/en_us/)
- [Tesla Light Show (GitHub)](https://github.com/teslamotors/light-show)
- [Tesla Custom Wraps (GitHub)](https://github.com/teslamotors/custom-wraps)
- [NotATeslaApp - Custom Lock Chime Guide](https://www.notateslaapp.com/news/3302/)
- [NotATeslaApp - USB Music Setup](https://www.notateslaapp.com/news/2145/)
- [Drive Tesla Canada - Boombox Setup](https://driveteslacanada.ca/software-updates/how-to-set-up-boombox-tesla-custom-mp3-sounds/)
