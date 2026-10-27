import test, { after, before } from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
import chromium, { inflate, setupLambdaEnvironment } from '@sparticuz/chromium';
import puppeteer from 'puppeteer-core';

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, '../..');
const assetsRoot = path.join(repoRoot, 'app/src/main/assets');
const fixturesRoot = path.join(here, 'fixtures');
const payloadsRoot = path.join(here, 'payloads');
const results = [];

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript',
  '.mjs': 'text/javascript',
  '.css': 'text/css',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2'
};

function safeJoin(root, rel) {
  const target = path.resolve(root, rel);
  if (!target.startsWith(root + path.sep) && target !== root) return null;
  return target;
}

function fixtureFor(req, url, key, fallback) {
  const referer = req.headers.referer ? new URL(req.headers.referer) : null;
  return url.searchParams.get(key) || referer?.searchParams.get(key) || fallback;
}

const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, 'http://127.0.0.1');
    let filePath = null;
    let mime = 'application/octet-stream';
    if (url.pathname.startsWith('/assets/')) {
      filePath = safeJoin(assetsRoot, url.pathname.slice('/assets/'.length));
      mime = MIME[path.extname(filePath || '').toLowerCase()] || 'application/octet-stream';
    } else if (url.pathname === '/docx-src/document.docx') {
      filePath = safeJoin(fixturesRoot, fixtureFor(req, url, 'file', 'basic.docx'));
    } else if (url.pathname === '/pptx-src/presentation.pptx') {
      filePath = safeJoin(fixturesRoot, fixtureFor(req, url, 'file', 'basic.pptx'));
    } else if (url.pathname === '/spreadsheet-src/data.json') {
      filePath = safeJoin(payloadsRoot, fixtureFor(req, url, 'payload', 'basic.json'));
      mime = 'application/json; charset=utf-8';
    }
    if (!filePath) {
      res.writeHead(403); res.end('blocked'); return;
    }
    const data = await fs.readFile(filePath);
    res.writeHead(200, {
      'Content-Type': mime,
      'Content-Length': String(data.length),
      'Cache-Control': 'no-store'
    });
    res.end(data);
  } catch {
    res.writeHead(404); res.end('not found');
  }
});

let browser;
let baseUrl;

before(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
  const require = createRequire(import.meta.url);
  const moduleEntry = require.resolve('@sparticuz/chromium');
  const binDir = path.join(path.dirname(moduleEntry), '..', 'bin');
  await inflate(path.join(binDir, 'al2023.tar.br'));
  setupLambdaEnvironment(path.join(os.tmpdir(), 'al2023', 'lib'));
  browser = await puppeteer.launch({
    executablePath: await chromium.executablePath(),
    args: await puppeteer.defaultArgs({ args: chromium.args, headless: 'shell' }),
    headless: 'shell',
    defaultViewport: { width: 390, height: 844, deviceScaleFactor: 1 }
  });
});

after(async () => {
  if (browser) await browser.close();
  await new Promise(resolve => server.close(resolve));
  await fs.mkdir(path.join(here, 'results'), { recursive: true });
  await fs.writeFile(
    path.join(here, 'results/renderer-tests.json'),
    JSON.stringify({ cases: results.length, results }, null, 2) + '\n'
  );
});

async function newPage(options = {}) {
  const page = await browser.newPage();
  await page.setViewport({
    width: options.width || 390,
    height: options.height || 844,
    deviceScaleFactor: 1
  });
  const logs = [];
  const externalRequests = [];
  page.on('console', message => {
    const text = message.text();
    if (message.type() === 'error' || message.type() === 'warning') logs.push({ type: message.type(), text });
  });
  page.on('pageerror', error => logs.push({ type: 'pageerror', text: String(error) }));
  page.on('request', request => {
    if (!request.url().startsWith(baseUrl) && !request.url().startsWith('data:')) {
      externalRequests.push(request.url());
    }
  });
  await page.evaluateOnNewDocument(noResizeObserver => {
    if (noResizeObserver) {
      try { delete window.ResizeObserver; } catch (_) {}
    }
    window.__bridge = {
      loaded: [], printReady: 0, errors: [], pages: [], slides: [], searches: [],
      zoom: [], fallback: [], edits: [], clipboard: [], gridReady: []
    };
    window.Android = {
      onDocumentLoaded: n => window.__bridge.loaded.push(n),
      onPrintReady: () => { window.__bridge.printReady++; },
      onPageChanged: (a, b) => window.__bridge.pages.push([a, b]),
      onSlideChanged: (a, b) => window.__bridge.slides.push([a, b]),
      onSearchResultCount: (a, b, c) => window.__bridge.searches.push([a, b, c]),
      onZoomChanged: z => window.__bridge.zoom.push(z),
      onDocumentError: e => window.__bridge.errors.push(String(e)),
      onDocumentTap: () => {}, onDocumentDoubleTap: () => {},
      onRendererFallback: v => window.__bridge.fallback.push(v),
      onGridReady: (r, c) => window.__bridge.gridReady.push([r, c]),
      onGridError: e => window.__bridge.errors.push(String(e)),
      onCellChanged: (r, c, t) => window.__bridge.edits.push([r, c, t]),
      onSelectionChanged: () => {},
      copyToClipboard: t => window.__bridge.clipboard.push(t),
      onGridTap: () => {},
      getDocxSize: () => 0, getDocxChunk: () => '', getDocxBase64: () => '',
      exitWithoutSaving: () => {}
    };
  }, Boolean(options.noResizeObserver));
  return { page, logs, externalRequests };
}

async function loadDocx(ctx, options = {}) {
  const { page } = ctx;
  const file = options.file || 'basic.docx';
  const corpus = options.corpus || 'generated';
  const print = Boolean(options.print);
  const url = `${baseUrl}/assets/editor/docx_editor.html?file=${encodeURIComponent(file)}&corpus=${encodeURIComponent(corpus)}${print ? '&print=1' : ''}`;
  await page.goto(url, { waitUntil: 'networkidle0', timeout: 30_000 });
  await page.evaluate((w, h) => {
    window.notifyViewport(w, h);
    window.setNotice('Read-only preview notice');
    window.setZoom(1);
    window.loadDocx(null, false);
  }, print ? 1122 : 390, print ? 794 : 844);
  if (options.lateViewport) {
    await new Promise(r => setTimeout(r, 250));
    await page.setViewport({ width: 390, height: 844, deviceScaleFactor: 1 });
    await page.evaluate(() => window.notifyViewport(390, 844));
  }
  await page.waitForFunction(() => window.__bridge.loaded.length || window.__bridge.errors.length, { timeout: 30_000 });
  await new Promise(r => setTimeout(r, 150));
  return page.evaluate(() => {
    const first = document.querySelector('#output .docx');
    const rect = first?.getBoundingClientRect();
    return {
      bridge: window.__bridge,
      pages: document.querySelectorAll('#output .docx').length,
      firstRect: rect ? { width: rect.width, height: rect.height } : null,
      visible: first ? getComputedStyle(first).visibility : null,
      notice: document.getElementById('notice').textContent,
      bodyText: (document.body.innerText || '').slice(0, 400)
    };
  });
}

async function loadPptx(ctx, options = {}) {
  const { page } = ctx;
  const file = options.file || 'basic.pptx';
  const corpus = options.corpus || 'generated';
  const print = Boolean(options.print);
  const url = `${baseUrl}/assets/editor/pptx_editor.html?file=${encodeURIComponent(file)}&corpus=${encodeURIComponent(corpus)}${print ? '&print=1' : ''}`;
  await page.setViewport({ width: print ? 1122 : 390, height: print ? 794 : 844, deviceScaleFactor: 1 });
  await page.goto(url, { waitUntil: 'networkidle0', timeout: 30_000 });
  await page.evaluate((w, h) => {
    window.notifyViewport(w, h);
    window.setNotice('Read-only preview notice');
    window.loadPptx(null, false);
  }, print ? 1122 : 390, print ? 794 : 844);
  await page.waitForFunction(() => window.__bridge.loaded.length || window.__bridge.errors.length, { timeout: 30_000 });
  if (print) await page.waitForFunction(() => window.__bridge.printReady > 0, { timeout: 15_000 });
  await new Promise(r => setTimeout(r, 150));
  return page.evaluate(() => ({
    bridge: window.__bridge,
    slideWrappers: document.querySelectorAll('.pptx-preview-slide-wrapper').length,
    fallbackCards: document.querySelectorAll('.fallback-card').length,
    images: document.querySelectorAll('#editor-container img').length,
    tables: document.querySelectorAll('#editor-container table').length,
    statusHidden: document.getElementById('status').classList.contains('hidden'),
    bodyText: (document.body.innerText || '').slice(0, 400)
  }));
}

async function loadGrid(ctx, payload = 'basic.json') {
  const { page } = ctx;
  await page.goto(`${baseUrl}/assets/spreadsheet/grid.html?payload=${encodeURIComponent(payload)}`, {
    waitUntil: 'domcontentloaded', timeout: 30_000
  });
  await page.waitForFunction(() => window.__bridge.gridReady.length || window.__bridge.errors.length, { timeout: 30_000 });
  await new Promise(r => setTimeout(r, 100));
  return page.evaluate(() => ({
    bridge: window.__bridge,
    visibleCells: document.querySelectorAll('.cell').length,
    headers: document.querySelectorAll('.header-cell').length,
    injected: document.querySelectorAll('#grid-shell img, #grid-shell script').length,
    statusHidden: document.getElementById('status').classList.contains('hidden'),
    notice: document.getElementById('notice').textContent
  }));
}

function record(name, status, details) {
  results.push({ name, status, details });
}

test('DOCX renders a visible page and reports readiness', { concurrency: false }, async () => {
  const ctx = await newPage();
  try {
    const state = await loadDocx(ctx, { file: 'basic.docx' });
    assert.equal(state.bridge.errors.length, 0);
    assert.equal(state.bridge.loaded[0], 1);
    assert.equal(state.pages, 1);
    assert.ok(state.firstRect.width > 100 && state.firstRect.height > 100);
    assert.equal(state.visible, 'visible');
    assert.match(state.bodyText, /File Claw Word fixture/);
    record('docx-normal', 'pass', { pages: state.pages, firstRect: state.firstRect, logs: ctx.logs });
  } finally { await ctx.page.close(); }
});

test('DOCX survives a late Compose layout without ResizeObserver', { concurrency: false }, async () => {
  const ctx = await newPage({ width: 1, height: 1, noResizeObserver: true });
  try {
    const state = await loadDocx(ctx, { file: 'basic.docx', lateViewport: true });
    assert.equal(state.bridge.errors.length, 0);
    assert.ok(state.firstRect.width > 100 && state.firstRect.height > 100);
    assert.equal(state.visible, 'visible');
    record('docx-late-layout', 'pass', { firstRect: state.firstRect, logs: ctx.logs });
  } finally { await ctx.page.close(); }
});

test('DOCX print mode reaches onPrintReady', { concurrency: false }, async () => {
  const ctx = await newPage({ width: 1122, height: 794 });
  try {
    const state = await loadDocx(ctx, { file: 'basic.docx', print: true });
    assert.equal(state.bridge.errors.length, 0);
    assert.equal(state.bridge.printReady, 1);
    record('docx-print', 'pass', { pages: state.pages, logs: ctx.logs });
  } finally { await ctx.page.close(); }
});

test('truncated DOCX produces an explicit renderer error', { concurrency: false }, async () => {
  const ctx = await newPage();
  try {
    const state = await loadDocx(ctx, { file: 'truncated.docx' });
    assert.ok(state.bridge.errors.length > 0);
    assert.equal(state.pages, 0);
    record('docx-truncated', 'pass', { error: state.bridge.errors[0], logs: ctx.logs });
  } finally { await ctx.page.close(); }
});

for (const [name, file] of [
  ['docx-docm', 'basic.docm'],
  ['docx-dotx', 'basic.dotx'],
  ['docx-poi-docm', 'poi/poi-60158.docm'],
  ['docx-poi-dotx', 'poi/poi-60316.dotx']
]) {
  test(`${name} renders as a read-only preview`, { concurrency: false }, async () => {
    const ctx = await newPage();
    try {
      const state = await loadDocx(ctx, { file, corpus: file.startsWith('poi/') ? 'poi' : 'generated' });
      assert.equal(state.bridge.errors.length, 0);
      assert.ok(state.pages >= 1);
      record(name, 'pass', { pages: state.pages, logs: ctx.logs });
    } finally { await ctx.page.close(); }
  });
}

test('OOXML external relationship does not trigger an external request', { concurrency: false }, async () => {
  const ctx = await newPage();
  try {
    const state = await loadDocx(ctx, { file: 'external-rel.docx' });
    assert.equal(state.bridge.errors.length, 0);
    assert.equal(ctx.externalRequests.length, 0);
    record('docx-external-rel', 'pass', { externalRequests: ctx.externalRequests, logs: ctx.logs });
  } finally { await ctx.page.close(); }
});

test('PPTX renders, navigates, searches and zooms', { concurrency: false }, async () => {
  const ctx = await newPage();
  try {
    const state = await loadPptx(ctx, { file: 'basic.pptx' });
    assert.equal(state.bridge.errors.length, 0);
    assert.equal(state.slideWrappers, 1);
    const interaction = await ctx.page.evaluate(async () => {
      window.nextSlide();
      window.goToSlide(2);
      const count = window.searchWord('ALPHA');
      window.setZoom(2);
      // Highlighting waits for the slide-render animation frame.
      await new Promise(resolve => setTimeout(resolve, 250));
      return {
        count,
        slides: window.__bridge.slides,
        searches: window.__bridge.searches,
        zoom: document.getElementById('stage').style.zoom,
        marks: document.querySelectorAll('mark.pptx-highlight').length
      };
    });
    assert.ok(interaction.count >= 1);
    assert.equal(interaction.zoom, '2');
    assert.ok(interaction.marks >= 1);
    record('pptx-basic', 'pass', { state, interaction, logs: ctx.logs });
  } finally { await ctx.page.close(); }
});

test('rich PPTX print mode renders every slide with images and tables', { concurrency: false }, async () => {
  const ctx = await newPage({ width: 1122, height: 794 });
  try {
    const state = await loadPptx(ctx, { file: 'rich.pptx', print: true });
    assert.equal(state.bridge.errors.length, 0);
    assert.equal(state.bridge.printReady, 1);
    assert.equal(state.slideWrappers, 8);
    assert.ok(state.images >= 1);
    assert.ok(state.tables >= 1);
    record('pptx-rich-print', 'pass', { state, logs: ctx.logs });
  } finally { await ctx.page.close(); }
});

for (const [name, file] of [
  ['pptx-pptm', 'basic.pptm'],
  ['pptx-potx', 'basic.potx'],
  ['pptx-poi', 'poi/poi-51187.pptx']
]) {
  test(`${name} renders`, { concurrency: false }, async () => {
    const ctx = await newPage();
    try {
      const state = await loadPptx(ctx, { file, corpus: file.startsWith('poi/') ? 'poi' : 'generated' });
      assert.equal(state.bridge.errors.length, 0);
      assert.ok(state.slideWrappers >= 1 || state.fallbackCards >= 1);
      record(name, 'pass', { state, logs: ctx.logs });
    } finally { await ctx.page.close(); }
  });
}

test('corrupt PPTX produces an explicit renderer error', { concurrency: false }, async () => {
  const ctx = await newPage();
  try {
    const state = await loadPptx(ctx, { file: 'corrupt.pptx' });
    assert.ok(state.bridge.errors.length > 0);
    record('pptx-corrupt', 'pass', { error: state.bridge.errors[0], logs: ctx.logs });
  } finally { await ctx.page.close(); }
});

test('spreadsheet grid is virtualized, searchable, editable and zoomable', { concurrency: false }, async () => {
  const ctx = await newPage();
  try {
    const state = await loadGrid(ctx, 'basic.json');
    assert.equal(state.bridge.errors.length, 0);
    assert.equal(state.statusHidden, true);
    assert.ok(state.visibleCells > 0 && state.visibleCells < 300);
    assert.equal(state.injected, 0);
    const interaction = await ctx.page.evaluate(() => {
      const count = window.searchWord('ALPHA');
      window.nextSearchMatch();
      window.setZoom(2);
      window.setDarkMode(true);
      window.goToCell('B2');
      window.copySelection();
      return {
        count,
        searches: window.__bridge.searches,
        zoom: document.getElementById('surface').style.zoom,
        dark: document.body.classList.contains('dark'),
        clipboard: window.__bridge.clipboard
      };
    });
    assert.ok(interaction.count >= 1);
    assert.equal(interaction.zoom, '2');
    assert.equal(interaction.dark, true);
    assert.ok(interaction.clipboard.length >= 1);
    // Return to the top-left cell and commit one bounded cell edit.
    await ctx.page.evaluate(() => { window.setZoom(1); });
    // Let setZoom's animation-frame scroll restoration complete before
    // returning to B1 for the edit interaction.
    await new Promise(r => setTimeout(r, 200));
    await ctx.page.evaluate(() => {
      const viewport = document.getElementById('viewport');
      viewport.scrollLeft = 0; viewport.scrollTop = 0;
      viewport.dispatchEvent(new Event('scroll'));
    });
    await new Promise(r => setTimeout(r, 120));
    const cell = await ctx.page.$('.cell[data-row="0"][data-col="1"]');
    assert.ok(cell, 'cell B1 should be visible');
    await cell.click();
    await new Promise(r => setTimeout(r, 320));
    await ctx.page.evaluate(() => {
      const input = document.getElementById('cell-editor');
      input.focus(); input.select();
    });
    await ctx.page.keyboard.type('Edited value');
    await ctx.page.keyboard.press('Enter');
    const edits = await ctx.page.evaluate(() => window.__bridge.edits);
    assert.deepEqual(edits.at(-1), [0, 1, 'Edited value']);
    record('grid-basic', 'pass', { state, interaction, edits, logs: ctx.logs });
  } finally { await ctx.page.close(); }
});

test('large spreadsheet payload stays virtualized', { concurrency: false }, async () => {
  const ctx = await newPage();
  try {
    const state = await loadGrid(ctx, 'large.json');
    assert.equal(state.bridge.errors.length, 0);
    assert.equal(state.bridge.gridReady[0][0], 5001);
    assert.ok(state.visibleCells < 300);
    record('grid-large', 'pass', { visibleCells: state.visibleCells, ready: state.bridge.gridReady[0], logs: ctx.logs });
  } finally { await ctx.page.close(); }
});

test('untrusted spreadsheet text is never injected as HTML', { concurrency: false }, async () => {
  const ctx = await newPage();
  try {
    const state = await loadGrid(ctx, 'adversarial.json');
    assert.equal(state.injected, 0);
    assert.match(state.notice, /Macro-free test payload/);
    const bodyText = await ctx.page.evaluate(() => document.body.innerText);
    assert.match(bodyText, /<img src=x onerror=alert\(1\)>/);
    assert.equal(ctx.logs.filter(l => l.type === 'pageerror').length, 0);
    record('grid-adversarial', 'pass', { visibleCells: state.visibleCells, logs: ctx.logs });
  } finally { await ctx.page.close(); }
});
