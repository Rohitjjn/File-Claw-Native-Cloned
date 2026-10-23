# Manual sanity checklist (run on a device/emulator before each release)

Mark each with ✅ / ❌ + notes. Items marked ★ are regressions fixed in 1.1.0; they must be re-verified on a real device first.

## Open
- [ ] Cold start to Home, no crash. Rotate on Home, nothing re-opens. ★
- [ ] From a file manager: "Open with → File Claw" on a PDF, a `.docx`, a `.csv`. Works **without** granting all-files access. ★
- [ ] Rotate while the opened file is showing: it is not re-imported. ★
- [ ] Open the same file twice from outside: one copy, no `name (1)` clutter unless sizes differ.
- [ ] Rename a PNG to `.jpg`, open it: shows as an image. A text file renamed `.pdf` opens as text. A binary renamed `.txt` shows hex with a note.
- [ ] Sample of new formats: `.sqlite`, `.svg`, `.rtf`, `.odt`, `.epub`, `.ttf`, `.stl`, `.obj`, `.tar.xz`, `.deb`, `.apk` (as archive), `.gz` (single file).
- [ ] `.rar` / `.doc` / `.xls` show a clear "not supported" notice plus hex, never an empty screen. ★

## Edit and save (the data-loss checks) ★
- [ ] Open a 1 MB text file: banner says truncated + read-only; Edit button hidden; nothing can be saved.
- [ ] Open a 50 KB UTF-8-with-BOM file, edit, save: BOM and encoding preserved (compare bytes).
- [ ] Open a UTF-16 text file: displays correctly (not "binary").
- [ ] CSV: edit a cell, **no** auto-save; "Save" appears; Back prompts Save / Discard / Cancel; Save writes once.
- [ ] TSV and a `;`-delimited CSV: columns split correctly and round-trip.
- [ ] XLSX with 2+ sheets: "View only" banner, cannot edit, file unchanged on disk.
- [ ] Edit a text file inside a ZIP, save, reopen the ZIP: change persisted; other entries intact.
- [ ] Kill the app during a save of a large file (adb `am force-stop`): original file is intact.
- [ ] A file opened from another app: save shows "Saved to the in-app copy" and the original is unchanged.

## Archives
- [ ] ZIP with 100k entries opens without ANR. Zip bomb sample is refused with a message, no crash/OOM.
- [ ] Entry named `../../x` is shown flagged and cannot be extracted.
- [ ] Encrypted ZIP and 7z: password prompt; wrong password gives an error; right password opens.

## UI / Design
- [ ] Light and dark: cards, app bar, permission screen are legible; contrast acceptable.
- [ ] Scrolling long lists feels smooth (no visible jank on a mid-range device).
- [ ] HTML preview: "Scripts are off for safety" banner; "Run scripts" enables them for that file only.
- [ ] Markdown with `<script>` / `onerror` renders without executing.

## Housekeeping
- [ ] Settings → clear cache works; open files are not deleted from under you.
- [ ] "Remove from history" returns to Home and does **not** re-open the file. ★
- [ ] App backup disabled: `adb shell dumpsys backup` shows no data for the package.
