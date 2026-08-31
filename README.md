# Pile Documents

Android app for organizing personal documents with tags (“piles”). Source lives in `Pile/` (Apache-2.0, based on [rubenalfon/Pile](https://github.com/rubenalfon/Pile)).

## Features

- **Your Piles** – horizontally scrollable, color-coded folder cards (Home, Work, Taxes…) showing the item count per pile, plus a “New Pile” card.
- **All Documents** – vertical list with a large file-type icon, file name, date added and file size, sortable by date (newest / oldest).
- **Pile detail view** – shows only the documents inside the selected pile, with a header tinted in the pile’s color.
- **Settings** – User Account (editable profile), Appearance (system theme), Resolution, Storage & Security (Cloud Backup, App Lock) and About & Support.
- Clean pastel UI with rounded cards and a floating “+” action button on the main screens for importing PDFs, gallery images or camera shots.

## Debug APK

GitHub Actions builds a debug APK on every push:

1. Open **Actions → Build APK**
2. Open the latest successful run
3. Download the **Pile-App-Debug-APK** artifact (`app-debug.apk`)

Local build:

```bash
cd Pile
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 21 and Android SDK (compileSdk 37).
