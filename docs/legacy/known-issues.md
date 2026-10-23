# File Claw - Known Issues & Tech Debt
Last Updated: June 15, 2026
Document Version: 1.0
Next Review Date: December 15, 2026

## 1. Issue Classification System
- **Severity**: `CRITICAL` (Crashes/Data Loss), `HIGH` (Core Feature Broken), `MEDIUM` (Degraded Experience), `LOW` (Cosmetic).
- **Status**: `OPEN`, `IN_PROGRESS`, `CLOSED`, `BY_DESIGN`.
- **Area**: `UI`, `FILE_IO`, `PDF_ENGINE`, `DATABASE`, `PERFORMANCE`.

## 2. Active Issues (CRITICAL and HIGH)

| ID | Title | Severity | Status | Area | Description & Workaround |
|----|-------|----------|--------|------|--------------------------|
| BUG-001 | Large Text File UI Freeze | HIGH | OPEN | `FILE_IO` | Files > 10MB cause ANRs due to full read into memory on main thread string assignment. *Workaround*: App limits rendering blocks, but true streaming is missing. |
| BUG-002 | Search Indexing Latency | HIGH| OPEN | `PERFORMANCE`| Searching entire external storage triggers massive GC churn. |

## 3. Active Issues (MEDIUM and LOW)

| ID | Title | Severity | Status | Area | Description & Workaround |
|----|-------|----------|--------|------|--------------------------|
| BUG-003 | PDF Zoom Reset on Rotation | MEDIUM| OPEN| `UI`| Activity recreation loses `pdfScale` bounds. |
| BUG-004 | CSV Table Horizontal Scroll | LOW| OPEN| `UI`| Extreme widths cause minor clipping on strict bounds. |
| BUG-005 | Password Field Obscuring | LOW| OPEN| `UI`| Keyboard overlaps the unlock dialog on small screens. |

## 4. Recently Fixed Issues
- **PDF Password Crash**: `PdfRenderer` previously crashed wildly on encrypted files. *Fix*: Implemented `PDFBox-Android` as a robust fallback to decrypt safely into cache. Verified via local tests.

## 5. Technical Debt Register
- **Architecture Debt**: `MainViewModel` is a God Class. It manages Home, Preview, Editor, and Navigation. MUST be split into scoped viewmodels.
- **Testing Debt**: Automated test coverage is currently 0%.
- **I/O Debt**: `FileManager` utilizes older `java.io.File` APIs synchronously in many places instead of non-blocking `NIO` or strict Coroutine stream channels.
- **String Debt**: Hardcoded UI strings exist across all Compose screens.

## 6. Platform Limitations
- **Android Scoped Storage**: Strict file limits prevent raw `java.io.File` modifications in standard `Documents` or `Downloads` directories on API 30+ without `MANAGE_EXTERNAL_STORAGE`.
- **Compose Lazy Lists**: Rapidly scrolling thousands of heavy items with complex thumbnail logic can drop frames on lower-end devices.

## 7. Third-Party Limitations
- **PDFBox-Android**: Extremely heavy dependency (~5MB). Updates are incredibly slow/orphan state, but required because native Android `PdfRenderer` explicitly lacks password support.

## 8. Won't Fix / By Design
- **DOCX Formatting Loss**: Custom parser strictly isolates text. Retaining word-perfect structural formatting (images, fonts, alignments) is `BY_DESIGN` ignored to keep the app lightweight and avoid bloated Apache POI imports.
- **No Cloud Sync**: App is marketed and designed as purely offline file manager. Network APIs are excluded intentionally.

## 9. Issue Triage Rules
- Users/QA submit items mapped to the BUG format.
- Maintainer assigns Severity.
- `CRITICAL` forces a hotfix branch. `HIGH` enters the next sprint.

## 10. Metrics and Trends
- Currently tracking high error rates on nested ZIP files (Unimplemented).
- Memory optimization is the highest trending issue area based on internal benchmarks.

---
**Update Log**
- 2026-06-15: v1.0 - Initial document creation.
