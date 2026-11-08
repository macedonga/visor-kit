package com.macedonga.visor.kit.testing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.macedonga.visor.kit.protocol.BriefingCard
import com.macedonga.visor.kit.protocol.KitJson
import com.macedonga.visor.kit.protocol.Layout
import com.macedonga.visor.kit.protocol.LayoutVideoState
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer

/**
 * A view that draws what the fake Display shows, with the renderer of `visor fake`'s Display simulator, so a layout
 * looks the same in both. A tap on a button or on a box with an id clicks it on the fake Display. DAT's
 * `createPreviewView()`. The icons' font comes from the network; offline, icons show their names.
 */
public fun FakeDisplay.preview(context: Context): View = DisplayPreview(context, this, briefing = false)

/**
 * A view of the Display home's Briefing with the app's cards, drawn as [preview] draws layouts. A tap on a card opens
 * its detail, a tap in the detail clicks on the fake Display ([FakeDisplay.clickCard]), and a double tap goes back.
 */
public fun FakeDisplay.briefingPreview(context: Context): View = DisplayPreview(context, this, briefing = true)

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
private class DisplayPreview(context: Context, private val display: FakeDisplay, private val briefing: Boolean) : WebView(context) {
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
                if (briefing) showCards(display.cards.value) else show(display.layout.value)
            }
        }
        loadDataWithBaseURL(null, page(), "text/html", "utf-8", null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updates = MainScope().launch { if (briefing) display.cards.collect(::showCards) else display.layout.collect(::show) }
    }

    override fun onDetachedFromWindow() {
        updates?.cancel()
        super.onDetachedFromWindow()
    }

    private fun show(layout: Layout?) {
        if (!ready) return
        evaluateJavascript("show(${layout?.let { KitJson.encodeToString(Layout.serializer(), it) } ?: "null"})", null)
    }

    private fun showCards(cards: List<BriefingCard>) {
        if (!ready) return
        evaluateJavascript("showCards(${KitJson.encodeToString(ListSerializer(BriefingCard.serializer()), cards)})", null)
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
        fun cardClick(card: String, id: String) {
            runCatching { display.clickCard(card, id) }
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
        let briefing = null;
        function showCards(cards) {
          if (briefing) briefing.update(cards);
          else briefing = VisorDisplay.briefing(cards, document.getElementById("screen"), { cardClick: (c, id) => VisorBridge.cardClick(c, id), back() {} });
        }
        // A phone has no back key for the page: a double tap goes back, as the band's double pinch does.
        document.addEventListener("dblclick", () => briefing?.back());
        ${if (briefing) "showCards([]);" else "show(null);"}</script></body></html>
    """.trimIndent()

    private fun resource(name: String): String =
        checkNotNull(FakeVisor::class.java.getResourceAsStream("display/$name")) { "kit-fake is missing the simulator's $name." }
            .use { it.readBytes().decodeToString() }
}
