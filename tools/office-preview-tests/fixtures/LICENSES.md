# Office fixture sources and licenses

## Original generated fixtures — CC0-1.0

All files in this directory except the `poi/` subdirectory were generated for
this repository by `../generate_fixtures.py`. They contain only original test
text, synthetic images and deterministic binary data; no production or personal
documents are included. The generated content is released under CC0-1.0
(https://creativecommons.org/publicdomain/zero/1.0/).

The generated corpus includes small and rich DOCX/XLSX/PPTX files, macro-enabled
and template OOXML variants, Hindi/Arabic/emoji Unicode, tables, images, charts,
formulas, multiple sheets/slides, a 5,001-row workbook, and adversarial cases:
truncated ZIPs, corrupt packages, misleading extensions, path traversal, XXE
DOCTYPE declarations, external relationships, and a compressible ZIP bomb.

The Python libraries used only during regeneration are installed from
`../requirements.txt`: python-docx (MIT), openpyxl (MIT), python-pptx (MIT),
xlwt (BSD-style), and Pillow (HPND). They are not app dependencies and are not
committed here.

## Apache POI fixtures — Apache-2.0

`poi/` contains small public test files copied from the Apache POI repository:

- Source: https://github.com/apache/poi/tree/trunk/test-data
- License: Apache License 2.0; the full text is in [`poi/LICENSE.txt`](poi/LICENSE.txt).
- Files used: two legacy `.doc` samples, one legacy `.ppt` sample, one legacy
  `.xls` sample, one `.docm`, one `.dotx`, one `.xlsm`, one `.pptx`, and small
  valid/invalid `.xlsx`/`.docx` samples.

They are included only to exercise detection, read-only extraction and error
handling. They are never used as app production assets. The filenames are
recorded in `../fixtures-manifest.json`.
