package com.example.formats.common.ui

internal val DocumentPreviewScript = """
    <script>
      var searchMatches = [];
      var currentMatchIdx = -1;
      var totalSearchMatches = 0;
      var searchMatchesTruncated = false;
      var maxSearchHighlights = 1000;

      function searchCountChanged() {
        if (window.Android && window.Android.onSearchResultCount) {
          window.Android.onSearchResultCount(totalSearchMatches, currentMatchIdx, searchMatchesTruncated);
        }
      }

      function clearSearch() {
        var root = document.getElementById('document-content') || document.body;
        root.querySelectorAll('mark.search-highlight').forEach(function (mark) {
          var parent = mark.parentNode;
          if (parent) {
            parent.replaceChild(document.createTextNode(mark.textContent || ''), mark);
            parent.normalize();
          }
        });
        searchMatches = [];
        currentMatchIdx = -1;
        totalSearchMatches = 0;
        searchMatchesTruncated = false;
      }

      function findIgnoreCase(lowerText, query, fromIndex) {
        return lowerText.indexOf(query, Math.max(0, fromIndex));
      }

      function highlightSearchMatch(index) {
        if (!searchMatches.length) {
          currentMatchIdx = -1;
          searchCountChanged();
          return -1;
        }
        currentMatchIdx = (index + searchMatches.length) % searchMatches.length;
        searchMatches.forEach(function (mark, i) {
          mark.className = i === currentMatchIdx
            ? 'search-highlight search-highlight-active'
            : 'search-highlight';
        });
        searchMatches[currentMatchIdx].scrollIntoView({ behavior: 'smooth', block: 'center' });
        searchCountChanged();
        return currentMatchIdx + 1;
      }

      function searchWord(rawQuery) {
        clearSearch();
        var query = String(rawQuery || '').trim().toLowerCase();
        var root = document.getElementById('document-content') || document.body;
        if (!query) {
          searchCountChanged();
          return 0;
        }

        var walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
          acceptNode: function (node) {
            var parent = node.parentElement;
            if (!parent || !node.nodeValue) return NodeFilter.FILTER_REJECT;
            if (parent.closest('script,style,button,.code-header,mark.search-highlight')) return NodeFilter.FILTER_REJECT;
            return NodeFilter.FILTER_ACCEPT;
          }
        });
        var nodes = [];
        while (walker.nextNode()) nodes.push(walker.currentNode);

        nodes.forEach(function (node) {
          var text = node.nodeValue || '';
          var lowerText = text.toLowerCase();
          var cursor = 0;
          var first = findIgnoreCase(lowerText, query, cursor);
          if (first < 0) return;
          var fragment = document.createDocumentFragment();
          while (first >= 0) {
            totalSearchMatches++;
            fragment.appendChild(document.createTextNode(text.slice(cursor, first)));
            if (searchMatches.length < maxSearchHighlights) {
              var mark = document.createElement('mark');
              mark.className = 'search-highlight';
              mark.textContent = text.slice(first, first + query.length);
              fragment.appendChild(mark);
              searchMatches.push(mark);
            }
            cursor = first + query.length;
            first = findIgnoreCase(lowerText, query, cursor);
          }
          fragment.appendChild(document.createTextNode(text.slice(cursor)));
          if (node.parentNode) node.parentNode.replaceChild(fragment, node);
        });

        searchMatchesTruncated = totalSearchMatches > searchMatches.length;
        if (searchMatches.length) highlightSearchMatch(0);
        else searchCountChanged();
        return totalSearchMatches;
      }

      function nextSearchMatch() {
        return searchMatches.length ? highlightSearchMatch(currentMatchIdx + 1) : 0;
      }

      function prevSearchMatch() {
        return searchMatches.length ? highlightSearchMatch(currentMatchIdx - 1) : 0;
      }

      // ---- Outline & Go-to-Line navigation -----------------------------------------
      var lineFlashTimer = null;

      function flashElement(element) {
        if (!element) return;
        try {
          element.classList.remove('line-anchor-flash');
          // Force a reflow so re-triggering restarts the CSS animation.
          void element.offsetWidth;
          element.classList.add('line-anchor-flash');
          if (lineFlashTimer) window.clearTimeout(lineFlashTimer);
          lineFlashTimer = window.setTimeout(function () {
            element.classList.remove('line-anchor-flash');
          }, 1600);
        } catch (e) { /* non-fatal */ }
      }

      // Scrolls to the Nth (0-based) heading element of the rendered document.
      function scrollToHeadingIndex(index) {
        var root = document.getElementById('document-content') || document.body;
        var headings = root.querySelectorAll('h1, h2, h3, h4, h5, h6');
        var target = headings.length ? headings[Math.max(0, parseInt(index, 10) || 0)] : null;
        if (!target) return false;
        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
        flashElement(target);
        return true;
      }

      // Scrolls to a source line by walking text nodes and counting newlines.
      // Exact for plain text (pre blocks keep real newlines); approximate for
      // rendered markdown whose block layout collapses blank lines.
      function scrollToLine(targetLine) {
        var root = document.getElementById('document-content') || document.body;
        var line = Math.max(1, parseInt(targetLine, 10) || 1);
        var walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
          acceptNode: function (node) {
            var parent = node.parentElement;
            if (!parent || !node.nodeValue) return NodeFilter.FILTER_REJECT;
            if (parent.closest && parent.closest('script,style,button,.code-header')) return NodeFilter.FILTER_REJECT;
            return NodeFilter.FILTER_ACCEPT;
          }
        });
        var currentLine = 1;
        var node;
        while ((node = walker.nextNode())) {
          if (currentLine >= line) {
            var range = document.createRange();
            try {
              range.setStart(node, 0);
              range.collapse(true);
              var span = document.createElement('span');
              span.className = 'line-anchor-flash';
              range.insertNode(span);
              span.scrollIntoView({ behavior: 'smooth', block: 'center' });
              window.setTimeout(function () {
                if (span.parentNode) {
                  span.parentNode.replaceChild(document.createTextNode(span.textContent || ''), span);
                  span.parentNode.normalize();
                }
              }, 1600);
            } catch (e) {
              if (node.parentElement) {
                node.parentElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
                flashElement(node.parentElement);
              }
            }
            return true;
          }
          var text = node.nodeValue || '';
          var remaining = line - currentLine;
          var pos = -1;
          for (var i = 0; i < remaining; i++) {
            pos = text.indexOf('\n', pos + 1);
            if (pos < 0) break;
          }
          if (pos >= 0) {
            // The requested line starts inside this node; retry loop targeting it directly.
            var range2 = document.createRange();
            try {
              range2.setStart(node, Math.min(pos + 1, text.length));
              range2.collapse(true);
              var span2 = document.createElement('span');
              span2.className = 'line-anchor-flash';
              range2.insertNode(span2);
              span2.scrollIntoView({ behavior: 'smooth', block: 'center' });
              window.setTimeout(function () {
                if (span2.parentNode) {
                  span2.parentNode.replaceChild(document.createTextNode(span2.textContent || ''), span2);
                  span2.parentNode.normalize();
                }
              }, 1600);
            } catch (e) {
              if (node.parentElement) {
                node.parentElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
                flashElement(node.parentElement);
              }
            }
            return true;
          }
          var matches = text.match(/\n/g);
          currentLine += matches ? matches.length : 0;
        }
        return false;
      }

      // ---- Browser-style viewer zoom (25% - 500%) ----------------------------------
      function setViewerZoom(percent) {
        var p = parseInt(percent, 10);
        if (isNaN(p)) p = 100;
        p = Math.max(25, Math.min(500, p));
        document.body.style.zoom = (p / 100);
        return p;
      }

      function copyCode(button, codeElement) {
        var codeText = codeElement ? (codeElement.textContent || '') : '';
        if (window.Android && window.Android.copyToClipboard) {
          window.Android.copyToClipboard(codeText);
          var oldLabel = button.textContent;
          button.textContent = 'Copied!';
          window.setTimeout(function () { button.textContent = oldLabel || 'Copy'; }, 1600);
        }
      }

      function decorateCodeBlocks() {
        var root = document.getElementById('document-content') || document.body;
        root.querySelectorAll('pre').forEach(function (pre) {
          if (pre.classList.contains('plain-text')) return;
          if (pre.parentElement && pre.parentElement.classList.contains('code-content')) return;
          var code = pre.querySelector('code') || pre;
          var language = 'Code';
          if (code.className) {
            var languageMatch = code.className.match(/language-([a-zA-Z0-9_-]+)/);
            if (languageMatch) language = languageMatch[1];
          }

          var container = document.createElement('div');
          container.className = 'code-block-container';
          var header = document.createElement('div');
          header.className = 'code-header';
          var languageLabel = document.createElement('span');
          languageLabel.className = 'code-lang';
          languageLabel.textContent = language;
          var copyButton = document.createElement('button');
          copyButton.className = 'copy-btn';
          copyButton.type = 'button';
          copyButton.textContent = 'Copy code';
          copyButton.onclick = function () { copyCode(copyButton, code); };
          header.appendChild(languageLabel);
          header.appendChild(copyButton);

          var content = document.createElement('div');
          content.className = 'code-content';
          var parent = pre.parentNode;
          if (!parent) return;
          parent.insertBefore(container, pre);
          container.appendChild(header);
          container.appendChild(content);
          content.appendChild(pre);
        });
      }

      document.addEventListener('DOMContentLoaded', function () {
        decorateCodeBlocks();
        var lastTapTime = 0;
        var singleTapTimer = null;
        document.addEventListener('click', function (event) {
          var target = event.target && event.target.closest ? event.target.closest('button, a, input, textarea, select') : null;
          if (target || !window.Android) return;
          var now = Date.now();
          if (now - lastTapTime < 280) {
            lastTapTime = 0;
            if (singleTapTimer) { clearTimeout(singleTapTimer); singleTapTimer = null; }
            if (window.Android.onDocumentDoubleTap) window.Android.onDocumentDoubleTap();
          } else {
            lastTapTime = now;
            if (singleTapTimer) clearTimeout(singleTapTimer);
            singleTapTimer = setTimeout(function () {
              singleTapTimer = null;
              if (window.Android.onDocumentTap) window.Android.onDocumentTap();
            }, 250);
          }
        });
      });
    </script>
""".trimIndent()
