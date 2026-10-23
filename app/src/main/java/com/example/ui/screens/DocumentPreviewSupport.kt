package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

import com.example.core.text.FencedCodeParser
import com.example.core.text.FencedCodeSegment

/** Shared, local-only controls injected into the generated TXT and Markdown preview documents. */
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

/** Makes a safe HTML rendering of a TXT file while keeping fenced code copyable as separate blocks. */
internal fun buildPlainTextPreviewHtml(
    content: String,
    isDark: Boolean,
    fontSizePx: Int,
    themeBgHex: String? = null,
    themeFgHex: String? = null
): String {
    val background = themeBgHex ?: if (isDark) "#151514" else "#faf9f5"
    val foreground = themeFgHex ?: if (isDark) "#d1cfc7" else "#3d3d3a"
    val codeBackground = if (isDark) "#1f1e1b" else "#efe9de"
    val codeHeader = if (isDark) "#252320" else "#e8e0d2"
    val border = if (isDark) "#383530" else "#e6dfd8"
    val buttonColor = if (isDark) "#c7c2b8" else "#6c6a64"

    val body = FencedCodeParser.split(content).joinToString(separator = "") { segment ->
        when (segment) {
            is FencedCodeSegment.Plain -> "<pre class=\"plain-text\">${escapeHtml(segment.text)}</pre>"
            is FencedCodeSegment.Code -> {
                val safeLanguage = segment.language
                    .takeIf { it.isNotBlank() }
                    ?.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
                    ?.take(32)
                    .orEmpty()
                val languageClass = if (safeLanguage.isNotEmpty()) " language-$safeLanguage" else ""
                "<pre><code class=\"$languageClass\">${escapeHtml(segment.text)}</code></pre>"
            }
        }
    }

    return """
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1, minimum-scale=0.25, maximum-scale=5, user-scalable=yes">
          <style>
            * { box-sizing: border-box; }
            html, body { min-height: 100%; margin: 0; background: $background; color: $foreground; }
            body { padding: 16px; font: ${fontSizePx}px/1.6 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; overflow-wrap: anywhere; }
            #document-content { width: 100%; }
            pre { margin: 0; padding: 0; white-space: pre-wrap; overflow-wrap: anywhere; }
            .plain-text { margin-bottom: 12px; }
            .code-block-container { margin: 0 0 16px; border: 1px solid $border; border-radius: 12px; overflow: hidden; background: $codeBackground; }
            .code-header { min-height: 40px; padding: 6px 12px; display: flex; align-items: center; justify-content: space-between; gap: 12px; background: $codeHeader; }
            .code-lang { color: $buttonColor; font-size: 11px; font-weight: 700; letter-spacing: .06em; text-transform: uppercase; }
            .copy-btn { padding: 6px 10px; border: 1px solid $border; border-radius: 8px; background: transparent; color: $buttonColor; font: 600 12px sans-serif; }
            .code-content { overflow-x: auto; padding: 14px; background: $codeBackground; }
            .code-content pre { white-space: pre; overflow-wrap: normal; }
            .search-highlight { background: rgba(204, 120, 92, 0.32); color: inherit; border-radius: 2px; }
            .search-highlight-active { background: #cc785c; color: #ffffff; outline: 2px solid rgba(232, 165, 90, 0.9); }
            .line-anchor-flash { background: rgba(204, 120, 92, 0.32); color: inherit; border-radius: 2px; outline: 2px solid #cc785c; transition: background 0.4s ease; }
            @media print { .code-header, .copy-btn { display: none !important; } .code-block-container { break-inside: avoid; } }
          </style>
          $DocumentPreviewScript
        </head>
        <body><main id="document-content">$body</main></body>
        </html>
    """.trimIndent()
}

/** Escapes plain text before embedding it in generated HTML. */
internal fun escapeHtml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

/** Opens a PDF with an installed external viewer using a scoped content URI. */
internal fun openPdfInBrowser(context: Context, file: File) {
    if (!file.isFile || !file.canRead()) {
        android.widget.Toast.makeText(context, "PDF file is unavailable or cannot be read", android.widget.Toast.LENGTH_LONG).show()
        return
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Open PDF with"))
    } catch (_: android.content.ActivityNotFoundException) {
        android.widget.Toast.makeText(context, "No PDF viewer is installed", android.widget.Toast.LENGTH_LONG).show()
    }
}

/**
 * Pure command-issuing policy for the WebView search pipelines (DOCX / TXT / MD).
 *
 * Loop 12 root cause for the reported "7 results but the arrows only reach 2-3":
 * both WebView search effects combined the query step and the navigation step into
 * ONE script. Every arrow press re-ran `searchWord()`, which rebuilds the highlight
 * list and resets the renderer's active index to 0, and the appended
 * `nextSearchMatch()` then only advanced one step from that freshly-reset index.
 * Deterministic outcome: the counter could never move past 2 on "next" no matter
 * how many matches existed (and "previous" always wrapped to the last match and
 * back). The list itself was never truncated — the commands were just wrong.
 *
 * The policy owns the *ordering* rules so the Compose effects stay thin:
 *  1. A query that has not been issued to the renderer yet is always searched first.
 *  2. Navigation triggers become next/prev DELTAS, never another `searchWord()`.
 *  3. A trigger counter that moved backwards (screen reset on file change) is
 *     absorbed silently instead of being re-issued as steps.
 */
object InViewerSearchCommandPolicy {

    /** Ledger of what the renderer has already been told to do. */
    data class Issued(
        val query: String? = null,
        val next: Int = 0,
        val prev: Int = 0
    )

    data class Plan(
        val search: Boolean,
        val nextSteps: Int,
        val prevSteps: Int,
        val issued: Issued
    ) {
        val isEmpty: Boolean get() = !search && nextSteps == 0 && prevSteps == 0
    }

    /**
     * Plans the next batch of renderer commands.
     *
     * @param issued last state actually delivered to the renderer.
     * @param query current find-bar query (raw, not normalized).
     * @param nextTarget monotonic "next" trigger counter owned by the screen.
     * @param prevTarget monotonic "previous" trigger counter owned by the screen.
     */
    fun plan(issued: Issued, query: String, nextTarget: Int, prevTarget: Int): Plan {
        val search = issued.query != query
        // Forward deltas only; a backwards move means the counters were reset
        // (file switched / find mode closed) and must be absorbed, not replayed.
        val nextSteps = (nextTarget - issued.next).coerceAtLeast(0)
        val prevSteps = (prevTarget - issued.prev).coerceAtLeast(0)
        return Plan(
            search = search,
            nextSteps = nextSteps,
            prevSteps = prevSteps,
            issued = Issued(query = query, next = nextTarget, prev = prevTarget)
        )
    }
}

/**
 * Pure match-navigation math shared by the viewer header find bar (Loop 10). Kept
 * side-effect free so the header arrows and the IME Search action provably walk the
 * same indices, and so the wrap-around behaviour is unit-testable without a device.
 */
object InViewerSearchNavigation {

    /**
     * Wraps forward through [count] matches. Returns null when there is nothing to
     * navigate (no matches), otherwise the next index clamped into [0, count).
     */
    fun nextIndex(current: Int, count: Int): Int? {
        if (count <= 0) return null
        val safeCurrent = if (current in 0 until count) current else 0
        return (safeCurrent + 1) % count
    }

    /**
     * Wraps backward through [count] matches. Returns null when there is nothing to
     * navigate (no matches), otherwise the previous index clamped into [0, count).
     */
    fun prevIndex(current: Int, count: Int): Int? {
        if (count <= 0) return null
        val safeCurrent = if (current in 0 until count) current else 0
        return (safeCurrent - 1 + count) % count
    }
}



/**
 * Loop 14: decision policy for the DOCX text-recovery state. A plain-text view of
 * a DOCX is ONLY ever a labeled recovery/error surface after the formatted
 * renderer (docx-preview in the WebView) genuinely failed — it must never
 * masquerade as a normal preview. Keeping the decision pure makes the routing
 * testable without a device or a WebView.
 */
object DocxTextRecovery {

    /** Max characters of a renderer failure reason shown to the user (no document content, ever). */
    const val MAX_REASON_CHARS = 180

    /**
     * True when the recovered text is worth showing under the recovery banner.
     * A DOCX whose extraction yields zero paragraphs has nothing to recover:
     * the state stays an honest "nothing could be recovered" error surface.
     */
    fun shouldShowRecoveredText(paragraphCount: Int): Boolean = paragraphCount > 0

    /** Banner headline — must read as a failure, not as a feature. */
    fun bannerHeadline(): String = "Formatted preview failed — showing recovered text"

    /** Failure reason clamped for one-line display; null-safe and content-free. */
    fun displayReason(rawReason: String?): String? {
        val trimmed = rawReason?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return trimmed.take(MAX_REASON_CHARS)
    }

    /** Recovery stats line; empty when there is nothing recovered to count. */
    fun statsLabel(wordCount: Int, paragraphCount: Int): String? =
        if (paragraphCount > 0) "$wordCount words • $paragraphCount paragraphs recovered" else null
}
