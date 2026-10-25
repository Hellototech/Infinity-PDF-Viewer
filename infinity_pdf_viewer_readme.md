# Infinity PDF Viewer

> **An ultra-lightweight, 100% offline, privacy-first native Android PDF reader, ML Kit document scanner, and vector annotation studio built exclusively with Jetpack Compose.**

[![Platform](https://img.shields.io/badge/Platform-Android%2024%2B-ED1C24?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Hellototech/Infinity-PDF-Viewer)
[![UI Toolkit](https://img.shields.io/badge/UI-Jetpack%20Compose-1E1E1E?style=for-the-badge&logo=jetpackcompose&logoColor=ED1C24)](https://developer.android.com/jetpack/compose)
[![Engine](https://img.shields.io/badge/Engine-Native%20PdfRenderer-1E1E1E?style=for-the-badge)](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Local%20First-00E676?style=for-the-badge)](https://github.com/Hellototech/Infinity-PDF-Viewer)

---

## 📖 Reclaiming the Mobile Document Experience

Over the last decade, the humble PDF reader on Android has undergone a quiet, frustrating transformation. What should be one of the simplest, fastest utilities on a smartphone has instead become a vehicle for software bloat. Today, opening a simple two-page receipt or an academic paper on a mobile device routinely requires downloading an 80-to-150 megabyte application packed with redundant third-party C++ rendering engines, mandatory cloud account sign-ins, intrusive telemetry trackers, and relentless subscription paywalls that lock basic document manipulation behind monthly fees. Worse still, many modern readers force users to upload sensitive personal contracts, financial statements, and research notes to external servers just to merge two files or add a simple signature.

**Infinity PDF Viewer** was engineered from the ground up as a direct rejection of that trend. It is built on a singular, uncompromising philosophy: **your documents belong strictly on your device, and your tools should be instantaneous.**

Rather than bundling massive, duplicate C++ binaries like PDFium or MuPDF inside the APK, Infinity PDF Viewer harnesses the raw, hardware-accelerated power of Android's own native `android.graphics.pdf.PdfRenderer` and `android.graphics.pdf.PdfDocument` APIs. By trusting the operating system's built-in graphics pipeline and pairing it with a mutex-guarded, resolution-adaptive LRU memory cache, Infinity PDF achieves 60fps scrolling, razor-sharp pinch-to-zoom supersampling, and complex multi-document compilation—all within a featherweight APK footprint under 15MB.

Every pixel of the interface is crafted in 100% declarative Jetpack Compose, governed by a strict industrial design language tailored for modern OLED displays. By utilizing a true `#000000` OLED black canvas paired with `#1E1E1E` dark slate cards and `#ED1C24` crimson accents, the application eliminates screen glare in dark environments while reducing display power consumption. When reading bright, white-background documents at night, a single tap engages the Smart Contrast Inverter—a real-time 5x4 color matrix transformation that turns blinding white pages into deep, comfortable dark slate without washing out vector ink annotations or technical diagrams.

Beyond pure reading, Infinity PDF Viewer is a complete, self-contained document studio. Whether you are scanning physical paperwork with real-time four-corner edge detection and perspective correction via Google ML Kit, sketching freehand Bezier ink notes that persist in resolution-independent normalized coordinates, reorganizing page sequences in a visual thumbnail grid, stamping diagonal watermarks across confidential packets, or rotating individual landscape tables on the fly without disturbing the rest of the document, every operation executes locally in milliseconds. There are no cloud queues, no artificial feature gates, and zero analytics tracking your reading habits.

With a built-in, zero-dependency updater wired directly to GitHub Releases, Infinity PDF Viewer remains lightweight, transparent, and evergreen—proving that native Android utilities can still be fast, private, and unapologetically crafted for the user.

---

## ✨ Flagship Feature Suite

### 1. High-Precision Native Viewport & Reading Engine
* **Dual Viewport Modes:** Seamlessly toggle between infinite **Continuous Vertical Scroll** and focused **Individual Page View**.
* **OLED Smart Contrast Inverter:** Hardware-accelerated `ColorMatrixColorFilter` inverts bright white PDF pages into glare-free dark slate while keeping vector annotations vivid and legible.
* **Granular Per-Page Rotation:** Rotate only the active page currently in view (`0°`, `90°`, `180°`, `270°`) to inspect landscape tables or schematics without rotating the entire document.
* **Spring-Physics Pinch & Double-Tap Zoom:** Smooth `1.0x` to `4.0x` magnification with bounded pan clamping and automatic high-DPI supersampling.
* **Study Mode Lock:** Dedicated reader toggle that holds `FLAG_KEEP_SCREEN_ON` so your display never dims while studying technical references or sheet music.
* **Encrypted PDF Shield:** Gracefully intercepts `SecurityException` on password-protected files and displays a clean notification dialog rather than crashing or hanging.

### 2. Vector Annotation & Markup Studio
* **Normalized Coordinate Matrix:** All freehand ink strokes, chisel highlights, callout notes, geometric boxes, and stamps are stored in `[0.0 .. 1.0]` normalized space inside Room SQLite—guaranteeing zero pixelation at any zoom level.
* **Freehand Ink Pen & Chisel Highlighter:** Adjustable stroke widths (`2pt` to `16pt`) and curated high-contrast color swatches (Crimson, Solar Yellow, Electric Cyan, Neon Emerald, Violet, Crisp White).
* **Architectural Stamp Badges:** One-tap placement of `APPROVED`, `CONFIDENTIAL`, `REVIEWED`, `VERIFIED`, and `DRAFT` vector badges.
* **Sticky Callout Notes:** Attach structured text callouts directly to specific coordinates on any page.
* **Precision Radius Eraser:** Interactive eraser with a live visual radius ring (`8pt` to `64pt`), plus instant **Undo** and **Clear Page** actions.

### 3. Physical Document Scanner (ML Kit)
* **Real-Time 4-Corner Detection & Auto-Capture:** Powered by `play-services-mlkit-document-scanner` (`SCANNER_MODE_FULL`), automatically locking onto document boundaries, correcting perspective distortion, and capturing pages hands-free.
* **Enhancement Filters:** Apply **Magic Color**, **High-Contrast B&W**, **Grayscale**, or **Original** profiles to remove shadows and sharpen text before compiling directly into a local PDF.
* **Built-in Studio Fallback:** Includes a complete in-app multi-page camera and gallery scanner sheet for devices or emulators without Play Services camera modules.

### 4. Local-First PDF Studio Tools
* **Visual Page Organizer:** Interactive thumbnail grid allowing you to reorder pages up/down or delete unwanted pages and compile the surviving order into a clean PDF.
* **Multi-Page Watermark & Numbering Suite:** Stamp custom diagonal (`-45°`) or horizontal (`0°`) watermarks with adjustable opacity (`10%` to `50%`) alongside formatted page numbers (`Page X of Y` or `X / Y`) at the Bottom Center, Bottom Right, or Top Right.
* **Multi-Document Merger:** Combine two or more PDF documents sequentially into a single unified packet.
* **Precision Range Splitter:** Extract custom `[Start .. End]` page intervals into standalone PDF files.
* **High-Resolution Page-to-Image Export:** Render any annotated PDF page at `2000px+` width into a lossless PNG and share it immediately via the system share sheet.
* **Native System Printing:** Spools local PDF bytes directly to Android's `PrintManager` and `PrintDocumentAdapter`.

### 5. Autonomous Device Discovery & GitHub Auto-Updater
* **Startup Storage Scanner:** Automatically indexes PDF documents across device storage (`Downloads`, `Documents`, and `MediaStore`) upon granting storage access, while supporting Android's native **"Open With"** (`ACTION_VIEW`) intent filter from any file manager.
* **GitHub Release Auto-Updater:** Built with standard `HttpURLConnection` (zero third-party network bloat) to check `Hellototech/Infinity-PDF-Viewer` releases, display changelogs, download the latest `.apk` with live progress, and trigger Android's package installer via `FileProvider`.

---

## 🎨 Design System & Strict Color Law

Infinity PDF Viewer enforces a high-contrast industrial dark theme (with full support for Light and System Default modes):

| Token Role | Dark / OLED Hex | Light Hex | Usage Specification |
| :--- | :--- | :--- | :--- |
| **Primary Canvas** | `#000000` | `#F5F6F9` | True OLED Black background eliminating pixel light bleed |
| **Elevated Surface** | `#1E1E1E` | `#FFFFFF` | Cards, modal bottom sheets, and dialogs with `16dp` rounded corners |
| **Secondary Surface** | `#282828` | `#EAECEF` | Interactive pills, stat badges, and unselected tool containers |
| **Primary Typography** | `#FFFFFF` | `#141518` | Space Grotesk headers, Inter body copy, and primary iconography |
| **Secondary Metadata** | `#B3B3B3` | `#5F6368` | JetBrains Mono telemetry, file sizes, timestamps, and subtitles |
| **Crimson Accent** | `#ED1C24` | `#ED1C24` | Strictly reserved for active navigation pills, selected tools, and primary actions |

---

## 🏗️ Technical Architecture

```text
app/src/main/java/com/example/
├── MainActivity.kt                  # Edge-to-edge host, permission workflow, ML Kit scanner & intent router
├── data/
│   ├── PdfEntities.kt               # Room entities (PdfDocumentEntity, PdfAnnotationEntity, PdfBookmarkEntity)
│   └── PdfDatabase.kt               # Room SQLite database, DAO queries, and local repository
├── pdf/
│   └── NativePdfEngine.kt           # Mutex-guarded PdfRenderer pipeline, LRU bitmap cache, merger, splitter & watermarker
├── update/
│   └── GitHubUpdateManager.kt       # Zero-dependency GitHub Releases checker, APK downloader & FileProvider installer
├── viewmodel/
│   └── InfinityPdfViewModel.kt      # Reactive StateFlow state holder, per-page rotation map & studio orchestration
└── ui/
    ├── components/
    │   ├── PdfThumbnailView.kt      # Asynchronous cached page thumbnail composable
    │   └── DocumentScannerSheet.kt  # In-app multi-page scan & filter studio sheet
    ├── screens/
    │   ├── LibraryScreen.kt         # Search, filter chips, sort menu, and auto-detected document cards
    │   ├── PdfReaderScreen.kt       # Interactive zoom/pan canvas, per-page rotation, vector ink dock & page scrubber
    │   ├── StudioToolsScreen.kt     # 2-column studio grid (Scan, Organizer, Watermark, Merge, Split)
    │   ├── BookmarksWorkspaceScreen.kt # Unified hub for saved page pins and vector annotations
    │   └── SettingsScreen.kt        # Telemetry stats, theme switcher, cache manager & GitHub update center
    └── theme/
        ├── Color.kt                 # Dynamic OLED Dark & Light color tokens
        ├── Type.kt                  # Space Grotesk, Inter, and JetBrains Mono font families
        └── Theme.kt                 # Material 3 theme wrapper
```

---

## 📥 Installation & Releases

1. Navigate to the [**Releases**](https://github.com/Hellototech/Infinity-PDF-Viewer/releases) page.
2. Download the latest **`Infinity-PDF-Viewer-v1.0.0.apk`** asset to your Android device.
3. Open the downloaded APK and tap **Install**.
4. On first launch, tap **Grant Access** when prompted so Infinity PDF Viewer can automatically discover and index the PDF files on your phone.
5. Future updates can be installed with a single tap directly inside **Settings → Check for Updates**.

---

## 🛠️ Building from Source

### Prerequisites
* **Android Studio** Ladybug / Meerkat or newer
* **JDK 17**
* **Android SDK 36** (Minimum SDK: **API 24 / Android 7.0**)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/Hellototech/Infinity-PDF-Viewer.git
cd Infinity-PDF-Viewer

# Compile a debug APK via Gradle Wrapper
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Privacy Guarantee

* **Zero Cloud Uploads:** Your PDF documents, scanned pages, bookmarks, and ink annotations never leave your device's local storage.
* **Zero Telemetry or Ads:** No analytics SDKs, no crash-reporting trackers, and no advertising identifiers.
* **Transparent Network Scope:** The `INTERNET` permission is used exclusively by `GitHubUpdateManager` to query `api.github.com/repos/Hellototech/Infinity-PDF-Viewer/releases/latest` when checking for or downloading signed app updates.

---

## 📄 License

Released as open-source software by [**Hellototech**](https://github.com/Hellototech).