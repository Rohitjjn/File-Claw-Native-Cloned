// Minimal test shim for the subset of Apache Commons Compress used by
// OoxmlPackageInspector. This is NOT bundled into the app. It reads the ZIP
// central directory directly so the harness can exercise entry metadata
// (Unix mode and encryption flag) without pulling Maven dependencies into the
// offline test environment.
package org.apache.commons.compress.archivers.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public final class ZipFile implements AutoCloseable {
    private static final long EOCD_SIGNATURE = 0x06054b50L;
    private static final long CD_SIGNATURE = 0x02014b50L;

    private static final class Meta {
        final String name;
        final int unixMode;
        final boolean encrypted;
        Meta(String name, int unixMode, boolean encrypted) {
            this.name = name;
            this.unixMode = unixMode;
            this.encrypted = encrypted;
        }
    }

    private final java.util.zip.ZipFile delegate;
    private final List<ZipArchiveEntry> entries = new ArrayList<>();

    public ZipFile(File file) throws IOException {
        this.delegate = new java.util.zip.ZipFile(file);
        List<Meta> metas = readCentralDirectory(file);
        Enumeration<? extends java.util.zip.ZipEntry> rawEntries = delegate.entries();
        int index = 0;
        while (rawEntries.hasMoreElements()) {
            java.util.zip.ZipEntry raw = rawEntries.nextElement();
            Meta meta = index < metas.size() ? metas.get(index) : new Meta(raw.getName(), 0, false);
            entries.add(new ZipArchiveEntry(raw, meta.unixMode, meta.encrypted));
            index++;
        }
    }

    private static List<Meta> readCentralDirectory(File file) throws IOException {
        List<Meta> result = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            int tailLength = (int) Math.min(length, 65557L + 22L);
            byte[] tail = new byte[tailLength];
            raf.seek(length - tailLength);
            raf.readFully(tail);
            int eocd = -1;
            for (int i = tailLength - 22; i >= 0; i--) {
                if (u32(tail, i) == EOCD_SIGNATURE) {
                    int commentLength = u16(tail, i + 20);
                    if (i + 22 + commentLength == tailLength) {
                        eocd = i;
                        break;
                    }
                }
            }
            if (eocd < 0) return result;
            int total = u16(tail, eocd + 10);
            long cdSize = u32(tail, eocd + 12);
            long cdOffset = u32(tail, eocd + 16);
            if (total == 0xFFFF || cdSize == 0xFFFFFFFFL || cdOffset == 0xFFFFFFFFL) return result;
            if (cdOffset < 0 || cdOffset + cdSize > length || cdSize > Integer.MAX_VALUE) return result;
            byte[] cd = new byte[(int) cdSize];
            raf.seek(cdOffset);
            raf.readFully(cd);
            int position = 0;
            for (int i = 0; i < total; i++) {
                if (position + 46 > cd.length || u32(cd, position) != CD_SIGNATURE) break;
                int flags = u16(cd, position + 8);
                boolean encrypted = (flags & 0x1) != 0;
                int nameLength = u16(cd, position + 28);
                int extraLength = u16(cd, position + 30);
                int commentLength = u16(cd, position + 32);
                long externalAttributes = u32(cd, position + 38);
                int unixMode = (int) ((externalAttributes >>> 16) & 0xF000);
                int nameStart = position + 46;
                if (nameStart + nameLength > cd.length) break;
                String name = new String(cd, nameStart, nameLength, StandardCharsets.UTF_8);
                result.add(new Meta(name, unixMode, encrypted));
                position = nameStart + nameLength + extraLength + commentLength;
            }
        }
        return result;
    }

    private static int u16(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    private static long u32(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFFL) |
            ((bytes[offset + 1] & 0xFFL) << 8) |
            ((bytes[offset + 2] & 0xFFL) << 16) |
            ((bytes[offset + 3] & 0xFFL) << 24);
    }

    public Enumeration<ZipArchiveEntry> getEntries() {
        return Collections.enumeration(entries);
    }

    public ZipArchiveEntry getEntry(String name) {
        for (ZipArchiveEntry entry : entries) {
            if (entry.getName().equals(name)) return entry;
        }
        return null;
    }

    public InputStream getInputStream(ZipArchiveEntry entry) throws IOException {
        return delegate.getInputStream(entry.raw);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
