// Minimal test shim for the subset of Apache Commons Compress used by
// OoxmlPackageInspector. This is NOT bundled into the app. It reads the ZIP
// central directory directly so the harness can exercise entry metadata
// (Unix mode and encryption flag) without pulling Maven dependencies into the
// offline test environment.
package org.apache.commons.compress.archivers.zip;

public final class ZipArchiveEntry {
    final java.util.zip.ZipEntry raw;
    private final int unixMode;
    private final boolean encrypted;

    ZipArchiveEntry(java.util.zip.ZipEntry raw, int unixMode, boolean encrypted) {
        this.raw = raw;
        this.unixMode = unixMode;
        this.encrypted = encrypted;
    }

    public String getName() { return raw.getName(); }
    public boolean isDirectory() { return raw.isDirectory(); }
    public long getSize() { return raw.getSize(); }
    public long getCompressedSize() { return raw.getCompressedSize(); }
    public int getUnixMode() { return unixMode; }
    public GeneralPurposeBit getGeneralPurposeBit() { return new GeneralPurposeBit(encrypted); }

    public static final class GeneralPurposeBit {
        private final boolean encrypted;
        GeneralPurposeBit(boolean encrypted) { this.encrypted = encrypted; }
        public boolean usesEncryption() { return encrypted; }
    }
}
