package com.macedonga.visor.kit.testing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.macedonga.visor.kit.protocol.KitJson
import com.macedonga.visor.kit.protocol.Layout
import com.macedonga.visor.kit.protocol.LayoutVideoState
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * A view that draws what the fake Display shows, with the renderer of `visor fake`'s Display simulator, so a layout
 * looks the same in both. A tap on a button or on a box with an id clicks it on the fake Display. DAT's
 * `createPreviewView()`. The icons' font comes from the network; offline, icons show their names.
 */
public fun FakeDisplay.preview(context: Context): View = DisplayPreview(context, this)

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
private class DisplayPreview(context: Context, private val display: FakeDisplay) : WebView(context) {
    private var updates: Job? = null
    private var ready = false

    init {
        settings.javaScriptEnabled = true
        // The page is 600 pixels wide, as the Display; the view scales it to its own width.
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        setBackgroundColor(Color.BLACK)
        addJavascriptInterface(Bridge(), "VisorBridge")
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                ready = true
                show(display.layout.value)
            }
        }
        loadDataWithBaseURL(null, page(), "text/html", "utf-8", null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updates = MainScope().launch { display.layout.collect(::show) }
    }

    override fun onDetachedFromWindow() {
        updates?.cancel()
        super.onDetachedFromWindow()
    }

    private fun show(layout: Layout?) {
        if (!ready) return
        evaluateJavascript("show(${layout?.let { KitJson.encodeToString(Layout.serializer(), it) } ?: "null"})", null)
    }

    // Called on the WebView's own thread. A tap can race a new layout that drops its element; that tap is lost, as on
    // the glasses.
    private inner class Bridge {
        @JavascriptInterface
        fun click(id: String) {
            runCatching { display.click(id) }
        }

        @JavascriptInterface
        fun back() {
            runCatching { display.back() }
        }

        @JavascriptInterface
        fun video(state: String, error: String?) {
            runCatching { display.video(LayoutVideoState(state), error) }
        }
    }

    private fun page(): String = """
        <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=600">
        <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;700&family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@24,400,0..1,0&display=block">
        <style>body { margin: 0; background: #000; } ${resource("display.css")}</style></head>
        <body><div class="vd-screen" id="screen"></div>
        <script>${resource("renderer.js")}
        const on = { click: (id) => VisorBridge.click(id), back: () => VisorBridge.back(), video: (s, e) => VisorBridge.video(s, e || null) };
        function show(layout) { VisorDisplay.render(layout, document.getElementById("screen"), on); }
        show(null);</script></body></html>
    """.trimIndent()

    private fun resource(name: String): String =
        checkNotNull(FakeVisor::class.java.getResourceAsStream("display/$name")) { "kit-fake is missing the simulator's $name." }
            .use { it.readBytes().decodeToString() }
}
