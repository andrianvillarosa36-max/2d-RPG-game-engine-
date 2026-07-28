package com.example.miniengine2d

import android.app.Activity
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * MiniEngine2D is intentionally "just" a WebView shell.
 * All engine logic (live preview, code runner, resource editor) lives in
 * app/src/main/assets/index.html - the exact same file you can test in a
 * desktop browser before ever touching Android Studio or Gradle.
 *
 * Keeping this class tiny means there is very little native surface area
 * that can break. Native features (save/load to device storage, share
 * sheet, file picker for sprite images, etc.) get added here later via
 * webView.addJavascriptInterface(...) - see README.md "Roadmap".
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true

        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
