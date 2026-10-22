# File Claw - Security Protocol
Last Updated: June 15, 2026
Document Version: 1.0
Next Review Date: December 15, 2026

## 1. Threat Model
- **Assets**: User's local file system data, parsed document contents (PDFs, DOCX), stored settings.
- **Threats**: 
  - Malicious files exploiting parsers (e.g., zip bombs).
  - Unauthorized reads of temporary decrypted cached files.
- **Risk**: Low/Medium. App is offline, but reads external payload constraints.
- **Trust Boundaries**: The OS file system is trusted. Imported/selected files from arbitrary sources are strictly untrusted.

## 2. Permission Security
- `READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE`: 
  - *Why*: Mandatory for core functionality (broad file management).
  - *Denial*: Splash screen blocks entry.
- **Minimization**: No network permissions (`INTERNET`), no location, no contacts. The app is strictly isolated to storage.

## 3. File Access Security
- **Path Validation**: When resolving `allFiles/{path}`, the path MUST be validated to not escape the root storage directory using `../` (Directory Traversal).
- **Validation**: Sniff extensions rigorously. Do not execute or evaluate content (e.g., no executing `.sh` or `.apk` files from the editor).
- **Sandbox Boundaries**: App respects `Context.filesDir` bounds for internal state.

## 4. Data Storage Security
- **Cache Security**: Passwords used to decrypt PDFs MUST NOT be saved to disk. Decrypted output files in `cacheDir` MUST be explicitly deleted when the `FilePreviewScreen` is disposed.
- **Database**: Room DB (`app_database`) is stored in internal storage. No external process can access it on unrooted devices.
- **SQL Injection**: Prevented globally by Room's compilation parameterization of DAO queries.

## 5. Input Validation Rules
- **Settings Values**: Limited string lengths, strict type casting.
- **Search Queries**: Maximum length 100 characters. Sanitized for Regex execution bounds to prevent ReDoS.
- **Zip Extractions**: Prevent Zip Slip vulnerabilities. Extracted paths MUST be validated to ensure they are children of the designated output directory.

## 6. Output Safety
- **Logs**: `Log.d` or `Log.e` MUST NEVER contain file content snippets, user passwords, or decrypted streams.
- **File Sharing**: (Planned feature) Will utilize `FileProvider` with temporary read-only URIs (`FLAG_GRANT_READ_URI_PERMISSION`). Absolute file paths will not be exposed via Intents.

## 7. Network Security
- **Current Status**: OFFLINE. The app requests no network permissions.

## 8. Cryptography Policy
- **PDF Decryption**: Handled entirely by `PDFBox`. Keys/passwords are kept in memory (`CharArray` or minimal runtime String) and garbage collected.
- **Backups**: (Future) Cloud backups MUST utilize AES-256 before transmission. Local backups of standard configs remain unencrypted SQLite.

## 9. Vulnerability Response Plan
- **Discovery**: Monitored via crash logs and Android Vitals.
- **Timeline**: Critical vulnerabilities patched within 48 hours of detection.
- **Disclosure**: Immediate changelog mention and Play Store release notes for critical security updates.

## 10. Privacy Compliance
- **Data Collection**: ZERO telemetry, analytics, or crashlytics integrated.
- **Data Retention**: App data retained until app uninstallation or explicit user clear.

## 11. Security Checklist (Pre-Release)
- [ ] Verify `INTERNET` permission is absent from Manifest.
- [ ] Verify `cacheDir` cleanup routines are functional.
- [ ] Verify Logcat output is extremely minimal and devoid of PII or file contents.
- [ ] Run standard Android Lint with security rules enabled.

---
**Update Log**
- 2026-06-15: v1.0 - Initial document creation.
