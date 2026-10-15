package com.zenpx.zbrowser

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    private lateinit var xView: WebView
    private lateinit var tgView: WebView
    private var current = 0

    private val UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    inner class Bridge {
        // JS: ZBridge.http("POST", url, headersJson, body) -> response text
        @JavascriptInterface
        fun http(method: String, url: String, headersJson: String, body: String): String {
            return try {
                val c = URL(url).openConnection() as HttpURLConnection
                c.requestMethod = method
                c.connectTimeout = 20000
                c.readTimeout = 60000
                val h = org.json.JSONObject(if (headersJson.isBlank()) "{}" else headersJson)
                h.keys().forEach { c.setRequestProperty(it, h.getString(it)) }
                if (body.isNotEmpty()) {
                    c.doOutput = true
                    c.outputStream.use { it.write(body.toByteArray()) }
                }
                val s = if (c.responseCode < 400) c.inputStream else c.errorStream
                s.bufferedReader().readText()
            } catch (e: Exception) {
                "{\"error\":\"${e.message?.replace("\"", "'")}\"}"
            }
        }

        // JS: ZBridge.go("https://x.com/search?q=...") -> X tab-e load
        @JavascriptInterface
        fun go(url: String) { runOnUiThread { xView.loadUrl(url) } }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun makeWebView(inject: Boolean): WebView {
        val w = WebView(this)
        w.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = UA
            javaScriptCanOpenWindowsAutomatically = true
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(w, true)
        }
        w.webChromeClient = WebChromeClient()
        w.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView, url: String) {
                if (inject) {
                    val js = assets.open("z_scripts.js").bufferedReader().readText()
                    v.evaluateJavascript(js, null)
                }
            }
        }
        if (inject) w.addJavascriptInterface(Bridge(), "ZBridge")
        w.layoutParams = FrameLayout.LayoutParams(-1, -1)
        return w
    }

    private fun show(i: Int) {
        current = i
        xView.visibility = if (i == 0) View.VISIBLE else View.GONE
        tgView.visibility = if (i == 1) View.VISIBLE else View.GONE
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#111111"))
        }
        fun tab(label: String, i: Int) = Button(this).apply {
            text = label
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { show(i) }
        }
        bar.addView(tab("X", 0))
        bar.addView(tab("Telegram", 1))
        bar.addView(Button(this).apply {
            text = "↻"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f)
            setOnClickListener { (if (current == 0) xView else tgView).reload() }
        })

        val frame = FrameLayout(this)
        xView = makeWebView(true)
        tgView = makeWebView(false)
        frame.addView(xView)
        frame.addView(tgView)

        root.addView(bar)
        root.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        xView.loadUrl("https://x.com")
        tgView.loadUrl("https://web.telegram.org/a/")
        show(0)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val w = if (current == 0) xView else tgView
        if (w.canGoBack()) w.goBack() else super.onBackPressed()
    }
}
