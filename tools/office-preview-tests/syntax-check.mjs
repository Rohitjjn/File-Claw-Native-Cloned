import fs from 'node:fs';
import fsp from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, '../..');
const assetsRoot = path.join(repoRoot, 'app/src/main/assets');
const tempDir = await fsp.mkdtemp(path.join(os.tmpdir(), 'fc-js-'));
const checked = [];

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full);
    else if (entry.name.endsWith('.js') || entry.name.endsWith('.mjs')) checked.push(full);
  }
}
walk(assetsRoot);

const htmlFiles = [
  path.join(assetsRoot, 'editor/docx_editor.html'),
  path.join(assetsRoot, 'editor/pptx_editor.html'),
  path.join(assetsRoot, 'spreadsheet/grid.html'),
  path.join(assetsRoot, 'pdfjs/search.html')
];
const inlineRegex = /<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/gi;
for (const [fileIndex, htmlFile] of htmlFiles.entries()) {
  const html = await fsp.readFile(htmlFile, 'utf8');
  let match;
  let index = 0;
  while ((match = inlineRegex.exec(html)) !== null) {
    const file = path.join(tempDir, `${path.basename(htmlFile, '.html')}-${fileIndex}-${index++}.js`);
    await fsp.writeFile(file, match[1], 'utf8');
    checked.push(file);
  }
}

for (const file of checked) {
  execFileSync(process.execPath, ['--check', file], { stdio: 'pipe' });
}
console.log(`node --check passed for ${checked.length} standalone and inline JavaScript assets.`);
await fsp.rm(tempDir, { recursive: true, force: true });
