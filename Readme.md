# Scanner

Sci-fi HUD (Jarvis style) scanner for Android 10+ : scan documents to PDF, scan QR codes and barcodes.
Same design language and architecture as the Dialer app (Jetpack Compose, cyan/amber HUD palette, phone + tablet layouts).

- Package: `com.mux.android.scanner` - Version: `1.0.0`

## Screens
| Tab  | What it does |
|------|--------------|
| CODE | Live camera scanner (QR, EAN, UPC, Code 128/39/93, ITF, Data Matrix, PDF417, Aztec), torch, scan from gallery image. Copy / share / open / search |
| DOCS | Document scanner (auto edge detection, crop, clean-up, multi page) saved as PDF; open, share, save to Downloads/Scanner, rename, delete |
| LOG  | History of scanned codes |
| SYS  | App info, theme (cyan / amber), camera permission, updates, author |

## Notes
- Code reader: ML Kit barcode model bundled in the app, works offline.
- Document scanner: ML Kit through Google Play services; needs GMS and internet the first time.
- `app/debug.keystore` is shared with Dialer so debug builds always have the same signature.
- `public/` holds `logo.ico` and `screenshots/` (design mockups of the screens).

## Build
Open in Android Studio, or `gradle assembleDebug` (Gradle 8.9, JDK 17). CI: `.github/workflows/build.yml`.
Release updates are read from GitHub releases of `mahfuz0712/Scanner` (edit `AppInfo.kt`).
