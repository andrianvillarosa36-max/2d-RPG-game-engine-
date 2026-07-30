package com.example.miniengine2d

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * MiniEngine2D is intentionally "just" a WebView shell.
 * All engine logic (live preview, code runner, resource editor) lives in
 * app/src/main/assets/index.html - the exact same file you can test in a
 * desktop browser before ever touching Android Studio or Gradle.
 *
 * Two bits of native surface area:
 * - [OrientationBridge]: the web Screen Orientation API is unreliable inside
 *   a plain WebView, so play mode's landscape lock goes through the
 *   Activity itself. JS calls window.Android.lockLandscape() /
 *   unlockOrientation() and falls back to the web API if this bridge isn't
 *   present (e.g. testing preview.html in a desktop browser).
 * - The file-chooser handling below: a bare WebView has no default way to
 *   satisfy <input type="file"> - that's what was silently doing nothing
 *   when picking a "Sprite image". A WebChromeClient with
 *   onShowFileChooser() is what actually launches Android's photo picker
 *   and hands the result back to the page's FileReader code, which was
 *   already correct and needed no changes.
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

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
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: WebChromeClient.FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback

                val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }

                return try {
                    @Suppress("DEPRECATION")
                    startActivityForResult(Intent.createChooser(intent, "Choose a sprite image"), FILE_CHOOSER_REQUEST)
                    true
                } catch (e: Exception) {
                    filePathCallback = null
                    false
                }
            }
        }
        webView.loadUrl("file:///android_asset/index.html")
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            val callback = filePathCallback
            filePathCallback = null
            if (callback == null) return
            val results: Array<Uri>? = if (resultCode == Activity.RESULT_OK && data?.data != null) {
                arrayOf(data.data!!)
            } else {
                null
            }
            callback.onReceiveValue(results)
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    companion object {
        private const val FILE_CHOOSER_REQUEST = 51426
    }
}

