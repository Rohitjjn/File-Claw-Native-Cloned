package com.example.utils

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Native Print and Export helper using Android's PrintManager & PrintDocumentAdapter.
 * Works 100% offline to print or save documents as PDF.
 * Supports PDF, DOCX, TXT, and Markdown files.
 */
object FilePrintHelper {

    // Keep active webview references to prevent GC before print job is received
    private val activeWebViews = mutableSetOf<WebView>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    fun printPdf(
        context: Context,
        pdfFile: File,
        jobName: String = "FileClaw_Document"
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            android.widget.Toast.makeText(context, "Print service is not available on this device", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            android.widget.Toast.makeText(context, "Cannot print: PDF file is empty or missing", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        mainHandler.post {
            try {
                val printAdapter = object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder(jobName)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        if (destination == null) {
                            callback?.onWriteFailed("Print destination is null")
                            return
                        }
                        try {
                            FileInputStream(pdfFile).use { input ->
                                FileOutputStream(destination.fileDescriptor).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message ?: "Print write error")
                        }
                    }
                }

                printManager.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder()
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .build()
                )
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Print failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun printWebView(
        context: Context,
        webView: WebView,
        jobName: String = "FileClaw_Document"
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        mainHandler.post {
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            printManager.print(
                jobName,
                printAdapter,
                PrintAttributes.Builder()
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .build()
            )
        }
    }

    fun printDocx(
        context: Context,
        docxFile: File,
        title: String
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            android.widget.Toast.makeText(context, "Print service is not available on this device", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (!docxFile.exists() || docxFile.length() == 0L) {
            android.widget.Toast.makeText(context, "Cannot print: File is empty or missing", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        mainHandler.post {
            try {
                val tempWebView = WebView(context)
                activeWebViews.add(tempWebView)

                val docxAssetLoader = androidx.webkit.WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", androidx.webkit.WebViewAssetLoader.AssetsPathHandler(context))
                    .addPathHandler("/docx-src/", object : androidx.webkit.WebViewAssetLoader.PathHandler {
                        override fun handle(path: String): android.webkit.WebResourceResponse? {
                            if (path != "document.docx" || !docxFile.isFile) return null
                            return runCatching {
                                android.webkit.WebResourceResponse(
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    null,
                                    200,
                                    "OK",
                                    mapOf("Content-Length" to docxFile.length().toString()),
                                    FileInputStream(docxFile)
                                )
                            }.getOrNull()
                        }
                    })
                    .build()

                tempWebView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    setSupportZoom(false)
                }

                var printSubmitted = false
                fun triggerPrint() {
                    if (printSubmitted) return
                    printSubmitted = true
                    val printAdapter = tempWebView.createPrintDocumentAdapter(title)
                    printManager.print(
                        title,
                        printAdapter,
                        PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                            .build()
                    )
                    mainHandler.postDelayed({
                        activeWebViews.remove(tempWebView)
                        tempWebView.destroy()
                    }, 60000)
                }

                fun fallbackTextPrint(reason: String) {
                    if (printSubmitted) return
                    printSubmitted = true
                    try {
                        val extracted = com.example.core.format.DocxExtractor.extractText(docxFile)
                        val paragraphsHtml = extracted.paragraphs.joinToString("\n") { p ->
                            val escaped = escapeHtml(p)
                            if (p.startsWith("#") || p.length < 50 && !p.endsWith(".")) {
                                "<h3>$escaped</h3>"
                            } else {
                                "<p>$escaped</p>"
                            }
                        }
                        val escapedTitle = escapeHtml(title)
                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="utf-8">
                                <style>
                                    body { font-family: sans-serif; font-size: 13px; line-height: 1.6; padding: 24px; color: #1a1a1a; }
                                    h2 { margin-top: 0; color: #111; font-size: 20px; border-bottom: 2px solid #ccc; padding-bottom: 8px; }
                                    h3 { color: #222; margin-top: 16px; margin-bottom: 8px; font-size: 15px; }
                                    p { margin: 8px 0; }
                                </style>
                            </head>
                            <body>
                                <h2>$escapedTitle</h2>
                                $paragraphsHtml
                            </body>
                            </html>
                        """.trimIndent()

                        tempWebView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                val printAdapter = tempWebView.createPrintDocumentAdapter(title)
                                printManager.print(
                                    title,
                                    printAdapter,
                                    PrintAttributes.Builder()
                                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                                        .build()
                                )
                                mainHandler.postDelayed({
                                    activeWebViews.remove(tempWebView)
                                    tempWebView.destroy()
                                }, 60000)
                            }
                        }
                        tempWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                        android.widget.Toast.makeText(context, "Printed with text recovery ($reason)", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        activeWebViews.remove(tempWebView)
                        tempWebView.destroy()
                        android.widget.Toast.makeText(context, "Print failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }

                tempWebView.addJavascriptInterface(object : Any() {
                    @android.webkit.JavascriptInterface
                    fun onPrintReady() {
                        mainHandler.post { triggerPrint() }
                    }

                    @android.webkit.JavascriptInterface
                    fun onDocumentLoaded(totalPages: Int) {
                        mainHandler.postDelayed({ triggerPrint() }, 300)
                    }

                    @android.webkit.JavascriptInterface
                    fun onDocumentError(error: String) {
                        mainHandler.post { fallbackTextPrint(error) }
                    }
                }, "Android")

                tempWebView.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): android.webkit.WebResourceResponse? {
                        return request?.url?.let { docxAssetLoader.shouldInterceptRequest(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript("loadDocx(null, false);", null)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            fallbackTextPrint(error?.description?.toString() ?: "Load error")
                        }
                    }
                }

                // Safety timeout: if formatted layout takes more than 12 seconds, fall back
                mainHandler.postDelayed({
                    if (!printSubmitted) {
                        fallbackTextPrint("Format timeout")
                    }
                }, 12000)

                tempWebView.loadUrl("https://appassets.androidplatform.net/assets/editor/docx_editor.html?print=1")
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Print initialization error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun printMarkdown(
        context: Context,
        markdownContent: String,
        title: String
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            android.widget.Toast.makeText(context, "Print service is not available on this device", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        mainHandler.post {
            try {
                val parser = org.intellij.markdown.parser.MarkdownParser(org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor())
                val parsedTree = parser.buildMarkdownTreeFromString(markdownContent)
                val rawHtml = com.example.core.text.HtmlSanitizer.sanitize(
                    org.intellij.markdown.html.HtmlGenerator(markdownContent, parsedTree, org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor()).generateHtml(),
                    keepImages = true
                )

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8">
                        <style>
                            body {
                                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                                font-size: 13px;
                                line-height: 1.6;
                                padding: 24px;
                                color: #1a1a1a;
                            }
                            h1, h2, h3 { color: #111; margin-top: 16px; margin-bottom: 8px; }
                            h1 { font-size: 22px; border-bottom: 2px solid #ddd; padding-bottom: 6px; }
                            h2 { font-size: 18px; }
                            p { margin: 8px 0; }
                            pre { background: #f5f5f5; padding: 12px; border-radius: 6px; font-family: monospace; font-size: 12px; white-space: pre-wrap; }
                            code { font-family: monospace; background: #eee; padding: 2px 4px; border-radius: 3px; font-size: 12px; }
                            table { border-collapse: collapse; width: 100%; margin: 12px 0; }
                            th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }
                            th { background: #f0f0f0; }
                            blockquote { border-left: 4px solid #cc785c; margin: 8px 0; padding-left: 12px; color: #555; }
                        </style>
                    </head>
                    <body>
                        $rawHtml
                    </body>
                    </html>
                """.trimIndent()

                val tempWebView = WebView(context)
                activeWebViews.add(tempWebView)
                tempWebView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val printAdapter = tempWebView.createPrintDocumentAdapter(title)
                        printManager.print(
                            title,
                            printAdapter,
                            PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                                .build()
                        )
                        mainHandler.postDelayed({
                            activeWebViews.remove(tempWebView)
                            tempWebView.destroy()
                        }, 60000)
                    }
                }
                tempWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            } catch (e: Exception) {
                // Fallback to text
                printTextOrMarkdown(context, markdownContent, title)
            }
        }
    }

    fun printTextOrMarkdown(
        context: Context,
        content: String,
        title: String
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            android.widget.Toast.makeText(context, "Print service is not available on this device", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        mainHandler.post {
            val tempWebView = WebView(context)
            activeWebViews.add(tempWebView)

            val escaped = escapeHtml(content)
            val escapedTitle = escapeHtml(title)

            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                            font-size: 13px;
                            line-height: 1.6;
                            padding: 24px;
                            color: #1a1a1a;
                        }
                        h2 {
                            margin-top: 0;
                            color: #111;
                            font-size: 18px;
                        }
                        hr {
                            border: none;
                            border-top: 1px solid #ddd;
                            margin: 12px 0 20px 0;
                        }
                        pre {
                            white-space: pre-wrap;
                            word-wrap: break-word;
                            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
                            font-size: 11px;
                            background: #f8f8f8;
                            padding: 12px;
                            border-radius: 6px;
                            line-height: 1.5;
                        }
                    </style>
                </head>
                <body>
                    <h2>$escapedTitle</h2>
                    <hr>
                    <pre>$escaped</pre>
                </body>
                </html>
            """.trimIndent()

            tempWebView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printAdapter = tempWebView.createPrintDocumentAdapter(title)
                    printManager.print(
                        title,
                        printAdapter,
                        PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                            .build()
                    )
                    mainHandler.postDelayed({
                        activeWebViews.remove(tempWebView)
                        tempWebView.destroy()
                    }, 60000)
                }
            }
            tempWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }

    fun printImage(
        context: Context,
        imageFile: File,
        title: String
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        mainHandler.post {
            val tempWebView = WebView(context)
            activeWebViews.add(tempWebView)
            val imageUri = android.net.Uri.fromFile(imageFile).toString()
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <style>
                        body {
                            margin: 0;
                            padding: 16px;
                            text-align: center;
                            font-family: sans-serif;
                        }
                        h3 {
                            margin-top: 0;
                            color: #333;
                        }
                        img {
                            max-width: 100%;
                            max-height: 90vh;
                            height: auto;
                            object-fit: contain;
                            display: block;
                            margin: 0 auto;
                        }
                    </style>
                </head>
                <body>
                    <h3>$title</h3>
                    <img src="$imageUri" />
                </body>
                </html>
            """.trimIndent()

            tempWebView.settings.apply {
                allowFileAccess = true
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = true
            }

            tempWebView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printAdapter = tempWebView.createPrintDocumentAdapter(title)
                    printManager.print(
                        title,
                        printAdapter,
                        PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                            .build()
                    )
                    mainHandler.postDelayed({
                        activeWebViews.remove(tempWebView)
                        tempWebView.destroy()
                    }, 60000)
                }
            }
            tempWebView.loadDataWithBaseURL("file:///", html, "text/html", "UTF-8", null)
        }
    }
}
