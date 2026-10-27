# Current work sequence

This is the verification sequence for the completed Office preview repair/expansion pass, not a promise of additional product features.

1. **Observe CI.** Confirm the final commit's `Office Renderer Tests` and `Android Build Verification` workflow results on the pull request. Fix failures with focused commits; do not weaken tests or security limits.
2. **Complete Android runtime verification.** On JDK 17 + SDK 34, run focused Gradle gates, install the APK on an emulator/device, and exercise [`SANITY_CHECKLIST.md`](SANITY_CHECKLIST.md) for PDF, DOCX, XLSX, PPTX, legacy Office, variants, external intents, archive entries, themes, print/share and safe save.
3. **Record evidence.** Update [`STATUS.md`](STATUS.md), [`TESTING.md`](TESTING.md), [`PERFORMANCE.md`](../PERFORMANCE.md) and [`CHANGELOG.md`](CHANGELOG.md) with exact commands, pass/fail counts, CI URL, device/WebView version and measured memory/timing where available.
4. **Address remaining gaps deliberately.** The general archive aggregate cap, Android WebView fuzzing, full legacy binary layout and full-fidelity Office editing are separate scoped tasks. Do not claim them as complete.

No version bump, new Gradle module, application-ID/SDK change, database migration or remote document service is introduced by this sequence.
