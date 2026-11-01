Infinity PDF Viewer

Infinity PDF Viewer is an ultra-lightweight, 100% offline Android PDF reader, document scanner, and annotation studio.

Most modern PDF apps are bloated with 100MB+ third-party C++ engines, ads, mandatory cloud sign-ins, and subscription paywalls. Infinity PDF Viewer was built to be the exact opposite: fast, private, and completely local to your device.

🛠️ How It Is Made & What We Used

Instead of bundling heavy external PDF libraries, this app is engineered using Android's native system capabilities:

UI & Design: Built 100% with Jetpack Compose and Material 3 (zero XML layouts), featuring a true #000000 OLED Black theme, #ED1C24 Crimson accents, and a floating Liquid Glass bottom navigation dock.

PDF Rendering & Manipulation: Powered natively by Android's android.graphics.pdf.PdfRenderer and PdfDocument APIs, paired with an in-memory LRU Bitmap Cache for smooth 60/120fps scrolling and full-document pinch-to-zoom.

Physical Document Scanner: Integrated with Google ML Kit Document Scanner (play-services-mlkit-document-scanner) for real-time 4-corner edge detection, perspective correction, auto-capture, and scan filters (Magic Color, B&W, Grayscale) while adding less than 300KB to the app size.

Local Database: Uses Room SQLite and Kotlinx Serialization to store vector ink strokes, highlights, stamps, callout notes, and bookmarks in normalized [0..1] coordinates so annotations never pixelate at any zoom level.

In-App Auto-Updater: Built with standard Android HttpURLConnection and FileProvider to check GitHub Releases, download new .apk updates, and install them directly without third-party networking bloat.

✨ Key Features

OLED Night Mode: Real-time color matrix inverter turns bright white pages into dark slate while keeping ink annotations clear.

Vector Annotation Studio: Freehand pen, chisel highlighter, sticky notes, stamps (APPROVED, CONFIDENTIAL, DRAFT), and adjustable eraser.

PDF Studio Tools: Merge multiple PDFs, split page ranges, reorder/delete pages visually, rotate individual pages, and add multi-page watermarks or page numbers.

High-Res Export & Print: Export annotated pages as 2000px+ PNG images or print directly via Android's native PrintManager.

100% Private: Zero ads, zero analytics, and zero cloud uploads. Your files never leave your phone.

📥 Installation

Go to Releases and download Infinity-PDF-Viewer-v1.0.0.apk.

Install and open the app, then tap Grant Access so it can automatically find and list all PDFs on your phone.

Future updates can be installed directly inside Settings → Check for Updates.
