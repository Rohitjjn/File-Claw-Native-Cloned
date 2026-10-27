package com.example.formats.ooxml

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream

/**
 * One hardening policy for all local Office preview/print WebViews.
 *
 * Pages and scripts are served only from the `WebViewAssetLoader` origin. Network
 * loads are blocked even though the app itself declares no INTERNET permission.
 * This does not replace the renderer's fixed path handlers; it simply refuses any
 * request outside those paths and keeps the WebView settings consistent.
 */
object OfficeWebViewSecurity {
    const val ASSET_HOST = "appassets.androidplatform.net"

    private val ALLOWED_PREFIXES = setOf(
        "/assets/editor/",
        "/assets/spreadsheet/",
        "/docx-src/",
        "/pptx-src/",
        "/spreadsheet-src/"
    )

    @Suppress("DEPRECATION")
    fun configure(settings: WebSettings) {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = false
        settings.databaseEnabled = false
        settings.saveFormData = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.allowFileAccessFromFileURLs = false
        settings.allowUniversalAccessFromFileURLs = false
        settings.blockNetworkLoads = true
        settings.blockNetworkImage = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)
        settings.setGeolocationEnabled(false)
    }

    /** True only for the exact local asset-loader origin and the Office renderer routes. */
    fun isAllowedAssetUrl(uri: Uri?): Boolean {
        if (uri == null) return false
        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme == "data" || scheme == "about") return true
        if (scheme != "https" || !uri.host.equals(ASSET_HOST, ignoreCase = true)) return false
        val path = uri.path.orEmpty()
        return ALLOWED_PREFIXES.any(path::startsWith)
    }

    /** Blocks any non-local request before delegating to the bundled asset loader. */
    fun intercept(
        request: WebResourceRequest?,
        assetLoader: WebViewAssetLoader
    ): WebResourceResponse? {
        val uri = request?.url
        if (!isAllowedAssetUrl(uri)) return blocked()
        return assetLoader.shouldInterceptRequest(uri!!)
    }

    /** Navigation guard used by `shouldOverrideUrlLoading`. */
    fun shouldBlockNavigation(request: WebResourceRequest?): Boolean =
        !isAllowedAssetUrl(request?.url)

    private fun blocked(): WebResourceResponse = WebResourceResponse(
        "text/plain",
        "utf-8",
        403,
        "Blocked",
        mapOf("Cache-Control" to "no-store"),
        ByteArrayInputStream("Local Office assets only.".toByteArray(Charsets.UTF_8))
    )
}
