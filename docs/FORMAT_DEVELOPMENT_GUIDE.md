# Working on one format

**Preparation checkpoint:** dedicated format handlers are not implemented yet. Use the current-path
columns of FORMAT_PACKAGE_MAP and FILE_MIGRATION_MAP until each family is actually migrated.

1. Establish the build baseline from BUILD_GUIDE and inspect known-issues.
2. Select one registry ID or genuine shared-engine family. Trace its detector, ViewModel branch,
   state, preview, search, save and print callers. Do not equate an extension label with support.
3. Create representative valid, malformed, truncated and oversized fixtures. Preserve current
   behavior with tests before moving code. Use synthetic fixtures without private user data.
4. Extract that family's existing logic to `com.example.formats.<family>`; move its tests with it.
   Depend on centralized bounded I/O, atomic writes, path/security checks and shared contracts.
   Do not add a new engine as part of architecture migration.
5. Keep Android rendering separate from pure parser tests. Preserve assets and WebView URLs.
   Route through the existing registry once the common handler contract is approved and tested.
6. Verify unsupported operations remain disabled, read-only reasons remain visible, and close/
   cancellation release all owned resources. Preserve errors and source write-back semantics.
7. Compile, run relevant tests, then full unit/build/lint/coverage gates and device regression checks.
   Stop at a failing or unavailable gate; do not mark untested code complete.
8. Update ownership, migration status, known limitations and changelog. Regenerate the registry
   matrix with `tools/GenFormatDocs.kt` when registry metadata changes; do not hand-edit that output.

Do not touch unrelated formats, change SDK/dependencies/application ID, or advertise expanded support
without a separate approved task. After migration, develop and approve one format at a time.
