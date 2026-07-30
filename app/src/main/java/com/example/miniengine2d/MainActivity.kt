package com.example.miniengine2d

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * MiniEngine2D is intentionally "just" a WebView shell.
 * All engine logic (live preview, code runner, resource editor) lives in
 * app/src/main/assets/index.html - the exact same file you can test in a
 * desktop browser before ever touching Android Studio or Gradle.
 *
 * The one bit of native surface area is [OrientationBridge] below: the web
 * Screen Orientation API is unreliable inside a plain WebView (it mostly
 * expects the page to be in true browser fullscreen first), so play mode's
 * landscape lock is done the reliable way, through the Activity itself.
 * The JS side calls window.Android.lockLandscape() / unlockOrientation()
 * and falls back to the web API if this bridge isn't present (e.g. when
 * testing preview.html in a desktop browser).
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView

    inner class OrientationBridge {
        @JavascriptInterface
        fun lockLandscape() {
            runOnUiThread { requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE }
        }

        @JavascriptInterface
        fun unlockOrientation() {
            runOnUiThread { requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true

        webView.addJavascriptInterface(OrientationBridge(), "Android")
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
