# Pile Documents

Android app for organizing personal documents with tags (“piles”). Source lives in `Pile/` (Apache-2.0, based on [rubenalfon/Pile](https://github.com/rubenalfon/Pile)).

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
