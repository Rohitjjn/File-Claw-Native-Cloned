# Database and user data

## Room database

`data/AppDatabase.kt` declares `AppDatabase` at schema **version 4** with the existing `RecentFileEntity` and `SettingEntity` tables. The database file is named `files_claw_database` in the app's private database storage.

- `RecentFileEntity` stores a file path, name, size, extension, open timestamp, sample flag, optional archive parent/entry information, and optional `sourceUri`.
- `SettingEntity` stores the app theme, reading/editor settings, encoding, history limit, and notification preferences.
- `AppRepository` is the existing DAO-facing repository.
- `MIGRATION_3_4` adds nullable `sourceUri` to `recent_files`.
- Only schemas 1 and 2 are configured for destructive recreation. Version 3 and later are expected to use explicit migrations; do not enable a general destructive fallback.

### Schema-export state

The checked-in schema export directory currently contains `app/schemas/com.example.data.AppDatabase/3.json`; an exported version-4 JSON file was not present during this documentation pass, even though the source database version is 4. The next successful KSP/Room build should be checked for a generated v4 schema and the migration test should be run. This is a recorded repository state, not an assertion that Room migration is broken. Do not change the schema version or migration as part of preview work without approval.

## Other persistence

- Favorite paths, search-term history, PDF page bookmarks, and a few viewer preferences use named `SharedPreferences` stores in their existing owners.
- Cache and temporary extraction files live in app cache directories and are not the source of user document contents.
- The app tracks paths/URIs and does not turn the Room recent-file record into a copy of each user document.
- Files opened from external providers may retain a `sourceUri`; the existing text/delimited save paths decide whether to write back or save an in-app copy. Preserve their current behavior.

## Backup behavior

The Android manifest sets `android:allowBackup="false"` and points to the app's backup/data-extraction rule XML files. No in-app cloud backup/restore flow is documented in current source. Do not promise cloud sync, automatic weekly backups, or a user-facing restore screen based on old project notes.

The repository ZIP listed in [`../backup-manager.md`](../backup-manager.md) is a development checkpoint of source files. It is unrelated to the app's Room database or a user's files.
