# Work remaining

The Office preview repair and expansion pass has implemented the three reported
root-cause fixes and added format/validation/UI coverage. The items below are
the remaining verification and compatibility work.

## Required runtime gates

- [ ] Observe the final commit's `Android Build Verification` CI result for debug/release compilation, release shrinking and JVM unit tests.
- [ ] Observe the final commit's `Office Renderer Tests` CI result for JavaScript syntax, OOXML validation, headless Chromium renderer tests and the `kotlin-inspector` JVM harness.
- [ ] On a JDK 17 + Android SDK 34 machine, run `compileDebugKotlin`, `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, `lintDebug`, `detekt` and `jacocoCoreVerify` and record each result separately.
- [ ] Install the APK on an emulator/device and open every supported Office format from local storage, an external content intent and a ZIP entry.
- [ ] Verify Android System WebView version, `WebViewAssetLoader` requests, absence of `file://`/network requests, search/navigation, zoom/pan, double-tap, dark/light themes, orientation, lifecycle, back/retry, share, print/export and safe save.

## Format-specific acceptance

- [ ] DOCX/DOCM/DOTX/DOTM: verify formatted visible content, search/count navigation, page jumps, pinch/double-tap zoom, dark mode, dedicated print and text-recovery retry.
- [ ] XLSX/XLSM/XLTX/XLTM: verify bounded grid, formulas-as-values, single-sheet light edit/save, read-only multi-sheet/macro/template notices, go-to-cell, copy, search, zoom and table print.
- [ ] PPTX/PPTM/PPSX/POTX/POTM: verify slide navigation, cross-slide search, images/tables, text fallback, dark mode, zoom/pan and all-slide print.
- [ ] DOC/XLS/PPT: verify bounded read-only text/grid extraction and honest identify-only fallback for unsupported/encrypted OLE2 variants.
- [ ] Corrupt, truncated, password-protected, misleading-extension and oversized files: verify explicit error/retry states and no crash.

## Performance and security follow-up

- [ ] Measure Android app heap and time-to-first-content for large DOCX/PPTX/XLSX files; browser heap figures are not Android memory measurements.
- [ ] Run an Android WebView fuzz/robustness pass with the committed adversarial corpus.
- [ ] Decide whether `ArchiveLimits.maxTotalBytes` should be enforced in the general archive extractor. The OOXML inspector has its own aggregate cap, but the general 16 GiB archive cap is still metadata-only.
- [ ] Consider removing the retained unused `xspreadsheet` asset after confirming no other viewer references it; preserve its license notice until then.
- [ ] Consider a full OOXML parser (for example POI) only if fidelity requirements justify its APK/memory cost; the current engines intentionally provide bounded preview rather than full Office editing.

## Scope boundary

Do not claim full Office editing, macro execution, legacy binary layout, chart
round-tripping, embedded media playback or unlimited large-file support. Keep
the single `FormatRegistry`, the existing safe-save/URI architecture, bundled
offline assets, security limits and the current design language.
