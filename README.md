# Doxora App

**Doxora** is a free, open-source, privacy-first **Android document manager** for organizing all your personal documents (PDF, photos, scans) with color-coded **Hubs**, on-device **OCR**, **favorites**, per-document **PIN lock**, a **recycle bin** and **local backup & restore**. Everything stays on your device — no cloud, no tracking.

- **App name:** Doxora
- **Developer / GitHub:** [codebyrahulsahu](https://github.com/codebyrahulsahu)
- **Source repository:** [`codebyrahulsahu/pile-documents`](https://github.com/codebyrahulsahu/pile-documents)
- **License:** Apache-2.0 (based on [rubenalfon/Pile](https://github.com/rubenalfon/Pile))
- **Platform:** Android (Kotlin + Jetpack Compose)

> Searching for **“doxora app github”** or **“Doxora App GitHub”**? This is the official GitHub repository — open the **Releases** tab to download the latest APK.

## Download APK

Grab the latest APK from the **[Releases](https://github.com/codebyrahulsahu/pile-documents/releases)** page (look for the latest release tagged `v1.2.0` and download the attached `*debug.apk`), or build it yourself from the instructions below.

## Features

- **Your Hubs** – horizontally scrollable, color-coded folder cards (Home, Work, Taxes…) showing the item count per hub, plus a “New Hub” card. Long-press and drag any hub to rearrange its order. Upload a **profile picture** for a hub (from the hub’s “+” menu or by tapping the avatar in its header) when the hub belongs to a specific person: the photo then replaces the icon on the hub card. Every picked photo opens a built-in **cropper** (square frame with a circular guide plus 90º rotation) so the avatar shows exactly the part of the picture you want.
- **All Documents** – vertical list with a real thumbnail of the document cover, file name, date added and file size, sortable by date (newest / oldest).
- **Hub detail view** – shows only the documents inside the selected hub, with a header tinted in the hub’s color. Long-press any document to enter **multi selection** and export, share or delete several documents at once, exactly like in the home screen.
- **Favorites** – star any document from the list, the document detail screen or the hub view, and find all of them in the dedicated **Favorites** screen (reachable from the home header or from Settings → Library).
- **Recognize Text (OCR)** – run on-device text recognition over a scanned document or an imported PDF. The extracted text is selectable (so it can be copied) and fully editable, and it is stored with the document.
- **Per document PIN lock** – protect any individual document with a 4 digit PIN. Locked documents show a lock badge in the lists and ask for the PIN before their content is displayed. The global “App Lock” has been removed in favour of this per document protection.
- **Recycle Bin** – deleted documents are moved to the Recycle Bin (Settings → Library) instead of being removed. They can be restored any time, and are permanently deleted automatically after 30 days (the pending deletion survives restarts and is also cleaned up on app start).
- **Multiple export options** – every Export button in the app (document toolbar, home multi selection and hub multi selection) opens the same menu with three options: **Export as PDF**, **Export as JPG** and **Export as PNG**. The save folder is requested **only once**, right after the format is chosen: the granted folder permission is persisted and reused for every later export. The export folder can also be changed manually at any time from **Settings → Export → Export folder**.
- **Document Resizer** – resize already saved documents to a **custom target file size** (KB or MB) with **zero quality loss**. The resizer is not offered anywhere globally (no Settings screen and no import prompt): it appears **at the top of the screen only while a document is actively selected** (long-press a document in the home screen or inside a hub). Tapping it opens a prompt with an input field for the target size (toggle KB / MB) and exactly two save options: **Save as original file in app** (the resized pages replace the original files) or **Save as duplicate file** (a new resized copy is created next to the original). Image quality is never reduced: pages that already fit the target are stored unchanged, and larger pages are downscaled (keeping maximum JPEG quality) until they fit. Imported PDFs are skipped.
- **Local Backup & Restore** – export every hub, document, image, favorite, recognized text, document lock, recycle bin entry and setting into a single `.zip` file stored wherever you choose on your device, and restore it later from the same screen. The exported file is always named **`doxora document backup.zip`**. There is no cloud backup: nothing ever leaves the device.
- **Settings** – User Account (editable profile), Appearance (system theme), Resolution, Library (Favorites and Recycle Bin), Local Backup & Restore and About & Support (support QR code, Instagram `@rahulsahux004`, GitHub `codebyrahulsahu` and `kanhaiyalaljojawar@gmail.com`).
- **Camera in Document mode** – “Take a photo” strictly opens the **document scanning mode built into the device's default camera** when it exposes one, and otherwise the plain device camera (still requesting its document mode through capture hints). The Google Play services camera scanner is never triggered.
- Clean pastel UI with rounded cards and a floating “+” action button on the main screens for importing PDFs, gallery images, images from the device folders or scans. Compact icon-only chips for the list/icon view toggle and the sort menu, and a collapsed **About & Support** section that only expands when its header is tapped.

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
