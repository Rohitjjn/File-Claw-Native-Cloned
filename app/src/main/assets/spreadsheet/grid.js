/* File Claw bounded spreadsheet grid. No third-party scripts or network access. */
(function () {
  'use strict';

  var ROW_HEIGHT = 36;
  var COL_WIDTH = 140;
  var HEADER_HEIGHT = 32;
  var MIN_ZOOM = 0.5;
  var MAX_ZOOM = 3.0;
  var MAX_MATCHES = 5000;
  var SEARCH_SCAN_LIMIT = 400000;

  var dom = {};
  var state = {
    rows: [],
    editable: false,
    sheetName: 'Sheet1',
    rowCount: 1,
    colCount: 1,
    zoom: 1,
    ready: false,
    selected: null,
    matches: [],
    matchKeys: new Set(),
    currentMatch: -1,
    searchTruncated: false,
    editor: null,
    lastReportedZoom: 1
  };

  function byId(id) { return document.getElementById(id); }
  function clamp(value, min, max) { return Math.max(min, Math.min(max, value)); }
  function bridge(name) {
    return window.Android && typeof window.Android[name] === 'function'
      ? window.Android[name] : null;
  }
  function rowHeaderBaseWidth() {
    return window.matchMedia && window.matchMedia('(max-width: 480px)').matches ? 46 : 58;
  }
  function columnLabel(index) {
    var n = index + 1;
    var label = '';
    while (n > 0) {
      n--;
      label = String.fromCharCode(65 + (n % 26)) + label;
      n = Math.floor(n / 26);
    }
    return label;
  }
  function cellLabel(row, col) { return columnLabel(col) + String(row + 1); }
  function getCellValue(row, col) {
    var values = state.rows[row];
    return values && values[col] != null ? String(values[col]) : '';
  }
  function setCellValue(row, col, text) {
    while (state.rows.length <= row) state.rows.push([]);
    while (state.rows[row].length <= col) state.rows[row].push('');
    state.rows[row][col] = text;
    state.rowCount = Math.max(state.rowCount, row + 1);
    state.colCount = Math.max(state.colCount, col + 1);
  }

  function showStatus(message, isError) {
    dom.status.textContent = message || 'Loading spreadsheet…';
    dom.status.classList.toggle('error', Boolean(isError));
    dom.status.classList.remove('hidden');
  }
  function hideStatus() { dom.status.classList.add('hidden'); }
  function notifyError(message) {
    showStatus(message, true);
    var callback = bridge('onGridError');
    if (callback) callback(String(message || 'Could not load the spreadsheet.'));
  }

  function setDarkMode(isDark) {
    document.body.classList.toggle('dark', Boolean(isDark));
  }
  window.setDarkMode = setDarkMode;

  function applyZoomScale() {
    var zoom = state.zoom;
    document.documentElement.style.setProperty('--header-height', Math.round(HEADER_HEIGHT * zoom) + 'px');
    document.documentElement.style.setProperty('--row-header-width', Math.round(rowHeaderBaseWidth() * zoom) + 'px');
    dom.surface.style.zoom = String(zoom);
    dom.surface.style.width = (state.colCount * COL_WIDTH) + 'px';
    dom.surface.style.height = (state.rowCount * ROW_HEIGHT) + 'px';
    dom.columnTrack.style.width = (state.colCount * COL_WIDTH * zoom) + 'px';
    dom.columnTrack.style.height = (HEADER_HEIGHT * zoom) + 'px';
    dom.rowTrack.style.width = (rowHeaderBaseWidth() * zoom) + 'px';
    dom.rowTrack.style.height = (state.rowCount * ROW_HEIGHT * zoom) + 'px';
  }

  function setZoom(factor, notify) {
    var value = Number(factor);
    if (!isFinite(value)) value = 1;
    value = clamp(value, MIN_ZOOM, MAX_ZOOM);
    var oldZoom = state.zoom;
    if (Math.abs(oldZoom - value) < 0.001) return value;
    var centerX = (dom.viewport.scrollLeft + dom.viewport.clientWidth / 2) / oldZoom;
    var centerY = (dom.viewport.scrollTop + dom.viewport.clientHeight / 2) / oldZoom;
    state.zoom = value;
    applyZoomScale();
    requestAnimationFrame(function () {
      dom.viewport.scrollLeft = Math.max(0, centerX * value - dom.viewport.clientWidth / 2);
      dom.viewport.scrollTop = Math.max(0, centerY * value - dom.viewport.clientHeight / 2);
      renderVisible();
    });
    if (notify !== false) {
      state.lastReportedZoom = value;
      var callback = bridge('onZoomChanged');
      if (callback) callback(value);
    }
    return value;
  }
  window.setZoom = setZoom;

  function renderHeaders(startRow, endRow, startCol, endCol) {
    var zoom = state.zoom;
    var columnFragment = document.createDocumentFragment();
    for (var c = startCol; c <= endCol; c++) {
      var header = document.createElement('div');
      header.className = 'header-cell';
      header.style.left = (c * COL_WIDTH * zoom) + 'px';
      header.style.top = '0px';
      header.style.width = (COL_WIDTH * zoom) + 'px';
      header.style.height = (HEADER_HEIGHT * zoom) + 'px';
      header.style.fontSize = Math.max(9, Math.round(11 * zoom)) + 'px';
      header.textContent = columnLabel(c);
      columnFragment.appendChild(header);
    }
    dom.columnTrack.replaceChildren(columnFragment);
    dom.columnTrack.style.transform = 'translateX(' + (-dom.viewport.scrollLeft) + 'px)';

    var rowFragment = document.createDocumentFragment();
    for (var r = startRow; r <= endRow; r++) {
      var rowHeader = document.createElement('div');
      rowHeader.className = 'header-cell row-header';
      rowHeader.style.left = '0px';
      rowHeader.style.top = (r * ROW_HEIGHT * zoom) + 'px';
      rowHeader.style.width = (rowHeaderBaseWidth() * zoom) + 'px';
      rowHeader.style.height = (ROW_HEIGHT * zoom) + 'px';
      rowHeader.style.fontSize = Math.max(9, Math.round(11 * zoom)) + 'px';
      rowHeader.textContent = String(r + 1);
      rowFragment.appendChild(rowHeader);
    }
    dom.rowTrack.replaceChildren(rowFragment);
    dom.rowTrack.style.transform = 'translateY(' + (-dom.viewport.scrollTop) + 'px)';
  }

  function renderVisible() {
    if (!state.ready) return;
    var zoom = state.zoom;
    var scrollX = dom.viewport.scrollLeft / zoom;
    var scrollY = dom.viewport.scrollTop / zoom;
    var viewWidth = dom.viewport.clientWidth / zoom;
    var viewHeight = dom.viewport.clientHeight / zoom;
    var startCol = clamp(Math.floor(scrollX / COL_WIDTH) - 1, 0, state.colCount - 1);
    var endCol = clamp(Math.ceil((scrollX + viewWidth) / COL_WIDTH) + 1, 0, state.colCount - 1);
    var startRow = clamp(Math.floor(scrollY / ROW_HEIGHT) - 1, 0, state.rowCount - 1);
    var endRow = clamp(Math.ceil((scrollY + viewHeight) / ROW_HEIGHT) + 1, 0, state.rowCount - 1);
    var fragment = document.createDocumentFragment();
    var activeKey = state.currentMatch >= 0 && state.matches[state.currentMatch]
      ? state.matches[state.currentMatch].key : null;
    for (var r = startRow; r <= endRow; r++) {
      for (var c = startCol; c <= endCol; c++) {
        var key = r + ':' + c;
        var cell = document.createElement('div');
        cell.className = 'cell';
        if (r % 2 === 1) cell.classList.add('alt');
        if (state.selected && state.selected.row === r && state.selected.col === c) cell.classList.add('selected');
        if (state.matchKeys.has(key)) cell.classList.add('search-match');
        if (key === activeKey) cell.classList.add('search-active');
        cell.dataset.row = String(r);
        cell.dataset.col = String(c);
        cell.style.left = (c * COL_WIDTH) + 'px';
        cell.style.top = (r * ROW_HEIGHT) + 'px';
        cell.style.width = COL_WIDTH + 'px';
        cell.style.height = ROW_HEIGHT + 'px';
        cell.textContent = getCellValue(r, c);
        cell.title = cellLabel(r, c) + ': ' + cell.textContent;
        fragment.appendChild(cell);
      }
    }
    dom.cells.replaceChildren(fragment);
    renderHeaders(startRow, endRow, startCol, endCol);
  }

  function scrollToCell(row, col) {
    var zoom = state.zoom;
    var targetLeft = col * COL_WIDTH * zoom - dom.viewport.clientWidth / 2 + COL_WIDTH * zoom / 2;
    var targetTop = row * ROW_HEIGHT * zoom - dom.viewport.clientHeight / 2 + ROW_HEIGHT * zoom / 2;
    dom.viewport.scrollLeft = clamp(targetLeft, 0, Math.max(0, dom.surface.scrollWidth * zoom - dom.viewport.clientWidth));
    dom.viewport.scrollTop = clamp(targetTop, 0, Math.max(0, dom.surface.scrollHeight * zoom - dom.viewport.clientHeight));
    renderVisible();
  }

  function selectCell(row, col, notify) {
    state.selected = { row: row, col: col };
    renderVisible();
    if (notify !== false) {
      var callback = bridge('onSelectionChanged');
      if (callback) callback(cellLabel(row, col), getCellValue(row, col));
    }
  }

  function copySelection() {
    if (!state.selected) selectCell(0, 0, false);
    var text = getCellValue(state.selected.row, state.selected.col);
    var callback = bridge('copyToClipboard');
    if (callback) callback(text);
    return text;
  }
  window.copySelection = copySelection;

  function closeEditor(commit) {
    var editor = state.editor;
    if (!editor) return;
    state.editor = null;
    dom.input.style.display = 'none';
    if (commit) {
      var value = dom.input.value;
      setCellValue(editor.row, editor.col, value);
      var callback = bridge('onCellChanged');
      if (callback) callback(editor.row, editor.col, value);
      selectCell(editor.row, editor.col, false);
      renderVisible();
    }
  }

  function openEditor(row, col) {
    if (!state.editable) { selectCell(row, col); return; }
    closeEditor(false);
    selectCell(row, col, false);
    var zoom = state.zoom;
    var left = rowHeaderBaseWidth() * zoom + col * COL_WIDTH * zoom - dom.viewport.scrollLeft;
    var top = HEADER_HEIGHT * zoom + row * ROW_HEIGHT * zoom - dom.viewport.scrollTop;
    state.editor = { row: row, col: col };
    dom.input.value = getCellValue(row, col);
    dom.input.style.left = left + 'px';
    dom.input.style.top = top + 'px';
    dom.input.style.width = Math.max(40, COL_WIDTH * zoom) + 'px';
    dom.input.style.height = Math.max(28, ROW_HEIGHT * zoom) + 'px';
    dom.input.style.fontSize = Math.max(11, Math.round(13 * zoom)) + 'px';
    dom.input.style.display = 'block';
    dom.input.focus();
    dom.input.select();
  }

  function notifySearchCount() {
    var callback = bridge('onSearchResultCount');
    if (callback) callback(state.matches.length, state.currentMatch, state.searchTruncated);
  }
  function highlightMatch(index) {
    if (!state.matches.length) {
      state.currentMatch = -1;
      notifySearchCount();
      return 0;
    }
    state.currentMatch = ((index % state.matches.length) + state.matches.length) % state.matches.length;
    var match = state.matches[state.currentMatch];
    selectCell(match.row, match.col, false);
    scrollToCell(match.row, match.col);
    notifySearchCount();
    return state.currentMatch + 1;
  }
  function searchWord(keyword) {
    state.matches = [];
    state.matchKeys = new Set();
    state.currentMatch = -1;
    state.searchTruncated = false;
    var query = String(keyword == null ? '' : keyword).trim().toLocaleLowerCase();
    if (!query) { notifySearchCount(); renderVisible(); return 0; }
    var scanned = 0;
    for (var r = 0; r < state.rows.length; r++) {
      var values = state.rows[r] || [];
      for (var c = 0; c < values.length; c++) {
        if (scanned++ >= SEARCH_SCAN_LIMIT) { state.searchTruncated = true; break; }
        var text = values[c] == null ? '' : String(values[c]);
        if (text.toLocaleLowerCase().indexOf(query) < 0) continue;
        if (state.matches.length < MAX_MATCHES) {
          var key = r + ':' + c;
          state.matches.push({ row: r, col: c, key: key });
          state.matchKeys.add(key);
        } else {
          state.searchTruncated = true;
        }
      }
      if (scanned >= SEARCH_SCAN_LIMIT) break;
    }
    renderVisible();
    if (state.matches.length) highlightMatch(0);
    else notifySearchCount();
    return state.matches.length;
  }
  function nextSearchMatch() { return highlightMatch(state.currentMatch + 1); }
  function prevSearchMatch() { return highlightMatch(state.currentMatch - 1); }
  window.searchWord = searchWord;
  window.nextSearchMatch = nextSearchMatch;
  window.prevSearchMatch = prevSearchMatch;

  function goToCell(address) {
    var match = /^([A-Za-z]{1,3})([1-9][0-9]*)$/.exec(String(address || '').trim().toUpperCase());
    if (!match) return false;
    var col = 0;
    for (var i = 0; i < match[1].length; i++) col = col * 26 + (match[1].charCodeAt(i) - 64);
    col -= 1;
    var row = parseInt(match[2], 10) - 1;
    if (row < 0 || row >= state.rowCount || col < 0 || col >= state.colCount) return false;
    selectCell(row, col);
    scrollToCell(row, col);
    return true;
  }
  window.goToCell = goToCell;

  function initialize(data) {
    state.rows = Array.isArray(data.rows) ? data.rows : [];
    state.editable = Boolean(data.editable);
    state.sheetName = String(data.sheetName || 'Sheet1');
    var maxCols = 1;
    for (var r = 0; r < state.rows.length; r++) {
      maxCols = Math.max(maxCols, Array.isArray(state.rows[r]) ? state.rows[r].length : 0);
    }
    state.rowCount = clamp(state.rows.length + (state.editable ? 20 : 0), 1, 20000);
    state.colCount = clamp(maxCols + (state.editable ? 4 : 0), 1, 100);
    state.ready = true;
    dom.notice.textContent = String(data.notice || '');
    dom.notice.classList.toggle('visible', Boolean(data.notice));
    applyZoomScale();
    hideStatus();
    renderVisible();
    var ready = bridge('onGridReady');
    if (ready) ready(state.rows.length, state.colCount);
  }

  function boot() {
    dom.viewport = byId('viewport');
    dom.surface = byId('surface');
    dom.cells = byId('cells');
    dom.columnTrack = byId('column-track');
    dom.rowTrack = byId('row-track');
    dom.input = byId('cell-editor');
    dom.status = byId('status');
    dom.notice = byId('notice');

    dom.viewport.addEventListener('scroll', renderVisible, { passive: true });
    window.addEventListener('resize', function () { applyZoomScale(); renderVisible(); });
    dom.viewport.addEventListener('keydown', function (event) {
      if (!state.selected) return;
      var row = state.selected.row, col = state.selected.col;
      if (event.key === 'ArrowDown') row++;
      else if (event.key === 'ArrowUp') row--;
      else if (event.key === 'ArrowRight') col++;
      else if (event.key === 'ArrowLeft') col--;
      else if (event.key === 'Enter' && state.editable) { event.preventDefault(); openEditor(row, col); return; }
      else return;
      event.preventDefault();
      row = clamp(row, 0, state.rowCount - 1);
      col = clamp(col, 0, state.colCount - 1);
      selectCell(row, col);
      scrollToCell(row, col);
    });
    dom.viewport.addEventListener('contextmenu', function (event) {
      event.preventDefault();
      copySelection();
    });

    var lastTapAt = 0, lastX = 0, lastY = 0, singleTapTimer = null;
    dom.viewport.addEventListener('pointerup', function (event) {
      if (event.target === dom.input || !state.ready) return;
      var cell = event.target && event.target.closest ? event.target.closest('.cell') : null;
      var now = Date.now();
      var distance = Math.hypot(event.clientX - lastX, event.clientY - lastY);
      if (now - lastTapAt < 280 && distance < 40) {
        lastTapAt = 0;
        if (singleTapTimer) { clearTimeout(singleTapTimer); singleTapTimer = null; }
        setZoom(state.zoom > 1.4 ? 1 : 2);
        return;
      }
      lastTapAt = now; lastX = event.clientX; lastY = event.clientY;
      if (singleTapTimer) clearTimeout(singleTapTimer);
      if (!cell) {
        var tap = bridge('onGridTap');
        if (tap) tap();
        return;
      }
      var row = parseInt(cell.dataset.row, 10);
      var col = parseInt(cell.dataset.col, 10);
      singleTapTimer = setTimeout(function () {
        singleTapTimer = null;
        if (state.editable) openEditor(row, col);
        else selectCell(row, col);
      }, 250);
    });
    dom.viewport.addEventListener('pointercancel', function () {
      if (singleTapTimer) clearTimeout(singleTapTimer);
      singleTapTimer = null;
    });

    var pinchStart = 0, pinchZoom = 1;
    dom.viewport.addEventListener('touchstart', function (event) {
      if (event.touches.length !== 2) return;
      pinchStart = Math.hypot(
        event.touches[0].clientX - event.touches[1].clientX,
        event.touches[0].clientY - event.touches[1].clientY
      );
      pinchZoom = state.zoom;
    }, { passive: true });
    dom.viewport.addEventListener('touchmove', function (event) {
      if (event.touches.length !== 2 || pinchStart < 8) return;
      event.preventDefault();
      var distance = Math.hypot(
        event.touches[0].clientX - event.touches[1].clientX,
        event.touches[0].clientY - event.touches[1].clientY
      );
      setZoom(pinchZoom * distance / pinchStart, false);
    }, { passive: false });
    dom.viewport.addEventListener('touchend', function (event) {
      if (event.touches.length < 2 && pinchStart > 0) {
        pinchStart = 0;
        state.lastReportedZoom = state.zoom;
        var callback = bridge('onZoomChanged');
        if (callback) callback(state.zoom);
      }
    }, { passive: true });

    dom.input.addEventListener('blur', function () { closeEditor(true); });
    dom.input.addEventListener('keydown', function (event) {
      if (event.key === 'Enter') { event.preventDefault(); closeEditor(true); }
      else if (event.key === 'Escape') { event.preventDefault(); closeEditor(false); }
    });

    showStatus('Loading spreadsheet…', false);
    fetch('/spreadsheet-src/data.json', { credentials: 'omit', cache: 'no-store' })
      .then(function (response) {
        if (!response.ok) throw new Error('HTTP ' + response.status);
        return response.json();
      })
      .then(initialize)
      .catch(function (error) {
        notifyError('The spreadsheet viewer could not load. ' + (error && error.message ? error.message : 'Unknown error'));
      });
  }
  window.retrySpreadsheet = function () { window.location.reload(); };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
}());
