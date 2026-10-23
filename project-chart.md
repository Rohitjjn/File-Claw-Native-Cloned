# File Claw - Project Chart & Matrices
Last Updated: June 15, 2026
Document Version: 1.0
Next Review Date: December 15, 2026

## 1. File Type Support Matrix

| Extension | Can Open | Can Preview | Can Edit | Can Save | Format Preserved | Thumbnail | Search Indexed |
|-----------|----------|-------------|----------|----------|------------------|-----------|----------------|
| .txt      | YES      | YES         | YES      | YES      | YES              | NO        | YES            |
| .md       | YES      | YES         | YES      | YES      | YES              | NO        | YES            |
| .csv      | YES      | YES (Table) | NO       | NO       | YES              | NO        | YES            |
| .zip      | YES      | YES (Tree)  | NO       | NO       | YES              | NO        | YES (Names)    |
| .pdf      | YES      | YES (Render)| NO       | [PARTIAL]| YES              | YES       | PLANNED        |
| .docx     | YES      | YES (Text)  | NO       | NO       | PARTIAL          | NO        | YES (Text)     |
| .png/jpg  | YES      | YES         | NO       | NO       | YES              | YES       | YES (Name)     |
| .mp3/wav  | YES      | YES (Play)  | NO       | NO       | N/A              | NO        | YES (Name)     |
| .mp4/mkv  | PLANNED  | PLANNED     | NO       | NO       | N/A              | PLANNED   | YES (Name)     |
| .json/xml | YES      | YES (Code)  | YES      | YES      | YES              | NO        | YES            |
| Unknown   | YES      | YES (Hex)   | NO       | NO       | N/A              | NO        | YES (Name)     |

> *Note on PDF Save: Only saving decrypted versions of password-protected PDFs is currently supported.*

## 2. Screen-to-Feature Mapping

| Screen | File List | Sorting | Preview Content | Editing | Theme Toggle | Search Index |
|--------|-----------|---------|-----------------|---------|--------------|--------------|
| Home   | YES (Recent)| NO    | NO              | NO      | NO           | NO           |
| AllFiles| YES      | YES     | NO              | NO      | NO           | NO           |
| Search | YES (Found)| NO     | NO              | NO      | NO           | YES          |
| Preview| NO        | NO      | YES             | NO      | NO           | NO           |
| Editor | NO        | NO      | NO              | YES     | NO           | NO           |
| Settings| NO       | NO      | NO              | NO      | YES          | NO           |

## 3. Permission Requirements Table

| Permission | API Level | When Requested | Why Needed | Fallback if Denied | User Message |
|------------|----------|----------------|------------|--------------------|--------------|
| `READ_EXTERNAL_STORAGE` | < 30 | Splash Screen | Required to crawl device files | Cannot proceed | "Storage permission is required." |
| `WRITE_EXTERNAL_STORAGE`| < 30 | Splash Screen | Required to save edits | Read-only mode | "Storage permission is required." |
| `MANAGE_EXTERNAL_STORAGE`| 30+ | Splash Screen | Required for full offline indexing | App terminates | "Allow management of all files is mandatory." |

## 4. Library Version Compatibility Chart

| Library Name | Current Version | Latest Available | Update Risk | Breaking Changes | Last Checked |
|--------------|-----------------|------------------|-------------|------------------|--------------|
| Jetpack Compose | 1.6.2 (BOM 2024)| BOM 2024.06.00 | MEDIUM      | Minor API shifts | Jun 2026     |
| Room         | 2.6.1           | 2.6.1            | LOW         | None expected    | Jun 2026     |
| PDFBox-Android| 2.0.27.0       | 2.0.27.0         | HIGH (Orphan)| High             | Jun 2026     |
| Coil Compose | 2.5.0           | 2.6.0            | LOW         | None             | Jun 2026     |

## 5. Performance Benchmarks Table

| Operation | Target Time | Worst Acceptable | Current Estimate | Measurement Method | Optimization Status |
|-----------|-------------|------------------|------------------|--------------------|---------------------|
| App Cold Start | < 1.0s | 3.0s | 1.2s | System Tracing | Acceptable |
| 1000-File Search | < 500ms | 2.0s | 600ms | In-app Timer | Needs threading optimization |
| 10MB Text Open | < 1.0s | 3.0s | 1.5s | Loading UI duration| Active (Chunking required) |
| 50-Page PDF Render| < 500ms/pg | 1.5s/pg | 200ms/pg | Visual Perception | Highly Optimized |

## 6. Error Handling Matrix

| Error Scenario | Source | Current Behavior | User Visible | Recovery Action | Logged | Priority |
|----------------|--------|------------------|--------------|-----------------|--------|----------|
| File NOT_FOUND | Disk | Falls back to Error State| YES | Show "File Deleted" | System | P1 |
| Config OOM | Memory | App crashes | NO | Requires chunked reader | System | P0 |
| PDF Password | User | Shows password dialog | YES | Retry decryption | No | P0 |
| File Locked | OS | Read-only constraint | YES | Disable edit button | No | P2 |

## 7. Build Size Breakdown

| Component | Approx. Size Impact | Notes | Mitigation Strategy |
|-----------|--------------------|-------|---------------------|
| PDFBox-Android | ~5 MB | Heavy native font/rendering logic | Proguard stripping if unused fonts |
| Compose Runtime| ~3 MB | Necessary baseline | Standard R8 Optimization |
| App Logic / Kotlin | < 1 MB | Highly efficient | N/A |
| **Total Target** | **~9-12 MB** | | |

## 8. Testing Coverage Matrix

| Component | Unit Tests | Integration Tests | UI Tests | Manual Tests | Coverage % | Critical Paths |
|-----------|------------|-------------------|----------|--------------|------------|----------------|
| ViewModels| NO         | NO                | NO       | YES          | 0%         | State emissions|
| FileManager| NO        | NO                | NO       | YES          | 0%         | File opening   |
| Compose UI| NO         | NO                | NO       | YES          | 0%         | Screen renders |
*(Note: Automated testing is currently a major [GAP] and identified as technical debt.)*
