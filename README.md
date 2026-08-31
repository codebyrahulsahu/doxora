# Pile Documents

Android app for organizing personal documents with tags (“piles”). Source lives in `Pile/` (Apache-2.0, based on [rubenalfon/Pile](https://github.com/rubenalfon/Pile)).

## Features

- **Your Piles** – horizontally scrollable, color-coded folder cards (Home, Work, Taxes…) showing the item count per pile, plus a “New Pile” card.
- **All Documents** – vertical list with a real thumbnail of the document cover, file name, date added and file size, sortable by date (newest / oldest).
- **Pile detail view** – shows only the documents inside the selected pile, with a header tinted in the pile’s color.
- **Favorites** – star any document from the list, the document detail screen or the pile view, and find all of them in the dedicated **Favorites** screen (reachable from the home header or from Settings → Library).
- **Recognize Text (OCR)** – run on-device text recognition over a scanned document or an imported PDF. The extracted text is selectable (so it can be copied) and fully editable, and it is stored with the document.
- **Per document PIN lock** – protect any individual document with a 4 digit PIN. Locked documents show a lock badge in the lists and ask for the PIN before their content is displayed. The global “App Lock” has been removed in favour of this per document protection.
- **Recycle Bin** – deleted documents are moved to the Recycle Bin (Settings → Library) instead of being removed. They can be restored any time, and are permanently deleted automatically after 30 days (the pending deletion survives restarts and is also cleaned up on app start).
- **Multiple export options** – every document can be exported as a **PDF** file or as **JPG/PNG image** files (one per page/scan) straight to your device’s Downloads folder.
- **Local Backup & Restore** – export every pile, document, image, favorite, recognized text, document lock, recycle bin entry and setting into a single `.zip` file stored wherever you choose on your device, and restore it later from the same screen. There is no cloud backup: nothing ever leaves the device.
- **Settings** – User Account (editable profile), Appearance (system theme), Resolution, Library (Favorites and Recycle Bin), Local Backup & Restore and About & Support (support QR code, Instagram `@rahulsahux004`, GitHub `codebyrahulsahu` and `kanhaiyalaljojawar@gmail.com`).
- Clean pastel UI with rounded cards and a floating “+” action button on the main screens for importing PDFs, gallery images or camera shots.

## Privacy

Everything happens on the device: documents, thumbnails, recognized text, favorites and PIN hashes are stored in the app’s local database, and backups are plain files you write yourself.

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
