# Better Cards 💳

A lightweight, fast, and 100% offline-first Android loyalty cards wallet designed to replace bloated loyalty apps.

## Key Features

- **100% Offline-First**: Uses local SQLite (Room) with zero network dependency to view barcodes at the cash register.
- **Auto-Brightness Boost**: Automatically maxes out screen brightness when viewing a card so checkout scanners never fail, restoring brightness when you leave.
- **Klarna Loyalty Cards Import & Export**:
  - 1-click import from Klarna JSON backup files.
  - Round-trip export: Export all cards or selected cards in the exact same JSON format to share with family and friends.
  - Portable logo export: Bundled and custom logos are embedded as `data:image/png;base64,...` so exported JSON files carry images offline across devices.
- **65+ Built-in Australian & Global Brand Presets**:
  - Bundled high-res wordmarks and brand colors (*Everyday Rewards, Woolworths, Flybuys, IKEA Family, Myer One, Qantas, Bunnings, Chemist Warehouse, Costco, Dan Murphy's, Grill'd, Rebel*, and more).
  - Domain icon lookup for any new store scanned via camera.
  - Custom logo photo picker for local businesses, gyms, and clubs.
- **Starred / Favorites**: Pin frequently used cards to the top of your wallet.
- **Flexible Views**: Switch between a 2-column physical card wallet grid and a compact list view.
- **Link Sharing & Web Viewer**:
  - Share individual card links via WhatsApp or SMS.
  - Hosted web viewer live on GitHub Pages: [https://itajun.github.io/better-cards/](https://itajun.github.io/better-cards/)
  - Standalone single-page web viewer (`web/index.html` & `docs/index.html`) allowing recipients on iOS or Android to scan or save cards.
- **On-Device Camera Scanner & Photo Import**:
  - Live viewfinder with ML Kit barcode scanning.
  - Batch screenshot migration to import cards directly from phone screenshots.

## Tech Stack

- **UI**: Jetpack Compose, Material 3 (with Material You dynamic theming)
- **Database**: Android Jetpack Room (SQLite)
- **Barcode Engine**: ZXing (Code 128, EAN-13, EAN-8, UPC-A, QR Code, Aztec, PDF-417, Code 39, Data Matrix)
- **Scanner & Vision**: CameraX + Google ML Kit Barcode Scanning & Text Recognition (OCR)
- **Language**: Kotlin 2.0+

## Building & Installing

```bash
# Build debug APK
./gradlew assembleDebug

# Install to connected device via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
