# Current work sequence

This is a verification sequence, not a promise of future product features.

1. **Complete the existing preview repair gate.** Run the Kotlin/Gradle checks on JDK 17 + SDK 34, then open existing PDF and DOCX examples in an Android WebView. The current MIME handler change is not runtime-confirmed yet.
2. **Confirm app regression behavior.** Use [`SANITY_CHECKLIST.md`](SANITY_CHECKLIST.md) to check external file opening, editing/saving, archives, search, print/share, Room migration behavior, and themes. Record failures rather than inferring success from source.
3. **Update documentation only from evidence.** Keep [`STATUS.md`](STATUS.md), [`FILE_FORMATS.md`](FILE_FORMATS.md), and [`DATABASE.md`](DATABASE.md) aligned with implementation and observed tests.
4. **Only after the gate passes**, discuss any further architecture or product work with the user. A new format or preview engine requires separate scope approval.

No version bump, new module, dependency change, database migration, or new preview capability is planned by this document.
