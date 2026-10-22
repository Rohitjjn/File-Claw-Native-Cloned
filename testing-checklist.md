# File Claw - Testing Checklist
Last Updated: June 15, 2026
Document Version: 1.0
Next Review Date: December 15, 2026

## 1. Testing Philosophy
- **Definition of Done**: Code compiles without warnings, passes all unit tests, meets performance benchmarks, and passes manual QA for critical paths without visual regression.
- **Testing Pyramid**: Target 60% Unit (ViewModels/Parsers), 30% Integration (Room/FileManager), 10% Manual UI QA.
- **Regression**: Any change to `FileManager` mandates a full re-test of all file format loading states.

## 2. Pre-Release Checklist (MANDATORY)

| Check Item | How to Test | Expected Result | Pass/Fail | Notes |
|------------|-------------|-----------------|-----------|-------|
| Permission Gate | Clear app data, launch app. | Splash requires permission before Home. | | |
| File Caching | Open a file, verify in Recent list. | File appears at top of Home screen. | | |
| PDF Render | Open heavy PDF. | Renders in < 1s, scrolling is smooth. | | |
| PDF Password | Open encrypted PDF. | Dialog appears, unlocking works via PDFBox. | | |
| DOCX Parsing | Open `.docx` file. | Text extracts successfully. | | |
| Text Editing | Open `.md`, edit, save. | Changes persist. Reopening shows new text. | | |
| ZIP Browsing | Open `.zip` file. | Shows directory tree of zip contents. | | |
| Search | Type in Search screen. | Results filter rapidly. | | |
| Theme Toggle | Switch Night Mode in Settings. | UI colors invert successfully. | | |
| Rotation | Rotate device while viewing PDF. | Doesn't crash, state is restored. | | |

*(Note: Minimum 50 specific checks are required. Abbreviated here for document scale, full matrix managed in QA software.)*

## 3. Unit Test Requirements
- **Must Have**: `AppRepository` flows, `MainViewModel` state reductions, `DocxElement` parsing logic.
- **Coverage Target**: 60% overall lines covered (Tech Debt: Currently 0%).
- **Mocking**: Mockk preferred for `Context` and `FileManager` abstractions.

## 4. Integration Test Requirements
- **Database**: Run Room testing suite to verify `Migration(1, 2)` mechanisms.
- **I/O**: Write temporary files during instrumentation tests, read them, and assert content matches.

## 5. UI Test Requirements
- **Automated**: Use Compose UI Testing (`createComposeRule`) to verify route navigation (Splash -> Home -> FilePreview).
- **Visuals**: Roborazzi screenshot verification for custom components (`ClaudeAppBar`, Error States).

## 6. Performance Test Requirements
- **Cold Start**: Measure `Activity.reportFullyDrawn()`. Target < 1.0s on reference device (Pixel 6+).
- **Memory**: Open 100MB PDF. Check Android Profiler. Heap MUST not exceed 256MB.
- **Scroll Jank**: Monitor `LazyColumn` frame drops in `AllFilesScreen`. Ensure < 5% dropped frames.

## 7. Manual Test Scenarios
- **Stress**: Place 2,000 `.txt` files in a folder. Open folder in `AllFilesScreen`. Scroll rapidly.
- **Interruption**: Leave `EditorScreen` with unsaved changes via Home Button. Kill app. (Ensure state loss is acceptable or mitigated).

## 8. Edge Case Catalog
- **File**: Opening 0-byte file (Should show empty state, not crash).
- **File**: Corrupted ZIP file (Should show `FileContentState.Error`).
- **File**: Opening a file without extension (Should binary dump or hex view).

## 9. Bug Reporting Template
- **Title**: [Screen/Component] Brief summary 
- **Steps**: 1... 2... 3...
- **Expected**: What should happen.
- **Actual**: What crashed or broke.
- **Device details**: API Level, Device Model.
- **Severity**: Blocker | Critical | Major | Minor | Trivial

## 10. Release Sign-Off
- Only designated Lead Maintainers can approve.
- Exceptions to failing checks require documented rationale in the Release Notes.

## 11. Testing Schedule
- **Weekly**: Full manual QA run on emulator.
- **Pre-Release**: Mandatory full execution of this document.

---
**Update Log**
- 2026-06-15: v1.0 - Initial document creation.
