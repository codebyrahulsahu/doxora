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
- **Document Resizer** – compress every new document page or scan to a custom target size (in KB or MB) while keeping the best possible quality. The feature has an **ON/OFF toggle switch** in **Settings → Document Resizer** and another one inside the import prompt itself, so it can be enabled or disabled without leaving the import. Images that already fit the target size are stored unchanged, and very heavy images are progressively downscaled (instead of being ruined by extremely low JPEG qualities) as a last resort.
- **Resizer on every import** – the Document Resizer prompt is shown every time images are imported, no matter the source: **gallery**, **device folders** (file browser), camera or scanner. The prompt is driven by an ON/OFF switch and, when it is on, the maximum size per image can be chosen (512 KB / 1 MB / 2 MB presets or a custom KB/MB value). The switch starts on the value stored in the settings, the answer wins for that import and is remembered as the new default.
- **Local Backup & Restore** – export every hub, document, image, favorite, recognized text, document lock, recycle bin entry and setting into a single `.zip` file stored wherever you choose on your device, and restore it later from the same screen. The exported file is always named **`doxora document backup.zip`**. There is no cloud backup: nothing ever leaves the device.
- **Settings** – User Account (editable profile), Appearance (system theme), Resolution, Document Resizer, Library (Favorites and Recycle Bin), Local Backup & Restore and About & Support (support QR code, Instagram `@rahulsahux004`, GitHub `codebyrahulsahu` and `kanhaiyalaljojawar@gmail.com`).
- **Camera in Document mode** – “Take a photo” opens the **Document mode built into the native camera** of the device when it exposes one. Otherwise the on-device document scanner of Google Play services is used (automatic edge detection, perspective correction, filters and multi-page capture), and if that one cannot run either the plain camera is opened, still requesting its document mode.
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
