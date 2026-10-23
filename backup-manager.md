# File Claw - Backup & Recovery Manager
Last Updated: June 15, 2026
Document Version: 1.0
Next Review Date: December 15, 2026

## 1. Backup Scope Definition
- **CRITICAL (Must Backup)**
  - Room Database (`RecentFileEntity` and `SettingEntity` data)
  - Edited files that have not been explicitly exported by the user
  - User-configured keys and custom preferences
  - *Retention Policy*: Indefinitely or until user clears app data.
- **IMPORTANT (Should Backup)**
  - Search indexes (speeds up cold starts)
  - Last viewed positions in large documents
  - *Retention Policy*: 30 days of inactivity.
- **OPTIONAL (Can Backup)**
  - Decrypted PDF caches (if configured to persist temporarily)
  - Extracted ZIP payloads currently actively viewed
  - *Retention Policy*: Clear on exit or memory pressure.
- **NEVER Backed Up**
  - Bitmaps, image caches, temporary hex dumps.
  - Crash dumps, Logcat outputs.

## 2. Backup Architecture
- **Local Backup Strategy**: Backups of the Room DB (`app_database`) are copied to the `Context.getExternalFilesDir("Backups")` directory. 
- **Cloud Backup Strategy**: [TODO] Google Drive integration is planned for Q4 2026. AES-256 encryption will be mandatory for cloud uploads.
- **Triggers**: 
  - Manual: Triggered from `SettingsScreen`.
  - Auto: Automatic weekly backup of Room DB to local storage if enabled in settings.
- **Format**: SQLite copy for the database. JSON for exported settings.

## 3. Database Backup Protocol
- **Procedure**:
  1. Acquire lock on database transactions.
  2. Perform `wal_checkpoint(TRUNCATE)` on Room SQLite DB.
  3. Copy `app_database`, `app_database-wal`, and `app_database-shm` to the backup directory.
  4. Append `.bak_yyyyMMdd_HHmmss` to the copied files.
  5. Release lock.
- **Validation**: Calculate SHA-256 checksum of the copied main DB file and verify it matches the active DB at the moment of copy.
- **Schedule**: Manual via Settings, or Weekly if Auto-Backup is enabled.
- **Rollback**: To restore, the app MUST be restarted. The app copies the `.bak` files back to the internal `databases/` directory before Room initialization.

## 4. User Files Backup Protocol
- **Imported Files**: Living outside the app's sandboxed storage, they rely on the OS backup mechanism. The app tracks URIs/paths, not the files themselves.
- **Edited Files**: If a file is opened, edited, and saved, the original is overwritten by default. 
- **ZIP Extracted Files**: Reside in `Context.cacheDir`. They are strictly temporary and are explicitly destroyed on app exit or during `trimMemory`.

## 5. Settings and Preferences Backup
- **Storage**: Persisted utilizing Room (`SettingEntity`).
- **Data Types**: Theme preferences, default encodings, layout toggles.
- **Migration**: Room migrations (`Migration(1, 2)`) MUST be explicitly defined to handle schema changes for settings.
- **Defaults**:
  - `theme`: "system"
  - `default_encoding`: "UTF-8"
  - `show_hidden_files`: "false"

## 6. Search Index Backup
- **Location**: In-memory cache backed by a serialized Room entity or JSON file in `cacheDir`.
- **Rebuild vs Restore**: The index is restored on launch. If the index is older than 24 hours or corruption is detected, a full rebuild is triggered globally in `Dispatchers.Default`.
- **Performance Impact**: Restoring takes < 100ms. Rebuilding blocks search functionality but operates non-blocking to the main UI.

## 7. Backup Failure Handling
- **Retry Logic**: If local backup fails (e.g., IO Exception), retry after 1 hour. Maximum 3 retries.
- **Storage Full**: Notify user via Snackbar. Delete oldest `.bak` files automatically if total backup size > 500MB.
- **Permission Loss**: Handled gracefully. If `WRITE_EXTERNAL_STORAGE` is denied, disable Auto-Backup and prompt user.
- **Corrupted Backup**: Detected via SHA-256 mismatch during scheduled verification. Mark as `[CORRUPTED]` and notify the user to run a manual backup.

## 8. Restore Procedures
- **Complete Restore**: 
  1. Fresh install.
  2. Navigate to Settings -> Restore.
  3. Select backup ZIP or folder.
  4. App validates schema version.
  5. App replaces active `app_database` files and restarts process.
- **Validation**: Upon restart, the app runs a query `SELECT COUNT(*) FROM RecentFileEntity` to verify data population.

## 9. Export/Import Protocol
- **Export Format**: Standard `.zip` containing the SQLite DB, settings JSON, and a `manifest.json`.
- **Import Validation**: Reject ZIP if `manifest.json` is missing or version mismatch is > 1 major version.
- **Conflict Resolution**: Importing a backup is an OVERWRITE operation. Existing local data is destroyed.

## 10. Disaster Recovery Plan
- **Loss Scenarios**:
  - *App Data Cleared*: Restore from `ExternalFilesDir` backup if intact.
  - *Phone Reset*: Unrecoverable unless the user manually exported the backup block to a PC or secondary device.
- **User Communication**: Explicitly state "The app does not use cloud sync; your data is local" on the first launch.
- **Prevention**: Enforce weekly auto-backup prompts if not configured.

---
**Update Log**
- 2026-06-15: v1.0 - Initial document creation.
- 2026-06-16: v1.1 - Added indices and WAL configuration, updated backup history logging for v1.0.1 hotfix.
- 2026-06-17: v1.2 - Replaced native Markdown renderer with WebView (Claude styling), logged as v1.0.2 backup.
