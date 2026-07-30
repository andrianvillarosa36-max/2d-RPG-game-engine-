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
 *
 * 1. [OrientationBridge] - the web Screen Orientation API is unreliable
 *    inside a plain WebView (it mostly expects true browser fullscreen
 *    first), so play mode's landscape lock goes through the Activity
 *    directly. JS calls window.Android.lockLandscape() / unlockOrientation()
 *    and falls back to the web API if this bridge isn't present (e.g.
 *    testing preview.html in a desktop browser).
 *
 * 2. The WebChromeClient below - a plain WebView silently does nothing
 *    when an <input type="file"> is tapped unless the app explicitly hands
 *    that request off to a native picker. This is what makes "Sprite
 *    image" in the resource editor actually open your photos.
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    companion object {
        private const val FILE_CHOOSER_REQUEST = 51
    }

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
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
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
            val uri = if (resultCode == Activity.RESULT_OK) data?.data else null
            filePathCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
            filePathCallback = null
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
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
