#!/usr/bin/env python3
"""Independent bounded OOXML package checks for the committed test corpus.

This is a Python mirror of the limits enforced by
`app/src/main/java/com/example/formats/ooxml/OoxmlPackageInspector.kt`. It is a
test oracle, not a second production registry: it never changes app routing.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path

MAX_PACKAGE_BYTES = 96 * 1024 * 1024
MAX_TOTAL_UNCOMPRESSED_BYTES = 512 * 1024 * 1024
MAX_SINGLE_UNCOMPRESSED_BYTES = 128 * 1024 * 1024
MAX_ZIP_ENTRIES = 10_000
MAX_XML_PART_BYTES = 2 * 1024 * 1024
MAX_RELATIONSHIP_PARTS = 512
MAX_COMPRESSION_RATIO = 2_000
MAX_NAME_LENGTH = 1_024
MAX_PATH_DEPTH = 32
OLE2_MAGIC = bytes([0xD0,0xCF,0x11,0xE0,0xA1,0xB1,0x1A,0xE1])

MAIN_PARTS = {
    'word/document.xml': 'WORD',
    'xl/workbook.xml': 'EXCEL',
    'ppt/presentation.xml': 'POWERPOINT',
}

class Invalid(Exception):
    pass

def unsafe_name(raw: str) -> bool:
    if not raw or len(raw) > MAX_NAME_LENGTH or '\x00' in raw:
        return True
    name = raw.replace('\\', '/')
    lower = name.lower()
    if name.startswith('/') or name.startswith('//') or '//' in name:
        return True
    if len(name) >= 2 and name[1] == ':' and name[0].isalpha():
        return True
    if any(token in lower for token in ('%2e%2e', '%2f', '%5c')):
        return True
    segments = name.split('/')
    if len(segments) > MAX_PATH_DEPTH:
        return True
    return any(part in ('.', '..') for part in segments)

def read_bounded(zf: zipfile.ZipFile, info: zipfile.ZipInfo, limit: int = MAX_XML_PART_BYTES) -> bytes:
    if info.file_size > limit:
        raise Invalid('XML part too large')
    with zf.open(info, 'r') as f:
        return f.read(limit + 1)

def inspect(path: Path) -> dict:
    size = path.stat().st_size
    if size <= 0:
        raise Invalid('empty package')
    if size > MAX_PACKAGE_BYTES:
        raise Invalid('package too large')
    with path.open('rb') as f:
        if f.read(8) == OLE2_MAGIC:
            raise Invalid('encrypted OLE2 package')
    with zipfile.ZipFile(path, 'r') as zf:
        infos = zf.infolist()
        if not infos or len(infos) > MAX_ZIP_ENTRIES:
            raise Invalid('invalid entry count')
        names = set()
        total = 0
        rels = 0
        for info in infos:
            if unsafe_name(info.filename):
                raise Invalid('unsafe entry name')
            if info.filename in names:
                raise Invalid('duplicate entry')
            names.add(info.filename)
            mode = (info.external_attr >> 16) & 0xF000
            if mode == 0xA000:
                raise Invalid('symlink entry')
            if info.flag_bits & 0x1:
                raise Invalid('encrypted ZIP entry')
            if info.is_dir():
                continue
            total += info.file_size
            if info.file_size > MAX_SINGLE_UNCOMPRESSED_BYTES:
                raise Invalid('entry too large')
            if total > MAX_TOTAL_UNCOMPRESSED_BYTES:
                raise Invalid('expanded package too large')
            if info.file_size > 4 * 1024 * 1024 and info.compress_size > 0:
                if info.file_size // info.compress_size > MAX_COMPRESSION_RATIO:
                    raise Invalid('unsafe compression ratio')
            if info.filename.endswith('.rels'):
                rels += 1
                if rels > MAX_RELATIONSHIP_PARTS:
                    raise Invalid('too many relationship parts')
                xml = read_bounded(zf, info).decode('utf-8', 'replace')
                if '<!DOCTYPE' in xml.upper():
                    raise Invalid('DOCTYPE in relationship part')
                for target in re.findall(r'Target\s*=\s*"([^"]+)"', xml, re.I):
                    low = target.lower()
                    if low.startswith(('file:', 'javascript:', 'data:', 'content:')):
                        raise Invalid('unsafe external relationship')
        if '[Content_Types].xml' not in names:
            raise Invalid('missing content types')
        ct_info = zf.getinfo('[Content_Types].xml')
        ct = read_bounded(zf, ct_info).decode('utf-8', 'replace')
        if '<!DOCTYPE' in ct.upper():
            raise Invalid('DOCTYPE in content types')
        main = next((p for p in MAIN_PARTS if p in names), None)
        if not main:
            raise Invalid('missing main Office part')
        main_xml = read_bounded(zf, zf.getinfo(main)).decode('utf-8', 'replace')
        if '<!DOCTYPE' in main_xml.upper():
            raise Invalid('DOCTYPE in main XML part')
        family = MAIN_PARTS[main]
        content_type = ''
        for part, ctype in re.findall(r'PartName\s*=\s*"([^"]+)"\s+ContentType\s*=\s*"([^"]+)"', ct, re.I):
            if part.lstrip('/') == main:
                content_type = ctype
                break
        if not content_type or not any(token in content_type for token in ('openxmlformats-officedocument', 'ms-word', 'ms-excel', 'ms-powerpoint')):
            raise Invalid('invalid main content type')
        lower = content_type.lower()
        has_macros = any(name.endswith('vbaProject.bin') for name in names) or 'macroenabled' in lower
        variant = 'addin' if 'addin' in lower else 'show' if 'slideshow' in lower else (
            'macro-template' if has_macros and 'template' in lower else
            'macro' if has_macros else 'template' if 'template' in lower else 'standard'
        )
        external_count = sum(
            1 for info in infos if info.filename.endswith('.rels') and
            'TargetMode="External"' in read_bounded(zf, info).decode('utf-8', 'replace')
        )
        return {
            'family': family,
            'variant': variant,
            'hasMacros': has_macros,
            'entryCount': len(infos),
            'uncompressedBytes': total,
            'externalRelationships': external_count,
        }

EXPECTED = {
    'basic.docx': ('ok', 'WORD', 'standard'),
    'rich.docx': ('ok', 'WORD', 'standard'),
    'basic.docm': ('ok', 'WORD', 'macro'),
    'basic.dotx': ('ok', 'WORD', 'template'),
    'poi/poi-60158.docm': ('ok', 'WORD', 'macro'),
    'poi/poi-60316.dotx': ('ok', 'WORD', 'template'),
    'poi/poi-sample.docx': ('ok', 'WORD', 'standard'),
    'basic.xlsx': ('ok', 'EXCEL', 'standard'),
    'rich.xlsx': ('ok', 'EXCEL', 'standard'),
    'large.xlsx': ('ok', 'EXCEL', 'standard'),
    'basic.xlsm': ('ok', 'EXCEL', 'macro'),
    'basic.xltx': ('ok', 'EXCEL', 'template'),
    'poi/poi-sample.xlsx': ('ok', 'EXCEL', 'standard'),
    'poi/poi-suffix.xlsm': ('ok', 'EXCEL', 'macro'),
    'basic.pptx': ('ok', 'POWERPOINT', 'standard'),
    'rich.pptx': ('ok', 'POWERPOINT', 'standard'),
    'basic.pptm': ('ok', 'POWERPOINT', 'macro'),
    'basic.potx': ('ok', 'POWERPOINT', 'template'),
    'poi/poi-51187.pptx': ('ok', 'POWERPOINT', 'standard'),
    'external-rel.docx': ('ok', 'WORD', 'standard'),
    'misleading.pdf': ('ok', 'WORD', 'standard'),
    'adversarial.docx': ('reject', None, None),
    'zip-bomb.docx': ('reject', None, None),
    'truncated.docx': ('reject', None, None),
    'corrupt.pptx': ('reject', None, None),
    'empty.xlsx': ('reject', None, None),
    'poi/poi-invalid.xlsx': ('reject', None, None),
}

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--fixtures', default='fixtures')
    parser.add_argument('--output', default=None)
    args = parser.parse_args()
    root = Path(args.fixtures).resolve()
    results = []
    failures = 0
    for name, (expected, family, variant) in sorted(EXPECTED.items()):
        path = root / name
        item = {'name': name, 'expected': expected, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else None}
        try:
            if not path.is_file():
                raise Invalid('fixture missing')
            info = inspect(path)
            item.update(status='ok', info=info)
            if expected != 'ok' or info['family'] != family or info['variant'] != variant:
                failures += 1
                item['failure'] = f"expected {expected}/{family}/{variant}, got ok/{info['family']}/{info['variant']}"
        except (Invalid, zipfile.BadZipFile, OSError, UnicodeError) as error:
            item.update(status='reject', reason=str(error))
            if expected != 'reject':
                failures += 1
                item['failure'] = f'expected ok, got reject: {error}'
        results.append(item)
    report = {'checked': len(results), 'failures': failures, 'results': results}
    text = json.dumps(report, indent=2, ensure_ascii=False) + '\n'
    if args.output:
        Path(args.output).write_text(text, encoding='utf-8')
    print(text)
    return 1 if failures else 0

if __name__ == '__main__':
    sys.exit(main())
