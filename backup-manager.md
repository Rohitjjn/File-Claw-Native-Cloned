# Repository checkpoint and app backup notes

## Source checkpoint for the preview and documentation task

- Workspace-only archive (not included in Git or the pull request): `backup/backup_v1.3.0_pre_docx_pdf_docs_2026-10-23.zip`.
- Workspace-only context record (not included in Git or the pull request): `backup/context_v1.3.0_pre_docx_pdf_docs_2026-10-23.txt`.
- The archive was made before this task's DOCX/PDF source changes and documentation refresh. `zip -T` passed. It contains 630 files and 17,548,104 uncompressed bytes (about 5.3 MiB compressed).
- At archive time the Git base was `ec4d91ca50d51749429970bf8caf631cc1439db0` on the Arena-managed branch `arena/29f255df-file-claw-native-cloned`. The archive includes the preceding uncommitted architecture migration; it is not a clean copy of Git `HEAD`.
- The checkpoint does not retroactively satisfy any earlier task's pre-edit archive requirement.

## App data backup behavior observed in source

The app manifest currently sets `android:allowBackup="false"` and references `app/src/main/res/xml/backup_rules.xml` and `data_extraction_rules.xml`. No in-app backup/restore screen or cloud-backup implementation was found in the reviewed project source. Room data uses the private `files_claw_database` database; see [`docs/DATABASE.md`](docs/DATABASE.md).

Do not describe the repository ZIP as a user-data backup. Do not promise weekly backups, cloud restore, or export/import behavior unless those features are implemented and verified in source.

## Verification blocker

The baseline Android Gradle commands could not start in the task environment because neither `JAVA_HOME` nor a `java` executable was available. Exact commands and the current post-change result are recorded in [`docs/STATUS.md`](docs/STATUS.md).
