package com.grabpro

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.format.Formatter
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import com.yausername.youtubedl_android.YoutubeDL
import java.io.ByteArrayInputStream
import org.json.JSONArray

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var bar: EditText
    private lateinit var grab: Button
    private lateinit var list: ListView
    private val foundMap = HashMap<WebView, LinkedHashMap<String, Item>>()
    private val varMap = HashMap<WebView, HashMap<String, Pair<String, Int>>>()
    private val found: LinkedHashMap<String, Item> get() = foundMap.getOrPut(web) { LinkedHashMap() }
    private val downloads = Engine.all
    private val ui = Handler(Looper.getMainLooper())
    private var adOn = true
    private var inkC = 0; private var bgC = 0; private var pillC = 0; private var accC = 0
    private lateinit var lock: TextView
    private lateinit var prog: ProgressBar
    private lateinit var tabsBtn: Button
    private lateinit var pillV: LinearLayout
    private lateinit var srl: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private val priv = HashSet<WebView>()
    private var tick = 0
    private lateinit var frame: FrameLayout
    private val tabs = ArrayList<WebView>()
    private val blockedN = java.util.concurrent.atomic.AtomicInteger()
    private val YT_ADS = "(function(){if(window.__aybYT)return;window.__aybYT=1;setInterval(function(){var v=document.querySelector('video');var ad=document.querySelector('.ad-showing,.ad-interrupting');if(ad&&v&&isFinite(v.duration)){v.muted=true;v.currentTime=v.duration;v.playbackRate=16}var b=document.querySelector('.ytp-ad-skip-button,.ytp-skip-ad-button,.ytp-ad-skip-button-modern');if(b)b.click();document.querySelectorAll('ytd-ad-slot-renderer,#player-ads,.ytp-ad-overlay-container,ytm-promoted-sparkles-web-renderer,ytm-companion-ad-renderer').forEach(function(e){e.style.display='none'})},400)})()"
    private val READER = "(function(){var old=document.getElementById('ayb-reader');if(old){old.remove();return}var best=document.body,bs=0;document.querySelectorAll('article,main,section,div').forEach(function(e){var t=0;for(var i=0;i<e.children.length;i++){if(e.children[i].tagName=='P')t+=e.children[i].textContent.length}if(t>bs){bs=t;best=e}});var d=window.matchMedia('(prefers-color-scheme: dark)').matches;var o=document.createElement('div');o.id='ayb-reader';o.style.cssText='position:fixed;left:0;top:0;right:0;bottom:0;z-index:2147483647;overflow:auto;padding:22px 20px 90px;font:19px/1.75 Georgia,serif;background:'+(d?'#0f1220':'#fafaff')+';color:'+(d?'#edeffa':'#14162b');var h=document.createElement('h1');h.textContent=document.title;h.style.cssText='font:700 26px/1.3 sans-serif;margin:0 0 18px';o.appendChild(h);best.querySelectorAll('p,h2,h3,h4,li,blockquote,img').forEach(function(n){var k=n.cloneNode(true);k.removeAttribute('style');k.removeAttribute('class');if(k.tagName=='IMG'){k.style.cssText='max-width:100%;height:auto';k.src=n.currentSrc||n.src}else{k.style.margin='0 0 14px'}o.appendChild(k)});document.body.appendChild(o)})()"
    private val COSMETIC = "(function(){var q='#onetrust-banner-sdk,#onetrust-consent-sdk,#CybotCookiebotDialog,#CybotCookiebotDialogBodyUnderlay,.cc-window,.cc-banner,.fc-consent-root,#cookie-banner,.cookie-banner,#cookieConsent,.cookie-consent,[id*=cookie-consent],[class*=cookie-consent],[class*=cookie-banner],[id*=cookiebanner]';var ads='.adsbygoogle,ins.adsbygoogle,[id^=google_ads],[id*=div-gpt-ad],[class*=ad-slot],[data-ad-slot]';var s=document.createElement('style');s.textContent=q+','+ads+'{display:none!important}';(document.head||document.documentElement).appendChild(s);if(document.querySelector(q)){document.documentElement.style.setProperty('overflow','auto','important');if(document.body)document.body.style.setProperty('overflow','auto','important')}})()"
    private val SCRAPE = "(function(){var s={};function add(u){if(u&&u.indexOf('http')==0)s[u]=1}function ab(v){try{add(new URL(v,location.href).href)}catch(x){}}var A=['src','data-src','data-full','data-original','data-lazy-src','data-hi-res','poster','href'];document.querySelectorAll('*').forEach(function(e){if(e.currentSrc)add(e.currentSrc);if(!e.getAttribute)return;A.forEach(function(k){var v=e.getAttribute(k);if(v)ab(v)});var ss=e.getAttribute('srcset')||e.getAttribute('data-srcset');if(ss){var best='',bw=-1;ss.split(',').forEach(function(p){var t=p.trim().split(/\\s+/);var w=parseInt(t[1])||0;if(w>bw){bw=w;best=t[0]}});if(best)ab(best)}});document.querySelectorAll('meta').forEach(function(m){var p=m.getAttribute('property')||'';if(p.indexOf('og:image')==0||p.indexOf('og:video')==0||p.indexOf('og:audio')==0)add(m.content)});Object.keys(s).forEach(function(u){GP.add(u)})})()"
    private var lastClip = ""
    private val prefKeys = listOf("subs", "sb", "meta", "wifi", "desktop", "adblock", "dark", "ocr", "lock", "amoled", "clean")
    private val prefDefs = booleanArrayOf(false, false, true, false, false, true, true, true, false, false, true)
    private val prefLabels = arrayOf("Embed subtitles (English)", "Remove sponsor segments (video sites)", "Embed cover art and tags",
        "Download on Wi-Fi only", "Desktop site mode", "Block ads", "Dark pages", "OCR slides (searchable PDF + text file)", "Fingerprint lock for downloads and private tabs", "AMOLED black (dark mode; restart to apply)", "Skip redirect warning pages and strip tracking links")
    private val cats = mapOf(
        "Video" to setOf("mp4", "webm", "mkv", "mov", "avi", "m3u8", "m4v", "flv", "3gp", "ogv", "ts", "mpd"),
        "Audio" to setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "oga", "opus"),
        "Images" to setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif", "heic", "tiff"),
        "Documents" to setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "epub", "txt", "rtf", "odt", "ods", "odp", "csv"),
        "Archives/Apps" to setOf("zip", "rar", "7z", "gz", "tar", "iso", "apk", "exe", "dmg"))
    private val exts = cats.values.flatten().toSet()
    private val adHosts = listOf("doubleclick.net", "googlesyndication.com", "adservice.google", "popads.net", "propellerads", "adsterra", "exoclick", "taboola.com", "outbrain.com")

    private fun mk(u: String, name: String, w: WebView = web) = Item(u, name, w.url ?: "", w.settings.userAgentString, CookieManager.getInstance().getCookie(u) ?: "")

    // sniffs by extension, by "Accept: image/*" on extensionless images, and keeps only the largest size of each image variant
    private fun sniff(w: WebView, u: String, hint: String? = null) {
        if (!u.startsWith("http")) return
        val path = u.substringBefore('?').substringBefore('#')
        var ext = path.substringAfterLast('.', "").lowercase().let { if (it.length > 5 || '/' in it) "" else it }
        if (ext !in exts) { if (hint == null) return; ext = hint }
        val f = foundMap.getOrPut(w) { LinkedHashMap() }
        if (f.containsKey(u)) return
        val vr = varMap.getOrPut(w) { HashMap() }
        val rm = Regex("-(\\d{3,4})\\.(jpg|jpeg|png|webp)$").find(path)
        if (rm != null) {
            val key = path.removeRange(rm.range); val wd = rm.groupValues[1].toInt()
            val old = vr[key]
            if (old != null) { if (old.second >= wd) return; f.remove(old.first) }
            vr[key] = u to wd
        }
        var name = Uri.decode(path.substringAfterLast('/')).ifEmpty { "file.$ext" }
        if (!name.contains('.')) name += ".$ext"
        if (ext == "m3u8" || ext == "mpd") {
            val t = (w.title ?: "").replace(Regex("[\\\\/:*?\"<>|]"), " ").trim().take(80).ifEmpty { name.substringBeforeLast('.') }
            f[u] = Item(u, "$t.mp4", w.url ?: "", w.settings.userAgentString, CookieManager.getInstance().getCookie(u) ?: "", "best")
        } else f[u] = mk(u, name, w)
        if (w === web) badge(f.size)
    }

    private fun clearTab(w: WebView) { foundMap[w]?.clear(); varMap[w]?.clear(); if (w === web) badge(0) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        actionBar?.hide()
        if (android.os.Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        initColors()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bgC) }
        lock = TextView(this).apply { text = "\uD83D\uDD0D"; textSize = 14f; setPadding(dp(14), 0, dp(4), 0) }
        bar = EditText(this).apply {
            hint = "Search ${eng().name} or type a URL"; isSingleLine = true; imeOptions = EditorInfo.IME_ACTION_GO
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            background = null; textSize = 15f; setTextColor(inkC); setHintTextColor(0xFF80868B.toInt()); setSelectAllOnFocus(true)
        }
        val go = ib("\u21BB")
        val pill = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; background = rr(pillC, 24) }
        pillV = pill; pill.addView(lock); pill.addView(bar, LinearLayout.LayoutParams(0, -2, 1f)); pill.addView(go)
        tabsBtn = ib("1", 14f).apply { background = GradientDrawable().apply { setStroke(dp(2), inkC); cornerRadius = dp(6).toFloat() } }
        val mb = ib("\u22EE")
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(6), dp(4), dp(6)) }
        top.addView(pill, LinearLayout.LayoutParams(0, dp(46), 1f))
        top.addView(tabsBtn, LinearLayout.LayoutParams(dp(34), dp(34)).apply { setMargins(dp(8), 0, dp(2), 0) }); top.addView(mb)
        prog = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; visibility = View.GONE; progressTintList = ColorStateList.valueOf(accC) }
        web = WebView(this); tabs.add(web); list = ListView(this).apply { visibility = View.GONE; divider = null }
        frame = FrameLayout(this); frame.addView(web); frame.addView(list)
        grab = Button(this).apply {
            text = "\u2B07 0"; isAllCaps = false; textSize = 15f; setTextColor(0xFFFFFFFF.toInt()); setPadding(0, 0, 0, 0)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(accLight[getSharedPreferences("gp", 0).getInt("accent", 0)].toInt(), 0xFF00B0FF.toInt())).apply { shape = GradientDrawable.OVAL }
            elevation = dp(8).toFloat(); visibility = View.GONE
        }
        frame.addView(grab, FrameLayout.LayoutParams(dp(64), dp(64), Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, dp(16), dp(16)) })
        val back = ib("\u25C0"); val fwd = ib("\u25B6"); val home = ib("\u2302"); val dl = ib("\u2B07"); val yt = ib("", 13f); val st = ib("\u2699")
        back.setOnClickListener { if (web.canGoBack()) web.goBack() }
        fwd.setOnClickListener { if (web.canGoForward()) web.goForward() }
        home.setOnClickListener { loadHome(web) }
        tabsBtn.setOnClickListener { tabsDialog() }
        lock.setOnClickListener { engineSheet() }
        grab.setOnLongClickListener { grabDialog("all"); true }
        val bottom = LinearLayout(this).apply { setBackgroundColor(bgC) }
        for (v in listOf(back, fwd, home, dl, st)) bottom.addView(v, LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(top); root.addView(prog, LinearLayout.LayoutParams(-1, dp(3)))
        srl = androidx.swiperefreshlayout.widget.SwipeRefreshLayout(this).apply {
            addView(frame); setOnRefreshListener { web.reload() }
            setOnChildScrollUpCallback { _, _ -> list.visibility == View.VISIBLE || web.scrollY > 0 }
        }
        root.addView(srl, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(bottom)
        setContentView(root)
        pendingPaste = intent?.action == "com.a2.ayb.PASTE"; pendingSearch = intent?.action == "com.a2.ayb.SEARCH"
        if (intent?.action == null || intent?.action == Intent.ACTION_MAIN) showIntro()

        setup(web)

        fun open() {
            val t = bar.text.toString().trim()
            val bang = Regex("^!(\\w+)\\s+(.+)").find(t)
            val be = bang?.let { m -> engines.firstOrNull { it.code == m.groupValues[1].lowercase() } }
            web.loadUrl(if (bang != null && be != null) be.action + "?" + be.param + "=" + Uri.encode(bang.groupValues[2])
                else if (t.contains(' ') || !t.contains('.')) searchUrl() + Uri.encode(t) else if (t.startsWith("http")) t else "https://$t")
        }
        go.setOnClickListener { open() }
        bar.setOnEditorActionListener { _, _, _ -> open(); true }

        grab.setOnClickListener { web.evaluateJavascript(SCRAPE, null); ui.postDelayed({ if (videoPage()) ytDialog(web.url ?: "", web.title ?: "Video") else grabDialog("all") }, 450) }
        mb.setOnClickListener { menu() }
        st.setOnClickListener { settings() }
        yt.setOnClickListener { web.url?.let { ytDialog(it, web.title ?: "video") } }
        dl.setOnClickListener { if (list.visibility == View.VISIBLE) showList(false) else auth { showList(true) } }

        Engine.load(this)
        if (downloads.any { it.status == "Scheduled" }) startForegroundService(Intent(this, Svc::class.java))
        list.adapter = object : BaseAdapter() {
            override fun getCount() = downloads.size
            override fun getItem(p: Int) = downloads[p]
            override fun getItemId(p: Int) = p.toLong()
            override fun getView(p: Int, v: View?, g: ViewGroup?): View {
                val d = downloads[p]
                val row = (v as? LinearLayout) ?: LinearLayout(this@MainActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12))
                    addView(TextView(context).apply { textSize = 26f; setPadding(0, 0, dp(14), 0) })
                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(TextView(context).apply { textSize = 15f; maxLines = 1; ellipsize = TextUtils.TruncateAt.END; setTextColor(inkC); setTypeface(null, Typeface.BOLD) })
                        addView(TextView(context).apply { textSize = 12f; setTextColor(0xFF80868B.toInt()) })
                        addView(ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progressTintList = ColorStateList.valueOf(accC) },
                            LinearLayout.LayoutParams(-1, dp(4)).apply { topMargin = dp(6) })
                    }, LinearLayout.LayoutParams(0, -2, 1f))
                }
                val col = row.getChildAt(1) as LinearLayout
                val ext = extOf(d)
                (row.getChildAt(0) as TextView).text = when {
                    ext == "pdf" -> "\uD83D\uDCC4"
                    cats["Audio"]!!.contains(ext) || d.ytmode == "mp3" -> "\uD83C\uDFB5"
                    cats["Images"]!!.contains(ext) -> "\uD83D\uDDBC"
                    cats["Archives/Apps"]!!.contains(ext) -> "\uD83D\uDCE6"
                    cats["Documents"]!!.contains(ext) -> "\uD83D\uDCDD"
                    else -> "\uD83C\uDFAC"
                }
                (col.getChildAt(0) as TextView).text = d.name
                if (d.status == "Downloading") d.tickSpeed()
                val pc = d.pct()
                val spd = if (d.status != "Downloading") "" else if (d.ytmode.isNotEmpty()) d.spdText else if (d.bps > 0) Formatter.formatShortFileSize(this@MainActivity, d.bps) + "/s" else ""
                (col.getChildAt(1) as TextView).text = listOf(d.status, if (pc >= 0 && d.status != "Done") "$pc%" else "", spd,
                    if (d.done.get() > 1 && d.ytmode.isEmpty()) Formatter.formatShortFileSize(this@MainActivity, d.done.get()) else "").filter { it.isNotEmpty() }.joinToString(" \u00B7 ")
                (col.getChildAt(2) as ProgressBar).apply { isIndeterminate = pc < 0 && d.status == "Downloading"; progress = maxOf(pc, 0) }
                return row
            }
        }
        list.setOnItemClickListener { _, _, p, _ ->
            val d = downloads[p]
            when {
                d.status == "Downloading" || d.status == "Queued" -> Engine.pause(d)
                d.status == "Scheduled" -> { d.startAt = -1; Engine.start(this, d) }
                d.status == "Done" -> try {
                    startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(d.uri, contentResolver.getType(d.uri!!)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                } catch (e: Exception) { Toast.makeText(this, "No app can open this file", Toast.LENGTH_SHORT).show() }
                else -> Engine.start(this, d)
            }
        }
        list.setOnItemLongClickListener { _, _, p, _ ->
            val d = downloads[p]
            AlertDialog.Builder(this).setItems(arrayOf("Retry / resume", "Copy link", "Share link", "Remove from list", "Delete file")) { _, i ->
                when (i) {
                    0 -> if (d.status != "Downloading" && d.status != "Done") { d.paused = false; d.startAt = -1; Engine.start(this, d) }
                    1 -> (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("link", d.url))
                    2 -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, d.url), "Share link"))
                    3 -> { Engine.pause(d); downloads.remove(d) }
                    else -> { Engine.pause(d); d.uri?.let { try { contentResolver.delete(it, null, null) } catch (_: Exception) {} }; downloads.remove(d) }
                }
                Unit
            }.show()
            true
        }
        ui.post(object : Runnable { override fun run() { (list.adapter as BaseAdapter).notifyDataSetChanged(); if (++tick % 6 == 0) Engine.save(this@MainActivity); ui.postDelayed(this, 500) } })
        adOn = getSharedPreferences("gp", 0).getBoolean("adblock", true); applyDesktop(false)
        intent?.data?.toString()?.let { web.loadUrl(it) } ?: loadHome(web)
        if (intent?.action == Intent.ACTION_SEND) intent.getStringExtra(Intent.EXTRA_TEXT)?.let { t -> Regex("https?://\\S+").find(t)?.value?.let { ytDialog(it, "video") } }
    }

    private fun nat(s: String): Long = Regex("\\d+").find(s.substringAfterLast("slide-", s))?.value?.toLongOrNull() ?: 0L
    private fun extOf(d: Item) = d.name.substringAfterLast('.').lowercase()

    private fun queueAll(l: List<Item>) {
        l.forEach { d -> if (downloads.none { it.url == d.url }) { downloads.add(d); Engine.start(this, d) } }
        showList(true)
    }

    private fun grabDialog(filter: String) {
        val all = found.values.toList().filter { filter == "all" || cats[filter]?.contains(extOf(it)) == true }
            .let { l -> if (filter == "Images") l.sortedBy { nat(it.name) } else l.reversed() }
        if (all.isEmpty()) { Toast.makeText(this, "Nothing found. Scroll or play the page, then tap Grab again.", Toast.LENGTH_LONG).show(); return }
        val checked = BooleanArray(all.size)
        AlertDialog.Builder(this).setTitle((if (filter == "all") "Found" else filter) + " (${all.size})")
            .setMultiChoiceItems(all.map { "[" + extOf(it).uppercase() + "] " + it.name }.toTypedArray(), checked) { _, i, c -> checked[i] = c }
            .setPositiveButton("Download") { _, _ -> queueAll(all.filterIndexed { i, _ -> checked[i] }) }
            .setNeutralButton("All") { _, _ -> queueAll(all) }
            .setNegativeButton("Filter") { _, _ -> filterDialog() }.show()
    }

    private fun filterDialog() {
        val names = arrayOf("All", "Video", "Audio", "Images", "Documents", "Archives/Apps", "All images into one PDF", "Copy all links")
        AlertDialog.Builder(this).setTitle("Show").setItems(names) { _, i ->
            if (i == 6) imagesToPdf(found.values.filter { cats["Images"]!!.contains(extOf(it)) }.sortedBy { nat(it.name) })
            else if (i == 7) {
                (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("links", found.keys.joinToString("\n")))
                Toast.makeText(this, "${found.size} links copied", Toast.LENGTH_SHORT).show()
            }
            else grabDialog(if (i == 0) "all" else names[i])
        }.show()
    }

    // slide decks / galleries: download every image in page order and stitch them into one PDF
    private fun imagesToPdf(imgs: List<Item>) {
        if (imgs.isEmpty()) { Toast.makeText(this, "No images found on this page", Toast.LENGTH_SHORT).show(); return }
        val d = Item("pdf:" + imgs.size, "slides-" + System.currentTimeMillis() / 1000 + ".pdf", "", "", "")
        d.status = "Downloading"; d.segs = imgs.size; downloads.add(d)
        startForegroundService(Intent(this, Svc::class.java)); showList(true)
        Thread {
            try {
                val pdf = android.graphics.pdf.PdfDocument()
                val ocr = Engine.pref(this, "ocr", true)
                val rec = if (ocr) com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS) else null
                val txt = StringBuilder()
                var n = 0
                for (m in imgs) {
                    val b = Engine.getBytes(m)
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(b, 0, b.size) ?: continue
                    val pg = pdf.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(bmp.width, bmp.height, ++n).create())
                    pg.canvas.drawBitmap(bmp, 0f, 0f, null)
                    if (rec != null) try { // near-invisible text layer over each slide = searchable, selectable PDF
                        val res = com.google.android.gms.tasks.Tasks.await(rec.process(com.google.mlkit.vision.common.InputImage.fromBitmap(bmp, 0)))
                        txt.append("--- Slide ").append(n).append(" ---\n").append(res.text).append("\n\n")
                        val p = android.graphics.Paint().apply { color = android.graphics.Color.argb(1, 0, 0, 0) }
                        for (bl in res.textBlocks) for (ln in bl.lines) {
                            val r = ln.boundingBox ?: continue
                            p.textSize = r.height().toFloat()
                            pg.canvas.drawText(ln.text, r.left.toFloat(), r.bottom.toFloat() - r.height() * 0.2f, p)
                        }
                    } catch (_: Exception) {}
                    pdf.finishPage(pg); bmp.recycle(); d.segDone = n
                }
                if (n == 0) throw java.io.IOException("No readable images")
                val uri = Engine.create(this, d.name); d.uri = uri
                contentResolver.openOutputStream(uri)!!.use { pdf.writeTo(it) }; pdf.close()
                contentResolver.update(uri, android.content.ContentValues().apply { put(android.provider.MediaStore.Downloads.IS_PENDING, 0) }, null, null)
                if (txt.isNotBlank()) {
                    val tu = Engine.create(this, d.name.removeSuffix(".pdf") + ".txt")
                    contentResolver.openOutputStream(tu)!!.use { it.write(txt.toString().toByteArray()) }
                    contentResolver.update(tu, android.content.ContentValues().apply { put(android.provider.MediaStore.Downloads.IS_PENDING, 0) }, null, null)
                }
                d.done.set(1); d.total = 1; d.segDone = imgs.size; d.status = "Done"
            } catch (e: Exception) { d.status = "Error: " + (e.message ?: "failed").take(120) }
        }.start()
    }

    // "A² presents AYB+ : Browser + Grabber" launch intro (tap to skip)
    private fun showIntro() {
        val splash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; isClickable = true
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF0A0C1C.toInt(), 0xFF1D1245.toInt()))
        }
        val presents = TextView(this).apply { text = "A\u00B2 presents"; textSize = 14f; letterSpacing = 0.3f; setTextColor(0xCCFFFFFF.toInt()) }
        val logo = ImageView(this).apply { setImageResource(R.drawable.ic_logo); scaleX = 0.7f; scaleY = 0.7f }
        val title = TextView(this).apply {
            text = "AYB+"; textSize = 46f; setTypeface(null, Typeface.BOLD); letterSpacing = 0.04f
            paint.shader = android.graphics.LinearGradient(0f, 0f, paint.measureText("AYB+"), 0f,
                intArrayOf(0xFFB39DFF.toInt(), 0xFF40C4FF.toInt()), null, android.graphics.Shader.TileMode.CLAMP)
        }
        val sub = TextView(this).apply { text = "Browser + Grabber"; textSize = 15f; letterSpacing = 0.15f; setTextColor(0xB3FFFFFF.toInt()) }
        splash.addView(presents, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(28) })
        splash.addView(logo, LinearLayout.LayoutParams(dp(112), dp(112)).apply { bottomMargin = dp(20) })
        splash.addView(title)
        splash.addView(sub, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
        listOf(presents to 100L, logo to 500L, title to 950L, sub to 1250L).forEach { (v, d) ->
            v.alpha = 0f; v.translationY = dp(16).toFloat()
            v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setStartDelay(d).setDuration(600).start()
        }
        addContentView(splash, ViewGroup.LayoutParams(-1, -1))
        fun close() { splash.animate().alpha(0f).setStartDelay(0).setDuration(350).withEndAction { (splash.parent as? ViewGroup)?.removeView(splash) }.start() }
        splash.setOnClickListener { close() }
        ui.postDelayed({ close() }, 2700)
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun rr(c: Int, r: Int) = GradientDrawable().apply { setColor(c); cornerRadius = dp(r).toFloat() }
    private fun ib(t: String, s: Float = 20f) = Button(this).apply {
        text = t; textSize = s; isAllCaps = false; setTextColor(inkC); background = null; minWidth = 0; minimumWidth = 0; setPadding(dp(8), 0, dp(8), 0)
    }
    private val videoHosts = listOf("youtube.com", "youtu.be", "instagram.com", "facebook.com", "fb.watch", "x.com", "twitter.com", "tiktok.com", "vimeo.com", "dailymotion.com", "reddit.com", "twitch.tv", "bilibili.com", "soundcloud.com")
    private fun videoPage(): Boolean { val h = try { Uri.parse(web.url ?: "").host ?: "" } catch (e: Exception) { "" }; return videoHosts.any { h == it || h.endsWith(".$it") } }
    private fun badge(n: Int) { grab.text = if (n > 0) "\u2B07 $n" else "\u2B07"; grab.visibility = if (n > 0 || videoPage()) View.VISIBLE else View.GONE }
    private class Eng(val name: String, val action: String, val param: String, val code: String)
    private val engines = listOf(
        Eng("Google", "https://www.google.com/search", "q", "g"), Eng("Bing", "https://www.bing.com/search", "q", "b"),
        Eng("DuckDuckGo", "https://duckduckgo.com/", "q", "d"), Eng("Brave", "https://search.brave.com/search", "q", "br"),
        Eng("Yahoo", "https://search.yahoo.com/search", "p", "y"), Eng("Ecosia", "https://www.ecosia.org/search", "q", "e"),
        Eng("Startpage", "https://www.startpage.com/do/search", "query", "sp"), Eng("Yandex", "https://yandex.com/search/", "text", "yx"),
        Eng("Baidu", "https://www.baidu.com/s", "wd", "bd"), Eng("Wikipedia", "https://en.wikipedia.org/w/index.php", "search", "w"),
        Eng("YouTube", "https://m.youtube.com/results", "search_query", "yt"))
    private fun eng(): Eng { val n = getSharedPreferences("gp", 0).getString("engine", "Google"); return engines.firstOrNull { it.name == n } ?: engines[0] }
    private fun searchUrl() = eng().let { it.action + "?" + it.param + "=" }

    // tap the search icon in the address bar to switch engine; type "!yt cats" or "!b news" to search one engine just once
    private fun engineSheet() {
        sheet("Search engine", engines.map { e ->
            ((if (e.name == eng().name) "\u2705  " else "\uD83D\uDD0D  ") + e.name + "   (!" + e.code + ")") to {
                getSharedPreferences("gp", 0).edit().putString("engine", e.name).apply()
                bar.hint = "Search ${e.name} or type a URL"
                if (web.url?.startsWith("https://start.grabpro") == true) loadHome(web)
            }
        })
    }

    private val accLight = listOf(0xFF6C47FF, 0xFF1A73E8, 0xFF00897B, 0xFF2E7D32, 0xFFEF6C00, 0xFFD81B60)
    private val accDark = listOf(0xFFA78BFF, 0xFF8AB4F8, 0xFF4DB6AC, 0xFF81C784, 0xFFFFB74D, 0xFFF48FB1)

    private fun accentSheet() {
        val names = listOf("Violet", "Blue", "Teal", "Green", "Orange", "Pink")
        sheet("Accent colour", names.mapIndexed { i, n -> ("\u25CF  " + n) to {
            getSharedPreferences("gp", 0).edit().putInt("accent", i).apply()
            Toast.makeText(this, "Restart AYB+ to apply $n", Toast.LENGTH_LONG).show()
        } })
    }

    private fun initColors() {
        val night = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        bgC = if (night) 0xFF0F1220.toInt() else 0xFFFAFAFF.toInt(); pillC = if (night) 0xFF1B2036.toInt() else 0xFFEEF0FB.toInt()
        inkC = if (night) 0xFFEDEFFA.toInt() else 0xFF14162B.toInt(); accC = (if (night) accDark else accLight)[getSharedPreferences("gp", 0).getInt("accent", 0)].toInt()
        if (night && getSharedPreferences("gp", 0).getBoolean("amoled", false)) { bgC = 0xFF000000.toInt(); pillC = 0xFF141414.toInt() }
        window.statusBarColor = bgC; window.navigationBarColor = bgC
        window.decorView.systemUiVisibility = if (night) 0 else (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
    }

    private val groupOf = HashMap<WebView, Int>()
    private val dots = listOf("\u26AA", "\uD83D\uDD34", "\uD83D\uDFE0", "\uD83D\uDFE2", "\uD83D\uDD35", "\uD83D\uDFE3")

    private fun tabsDialog() {
        val rows = mutableListOf<Pair<String, () -> Unit>>()
        tabs.forEachIndexed { k, t ->
            rows.add(((if (priv.contains(t)) "\uD83D\uDD76 " else dots[groupOf[t] ?: 0] + " ") + (t.title ?: "New tab").take(38)) to { select(k) })
        }
        rows.add("\uD83C\uDFA8  Set group for this tab" to { groupSheet() })
        rows.add("\uD83E\uDDF9  Close all tabs in this tab's group" to { closeGroup() })
        rows.add("\u2795  New tab" to { newTab(null) })
        rows.add("\u2716  Close this tab" to { closeTab() })
        sheet("Tabs (${tabs.size})", rows)
    }

    private fun groupSheet() {
        sheet("Tab group", dots.mapIndexed { i, d -> (d + "  " + (if (i == 0) "No group" else "Group $i")) to { groupOf[web] = i; Unit } })
    }

    private fun closeGroup() {
        val g = groupOf[web] ?: 0
        if (g == 0) { Toast.makeText(this, "This tab isn't in a group", Toast.LENGTH_SHORT).show(); return }
        tabs.filter { groupOf[it] == g }.forEach { t -> if (tabs.size > 1) { web = t; closeTab() } }
    }

    private fun readerMode() { web.evaluateJavascript(READER, null) }

    private fun translate(lang: String) {
        val u = Uri.parse(web.url ?: return)
        val h = (u.host ?: return).replace("-", "--").replace(".", "-") + ".translate.goog"
        val q = (u.encodedQuery?.let { "$it&" } ?: "") + "_x_tr_sl=auto&_x_tr_tl=$lang&_x_tr_hl=$lang&_x_tr_pto=wapp"
        web.loadUrl("https://$h${u.encodedPath ?: "/"}?$q")
    }

    private fun langSheet() {
        val langs = listOf("English" to "en", "Hindi" to "hi", "Spanish" to "es", "French" to "fr", "German" to "de", "Arabic" to "ar", "Portuguese" to "pt",
            "Russian" to "ru", "Japanese" to "ja", "Chinese" to "zh-CN", "Bengali" to "bn", "Urdu" to "ur", "Tamil" to "ta", "Telugu" to "te")
        sheet("Translate this page to...", langs.map { (n, c) -> ("\uD83C\uDF10  " + n) to { translate(c) } })
    }

    private var unlockedUntil = 0L
    private fun auth(ok: () -> Unit) {
        val kg = getSystemService(android.app.KeyguardManager::class.java)
        if (!Engine.pref(this, "lock") || !kg.isDeviceSecure || System.currentTimeMillis() < unlockedUntil) { ok(); return }
        val b = android.hardware.biometrics.BiometricPrompt.Builder(this).setTitle("AYB+").setSubtitle("Unlock to continue")
        if (android.os.Build.VERSION.SDK_INT >= 30) b.setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        else b.setDeviceCredentialAllowed(true)
        b.build().authenticate(android.os.CancellationSignal(), mainExecutor, object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(r: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) { unlockedUntil = System.currentTimeMillis() + 120000; ok() }
        })
    }

    private fun closeTab() {
        if (tabs.size > 1) { val k = tabs.indexOf(web); frame.removeView(web); foundMap.remove(web); varMap.remove(web); priv.remove(web); web.destroy(); tabs.removeAt(k); if (priv.isEmpty()) try { androidx.webkit.ProfileStore.getInstance().deleteProfile("private") } catch (_: Exception) {}; select(minOf(k, tabs.size - 1)) }
    }

    private fun loadHome(w: WebView) {
        val act = eng().action
        val sites = listOf("YouTube" to "https://m.youtube.com", "Google" to "https://www.google.com", "SlideShare" to "https://www.slideshare.net",
            "Instagram" to "https://www.instagram.com", "X" to "https://x.com", "Facebook" to "https://m.facebook.com", "Wikipedia" to "https://www.wikipedia.org",
            "Reddit" to "https://www.reddit.com", "Archive" to "https://archive.org", "Pinterest" to "https://www.pinterest.com")
        val tiles = sites.joinToString("") { (n, u) -> "<a class=t href='$u'><i>${n[0]}</i>$n</a>" }
        val html = """<!doctype html><html><head><meta name=viewport content='width=device-width,initial-scale=1'><meta name=color-scheme content='light dark'><style>
*{box-sizing:border-box}body{margin:0;min-height:100vh;font-family:sans-serif;text-align:center;color:#14162b;background:radial-gradient(900px 500px at 50% -10%,#e4dcff,transparent),#fafaff}
@media(prefers-color-scheme:dark){body{color:#edeffa;background:radial-gradient(900px 500px at 50% -10%,#3a2a7a,transparent),#0f1220}.s{background:#1b2036!important;border-color:#2a3052!important;color:#edeffa}.t i{background:#1b2036!important}}
svg{margin-top:7vh;filter:drop-shadow(0 8px 20px #7c4dff66)}.a{margin-top:14px;font-size:12px;letter-spacing:3px;text-transform:uppercase;opacity:.65}
h1{margin:6px 0 2px;font-size:56px;font-weight:800;letter-spacing:-2px;background:linear-gradient(90deg,#7c4dff,#00b0ff);-webkit-background-clip:text;color:transparent}
.sub{opacity:.7;font-size:14px;margin-bottom:26px}
.s{width:86%;max-width:560px;padding:15px 22px;border-radius:30px;border:1px solid #e0dcf5;font-size:16px;background:#fff;box-shadow:0 8px 30px #7c4dff22}
.g{display:grid;grid-template-columns:repeat(4,1fr);gap:18px 6px;max-width:520px;margin:34px auto;padding:0 12px}
a{text-decoration:none;color:inherit;font-size:11.5px}
.t i{display:grid;place-items:center;width:56px;height:56px;margin:0 auto 7px;border-radius:18px;background:#fff;box-shadow:0 4px 14px #7c4dff22;font:700 22px sans-serif;font-style:normal;color:#7c4dff}
</style></head><body><svg width=76 height=76 viewBox='0 0 108 108'><defs><linearGradient id=g x1=0 y1=0 x2=1 y2=1><stop offset=0 stop-color='#5b2ee6'/><stop offset=.5 stop-color='#7c4dff'/><stop offset=1 stop-color='#00b0ff'/></linearGradient></defs><rect width=108 height=108 rx=26 fill='url(#g)'/><g fill=none stroke=#fff stroke-width=7 stroke-linecap=round stroke-linejoin=round><path d='M35,84 L54,30 L73,84'/><path d='M54,48 L54,66 M49.5,61.5 L54,66 L58.5,61.5' stroke-width=4.5 /><path d='M77,27 L77,39 M71,33 L83,33' stroke=#b3ecff stroke-width=4 /></g></svg><div class=a>A&sup2; presents</div><h1>AYB+</h1><div class=sub>Browser + Grabber</div><form action='$act'><input class=s name=${eng().param} placeholder='Search ${eng().name} or type a URL' autocomplete=off></form><div class=g>$tiles</div></body></html>"""
        w.loadDataWithBaseURL("https://start.grabpro/", html, "text/html", "utf-8", null)
    }

    private fun setup(w: WebView) {
        w.webChromeClient = object : WebChromeClient() { override fun onProgressChanged(v: WebView, p: Int) { if (v === web) { prog.progress = p; prog.visibility = if (p in 1..99) View.VISIBLE else View.GONE; if (p == 100) srl.isRefreshing = false } } }
        w.setOnLongClickListener {
            val r = w.hitTestResult; val u = r.extra
            if (u == null || !u.startsWith("http") || r.type == WebView.HitTestResult.UNKNOWN_TYPE || r.type == WebView.HitTestResult.EDIT_TEXT_TYPE) false
            else {
                val img = r.type == WebView.HitTestResult.IMAGE_TYPE || r.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                val opts = arrayOf(if (img) "Download image" else "Download file", "Open in new tab", "Copy link", "Unshorten link", "Download as video/audio")
                AlertDialog.Builder(this).setTitle(u.take(60)).setItems(opts) { _, i ->
                    when (i) {
                        0 -> {
                            var n = Uri.decode(u.substringBefore('?').substringAfterLast('/')).ifEmpty { "file" }
                            if (!n.contains('.')) n += if (img) ".jpg" else ".bin"
                            val d = mk(u, n, w); downloads.add(d); Engine.start(this, d); showList(true)
                        }
                        1 -> newTab(u)
                        2 -> (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("link", u))
                        3 -> unshortenDialog(u)
                        else -> ytDialog(u, "video")
                    }
                    Unit
                }.show()
                true
            }
        }
        w.settings.javaScriptCanOpenWindowsAutomatically = false
        w.settings.apply { javaScriptEnabled = true; domStorageEnabled = true; mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW }
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, true)
        w.addJavascriptInterface(object { @JavascriptInterface fun add(u: String) { runOnUiThread { sniff(w, u) } } }, "GP")
        w.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest): WebResourceResponse? {
                val u = r.url.toString()
                if (adOn && !r.isForMainFrame && (AdBlock.blocked(r.url.host) || adHosts.any { r.url.host?.contains(it) == true })) { blockedN.incrementAndGet(); return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0))) }
                val acc = r.requestHeaders["Accept"] ?: ""
                val hint = if (acc.startsWith("image/")) "jpg" else null
                runOnUiThread { sniff(v, u, hint) }
                return null
            }
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                if (adOn && !r.hasGesture() && AdBlock.blocked(r.url.host)) { blockedN.incrementAndGet(); return true }
                if (r.isForMainFrame && Engine.pref(this@MainActivity, "clean", true)) {
                    val s = r.url.toString(); val t = clean(unwrap(s) ?: s)
                    if (t != s) { v.post { v.loadUrl(t) }; return true }
                }
                return false
            }
            override fun onPageStarted(v: WebView, u: String, f: android.graphics.Bitmap?) { clearTab(v); if (v === web) { val sp = u.startsWith("https://start.grabpro"); bar.setText(if (sp) "" else u); lock.text = if (sp) "\uD83D\uDD0D" else if (u.startsWith("https")) "\uD83D\uDD12" else "\u24D8" } }
            override fun onPageFinished(v: WebView, u: String) {
                if (v === web) badge(found.size)
                if (adOn) v.evaluateJavascript(COSMETIC, null)
                if (adOn && u.contains("youtube.com")) v.evaluateJavascript(YT_ADS, null)
                v.evaluateJavascript(SCRAPE, null)
            }
        }
        w.setDownloadListener { u, _, cd, mime, _ ->
            val d = mk(u, URLUtil.guessFileName(u, cd, mime)); downloads.add(d); Engine.start(this, d)
            Toast.makeText(this, "Downloading ${d.name}", Toast.LENGTH_SHORT).show()
        }

    }

    private fun newTab(url: String?, incog: Boolean = false) {
        val w = WebView(this)
        if (incog) {
            if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.MULTI_PROFILE)) {
                androidx.webkit.ProfileStore.getInstance().getOrCreateProfile("private"); androidx.webkit.WebViewCompat.setProfile(w, "private"); priv.add(w)
            } else { Toast.makeText(this, "Private tabs need a newer Android System WebView. Update it in the Play Store.", Toast.LENGTH_LONG).show(); w.destroy(); return }
        }
        setup(w); tabs.add(w); frame.addView(w, 0); applyDesktop(false); select(tabs.size - 1)
        if (url != null) w.loadUrl(url) else loadHome(w)
    }

    private fun select(i: Int) {
        tabs.forEachIndexed { k, t -> t.visibility = if (k == i) View.VISIBLE else View.GONE }
        web = tabs[i]; tabsBtn.text = tabs.size.toString(); pillV.background = rr(if (priv.contains(web)) 0xFF4A3B6B.toInt() else pillC, 24); bar.setText(web.url?.takeIf { !it.startsWith("https://start.grabpro") } ?: ""); badge(found.size); showList(false)
    }

    private fun loadBm(): MutableList<Array<String>> {
        val out = mutableListOf<Array<String>>()
        try { val a = JSONArray(getSharedPreferences("gp", 0).getString("bm", "[]")); for (i in 0 until a.length()) { val o = a.getJSONArray(i); out.add(arrayOf(o.getString(0), o.getString(1))) } } catch (_: Exception) {}
        return out
    }

    private fun saveBm(l: List<Array<String>>) {
        val a = JSONArray(); l.forEach { a.put(JSONArray(listOf(it[0], it[1]))) }
        getSharedPreferences("gp", 0).edit().putString("bm", a.toString()).apply()
    }

    private fun menu() {
        val rows = listOf<Pair<String, () -> Unit>>(
            "\uD83D\uDDC2  Tabs (${tabs.size})" to { tabsDialog() },
            "\u2795  New tab" to { newTab(null) },
            "\uD83D\uDD0D  Search engine \u00B7 ${eng().name}" to { engineSheet() },
            "\uD83D\uDD76  New private tab" to { auth { newTab(null, true) } },
            "\u2B50  Bookmark this page" to { web.url?.let { u -> val l = loadBm(); l.add(arrayOf(web.title ?: u, u)); saveBm(l); Toast.makeText(this, "Bookmarked", Toast.LENGTH_SHORT).show() }; Unit },
            "\uD83D\uDD16  Bookmarks" to { bookmarks() },
            "\uD83D\uDCD6  Reader mode" to { readerMode() },
            "\uD83C\uDF10  Translate page" to { langSheet() },
            "\uD83D\uDD0E  Find in page" to { findDialog() },
            "\u23ED  Find next" to { web.findNext(true) },
            "\uD83D\uDCE4  Share page link" to { web.url?.let { u -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, u), "Share")) }; Unit },
            "\uD83D\uDDA8  Save page as PDF" to { val t = web.title ?: "page"; (getSystemService(PRINT_SERVICE) as android.print.PrintManager).print(t, web.createPrintDocumentAdapter(t), android.print.PrintAttributes.Builder().build()); Unit },
            "\uD83D\uDCE5  Open downloads folder" to { startActivity(Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS)) },
            "\uD83E\uDDF9  Clear browsing data" to {
                CookieManager.getInstance().removeAllCookies(null); WebStorage.getInstance().deleteAllData(); web.clearCache(true); web.clearHistory()
                Toast.makeText(this, "Cookies, cache and site data cleared", Toast.LENGTH_SHORT).show() },
            "\uD83D\uDD78  Scan linked pages (site grabber)" to { deepScan() },
            "\uD83D\uDD22  Numbered series (batch)" to { patternDialog() },
            "\uD83D\uDEE1  Blocked this session: ${blockedN.get()}" to { Toast.makeText(this, "${blockedN.get()} ads and trackers blocked", Toast.LENGTH_SHORT).show() },
            "\uD83D\uDD17  Unshorten a link" to { unshortenDialog(lastClip) },
            "\u26A1  Speed test" to { speedTest() },
            "\uD83D\uDDC2  My files (file manager)" to { auth { filesScreen() } },
            "\uD83C\uDFA8  Accent colour" to { accentSheet() },
            "\uD83D\uDCBE  Backup settings, bookmarks, history" to { backup() },
            "\u267B  Restore from backup" to { restore() },
            "\uD83C\uDFE0  Home" to { loadHome(web) },
            "\u2716  Close this tab" to { closeTab() })
        sheet("AYB+  \u00B7  A\u00B2 presents", rows)
    }

    // IDM-style site grabber: reads this page and up to 40 same-site pages it links to, and collects every downloadable file
    private fun deepScan() {
        val start = web.url ?: return
        val w = web; val host = Uri.parse(start).host ?: return
        val cookie = CookieManager.getInstance().getCookie(start) ?: ""; val ua = w.settings.userAgentString
        Toast.makeText(this, "Scanning linked pages...", Toast.LENGTH_SHORT).show()
        Thread {
            val seen = HashSet<String>(); val queue = ArrayDeque<String>(); val out = LinkedHashSet<String>()
            queue.add(start); seen.add(start); var pages = 0
            val re = Regex("(?:href|src|data-src)\\s*=\\s*[\"']([^\"'#]+)[\"']", RegexOption.IGNORE_CASE)
            val pageExt = setOf("", "html", "htm", "php", "asp", "aspx", "jsp")
            while (queue.isNotEmpty() && pages < 40) {
                val p = queue.removeFirst(); pages++
                val html = try {
                    val c = java.net.URL(p).openConnection() as java.net.HttpURLConnection
                    c.connectTimeout = 10000; c.readTimeout = 10000
                    c.setRequestProperty("User-Agent", ua); c.setRequestProperty("Cookie", cookie); c.setRequestProperty("Referer", start)
                    c.inputStream.bufferedReader().use { it.readText().take(2_000_000) }
                } catch (e: Exception) { continue }
                for (m in re.findAll(html)) {
                    val u = try { java.net.URL(java.net.URL(p), m.groupValues[1]).toString() } catch (e: Exception) { continue }
                    val uri = Uri.parse(u); val ext = (uri.path ?: "").substringAfterLast('/').substringAfterLast('.', "").lowercase()
                    if (ext in exts) out.add(u)
                    else if (uri.host == host && ext in pageExt && seen.size < 400 && seen.add(u)) queue.add(u)
                }
            }
            runOnUiThread {
                out.forEach { sniff(w, it) }
                Toast.makeText(this, "Scanned $pages pages, found ${out.size} files", Toast.LENGTH_LONG).show()
                grabDialog("all")
            }
        }.start()
    }

    // numbered series: https://site.com/img{001..120}.jpg  ->  120 downloads
    private fun patternDialog() {
        val et = EditText(this).apply { hint = "https://site.com/img{001..120}.jpg"; setText(web.url ?: "") }
        AlertDialog.Builder(this).setTitle("Numbered series").setMessage("Put a {from..to} range in the link, e.g. page{1..50}.jpg or file{001..120}.pdf").setView(et)
            .setPositiveButton("Add to queue") { _, _ ->
                val pat = et.text.toString().trim()
                val m = Regex("\\{(\\d+)\\.\\.(\\d+)\\}").find(pat)
                if (m == null) { Toast.makeText(this, "Add a {from..to} range to the link", Toast.LENGTH_LONG).show(); return@setPositiveButton }
                val a = m.groupValues[1]; val b = m.groupValues[2].toInt(); val pad = if (a.startsWith("0")) a.length else 0
                val items = (a.toInt()..minOf(b, a.toInt() + 1999)).map { n ->
                    val u = pat.replaceRange(m.range, n.toString().padStart(pad, '0'))
                    mk(u, Uri.decode(u.substringBefore('?').substringAfterLast('/')).ifEmpty { "file" })
                }
                queueAll(items); Toast.makeText(this, "${items.size} files queued", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun backup() {
        try {
            val sp = getSharedPreferences("gp", 0); val o = org.json.JSONObject()
            for ((k, v) in sp.all) {
                if (k == "upd" || v == null) continue
                var s = v.toString()
                if (k == "hist") { val h = JSONArray(s); for (i in 0 until h.length()) h.getJSONObject(i).put("c", ""); s = h.toString() } // never export login cookies
                o.put(k, org.json.JSONObject().put("t", when (v) { is Boolean -> "b"; is Int -> "i"; is Long -> "l"; else -> "s" }).put("v", s))
            }
            val name = "AYB-backup-" + java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US).format(java.util.Date()) + ".json"
            val uri = Engine.create(this, name)
            contentResolver.openOutputStream(uri)!!.use { it.write(o.toString(2).toByteArray()) }
            contentResolver.update(uri, android.content.ContentValues().apply { put(android.provider.MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            Toast.makeText(this, "Backup saved: Downloads/AYB+/$name", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Toast.makeText(this, "Backup failed: " + e.message, Toast.LENGTH_LONG).show() }
    }

    private fun restore() { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), 77) }

    @Deprecated("fine")
    override fun onActivityResult(rc: Int, res: Int, data: Intent?) {
        super.onActivityResult(rc, res, data)
        if (rc != 77 || res != RESULT_OK) return
        try {
            val o = org.json.JSONObject(contentResolver.openInputStream(data!!.data!!)!!.bufferedReader().use { it.readText() })
            val e = getSharedPreferences("gp", 0).edit()
            for (k in o.keys()) {
                val x = o.getJSONObject(k); val v = x.getString("v")
                when (x.getString("t")) { "b" -> e.putBoolean(k, v.toBoolean()); "i" -> e.putInt(k, v.toInt()); "l" -> e.putLong(k, v.toLong()); else -> e.putString(k, v) }
            }
            e.apply(); Toast.makeText(this, "Restored. Restart AYB+ to apply everything.", Toast.LENGTH_LONG).show()
        } catch (ex: Exception) { Toast.makeText(this, "That isn't a valid AYB+ backup", Toast.LENGTH_LONG).show() }
    }

    // home-screen widget actions + links/shares sent to the already-open app
    private var pendingPaste = false
    private var pendingSearch = false
    override fun onNewIntent(i: Intent?) {
        super.onNewIntent(i); setIntent(i)
        when (i?.action) {
            Intent.ACTION_VIEW -> i.data?.toString()?.let { newTab(it) }
            Intent.ACTION_SEND -> i.getStringExtra(Intent.EXTRA_TEXT)?.let { t -> Regex("https?://\\S+").find(t)?.value?.let { ytDialog(it, "Video") } }
            "com.a2.ayb.PASTE" -> pendingPaste = true
            "com.a2.ayb.SEARCH" -> pendingSearch = true
        }
    }

    // ---------- speed test (Cloudflare's public test endpoints): ping, download, upload ----------
    private fun speedTest() {
        val tv = TextView(this).apply { textSize = 16f; setTextColor(inkC); setPadding(dp(24), dp(12), dp(24), dp(12)); text = "Starting..." }
        val stop = java.util.concurrent.atomic.AtomicBoolean(false)
        val dlg = AlertDialog.Builder(this).setTitle("\u26A1 Speed test").setView(tv)
            .setPositiveButton("Run again") { _, _ -> speedTest() }.setNegativeButton("Close", null).create()
        dlg.setOnDismissListener { stop.set(true) }
        dlg.show()
        Thread {
            fun ui(s: String) { runOnUiThread { if (!stop.get()) tv.text = s } }
            val base = "https://speed.cloudflare.com"
            try {
                val pings = ArrayList<Long>()
                repeat(6) {
                    val t = System.nanoTime()
                    val c = java.net.URL("$base/__down?bytes=0").openConnection() as java.net.HttpURLConnection
                    c.useCaches = false; c.inputStream.use { s -> s.readBytes() }
                    pings.add((System.nanoTime() - t) / 1_000_000)
                }
                pings.sort(); val ping = pings[pings.size / 2]
                ui("Ping  $ping ms\nDownload  testing...")
                val got = java.util.concurrent.atomic.AtomicLong(); val t0 = System.nanoTime(); val end = t0 + 8_000_000_000L
                val dths = (1..4).map {
                    Thread {
                        try {
                            while (System.nanoTime() < end && !stop.get()) {
                                val c = java.net.URL("$base/__down?bytes=50000000").openConnection() as java.net.HttpURLConnection
                                c.readTimeout = 8000
                                c.inputStream.use { i ->
                                    val b = ByteArray(65536)
                                    while (System.nanoTime() < end && !stop.get()) { val n = i.read(b); if (n < 0) break; got.addAndGet(n.toLong()) }
                                }
                            }
                        } catch (_: Exception) {}
                    }.apply { start() }
                }
                while (dths.any { it.isAlive } && !stop.get()) {
                    Thread.sleep(250)
                    ui("Ping  $ping ms\nDownload  %.1f Mbps".format(got.get() * 8.0 / ((System.nanoTime() - t0) / 1e9) / 1e6))
                }
                val down = got.get() * 8.0 / ((System.nanoTime() - t0) / 1e9) / 1e6
                ui("Ping  $ping ms\nDownload  %.1f Mbps\nUpload  testing...".format(down))
                val sent = java.util.concurrent.atomic.AtomicLong(); val u0 = System.nanoTime(); val uend = u0 + 6_000_000_000L
                val uths = (1..3).map {
                    Thread {
                        try {
                            val chunk = ByteArray(65536)
                            while (System.nanoTime() < uend && !stop.get()) {
                                val c = java.net.URL("$base/__up").openConnection() as java.net.HttpURLConnection
                                c.requestMethod = "POST"; c.doOutput = true; c.setFixedLengthStreamingMode(2_000_000); c.readTimeout = 8000
                                c.outputStream.use { o -> var w = 0; while (w < 2_000_000) { val n = minOf(chunk.size, 2_000_000 - w); o.write(chunk, 0, n); w += n; sent.addAndGet(n.toLong()) } }
                                c.inputStream.use { s -> s.readBytes() }
                            }
                        } catch (_: Exception) {}
                    }.apply { start() }
                }
                while (uths.any { it.isAlive } && !stop.get()) {
                    Thread.sleep(250)
                    ui("Ping  $ping ms\nDownload  %.1f Mbps\nUpload  %.1f Mbps".format(down, sent.get() * 8.0 / ((System.nanoTime() - u0) / 1e9) / 1e6))
                }
                val up = sent.get() * 8.0 / ((System.nanoTime() - u0) / 1e9) / 1e6
                val tier = if (down >= 25) "Plenty for 4K streaming" else if (down >= 8) "Good for HD video" else "Fine for SD video and browsing"
                ui("Ping  $ping ms\nDownload  %.1f Mbps\nUpload  %.1f Mbps\n\n$tier".format(down, up))
            } catch (e: Exception) { ui("Speed test failed: ${e.message}\nCheck your connection and try again.") }
        }.start()
    }

    // ---------- built-in file manager for everything saved in Downloads/AYB+ ----------
    private class FRow(val uri: Uri, val name: String, val size: Long, val date: Long, val mime: String)

    private fun openFile(r: FRow) {
        try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(r.uri, r.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
        catch (e: Exception) { Toast.makeText(this, "No app can open this file", Toast.LENGTH_SHORT).show() }
    }

    private fun renameFile(r: FRow, done: () -> Unit) {
        val et = EditText(this).apply { setText(r.name) }
        AlertDialog.Builder(this).setTitle("Rename").setView(et).setPositiveButton("Save") { _, _ ->
            try { contentResolver.update(r.uri, android.content.ContentValues().apply { put(android.provider.MediaStore.Downloads.DISPLAY_NAME, et.text.toString()) }, null, null) }
            catch (e: Exception) { Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show() }
            done()
        }.setNegativeButton("Cancel", null).show()
    }

    private fun filesScreen() {
        val all = ArrayList<FRow>(); val shown = ArrayList<FRow>()
        var filter = "All"; var sort = 0; var q = ""
        val dlg = android.app.Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val head = TextView(this).apply { textSize = 18f; setTypeface(null, Typeface.BOLD); setTextColor(inkC); setPadding(dp(18), dp(16), dp(18), dp(2)) }
        val sub = TextView(this).apply { textSize = 12f; setTextColor(0xFF80868B.toInt()); setPadding(dp(18), 0, dp(18), dp(8)) }
        val search = EditText(this).apply { hint = "Search files"; isSingleLine = true; setTextColor(inkC); setHintTextColor(0xFF80868B.toInt()); background = rr(pillC, 22); setPadding(dp(16), dp(10), dp(16), dp(10)) }
        val chips = LinearLayout(this)
        val lv = ListView(this).apply { divider = null }
        val dl = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
        fun load() {
            all.clear()
            contentResolver.query(dl, arrayOf(android.provider.MediaStore.Downloads._ID, android.provider.MediaStore.Downloads.DISPLAY_NAME, android.provider.MediaStore.Downloads.SIZE,
                android.provider.MediaStore.Downloads.DATE_ADDED, android.provider.MediaStore.Downloads.MIME_TYPE),
                android.provider.MediaStore.Downloads.RELATIVE_PATH + " LIKE ?", arrayOf("Download/AYB+%"), null)?.use { c ->
                while (c.moveToNext()) all.add(FRow(android.content.ContentUris.withAppendedId(dl, c.getLong(0)), c.getString(1) ?: "file", c.getLong(2), c.getLong(3) * 1000, c.getString(4) ?: "*/*"))
            }
        }
        fun refresh() {
            shown.clear()
            shown.addAll(all.filter { r ->
                val ext = r.name.substringAfterLast('.', "").lowercase()
                val typeOk = when (filter) {
                    "All" -> true
                    "Videos" -> cats["Video"]!!.contains(ext)
                    "Audio" -> cats["Audio"]!!.contains(ext)
                    "Images" -> cats["Images"]!!.contains(ext)
                    "Docs" -> cats["Documents"]!!.contains(ext)
                    else -> cats.values.none { it.contains(ext) } || cats["Archives/Apps"]!!.contains(ext)
                }
                typeOk && (q.isEmpty() || r.name.contains(q, true))
            })
            when (sort) { 0 -> shown.sortByDescending { it.date }; 1 -> shown.sortBy { it.name.lowercase() }; else -> shown.sortByDescending { it.size } }
            head.text = "\uD83D\uDCC1  My files  \u00B7  ${shown.size}"
            sub.text = Formatter.formatShortFileSize(this, shown.sumOf { it.size }) + " shown  \u00B7  " +
                Formatter.formatShortFileSize(this, android.os.Environment.getExternalStorageDirectory().freeSpace) + " free  \u00B7  sort: " + listOf("newest", "name", "size")[sort]
            (lv.adapter as BaseAdapter).notifyDataSetChanged()
        }
        fun styleChips() {
            for (i in 0 until chips.childCount) {
                val t = chips.getChildAt(i) as TextView; val on = t.text.toString() == filter
                t.background = rr(if (on) accC else pillC, 18); t.setTextColor(if (on) 0xFFFFFFFF.toInt() else inkC)
            }
        }
        for (n in listOf("All", "Videos", "Audio", "Images", "Docs", "Other")) chips.addView(TextView(this).apply {
            text = n; textSize = 13f; setPadding(dp(14), dp(8), dp(14), dp(8)); setOnClickListener { filter = n; styleChips(); refresh() }
        }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
        lv.adapter = object : BaseAdapter() {
            override fun getCount() = shown.size
            override fun getItem(p: Int) = shown[p]
            override fun getItemId(p: Int) = p.toLong()
            override fun getView(p: Int, v: View?, g: ViewGroup?): View {
                val r = shown[p]
                val row = (v as? LinearLayout) ?: LinearLayout(this@MainActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(10), dp(18), dp(10))
                    addView(TextView(context).apply { textSize = 24f; setPadding(0, 0, dp(14), 0) })
                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(TextView(context).apply { textSize = 15f; maxLines = 1; ellipsize = TextUtils.TruncateAt.MIDDLE; setTextColor(inkC) })
                        addView(TextView(context).apply { textSize = 12f; setTextColor(0xFF80868B.toInt()) })
                    }, LinearLayout.LayoutParams(0, -2, 1f))
                }
                val ext = r.name.substringAfterLast('.', "").lowercase()
                (row.getChildAt(0) as TextView).text = when {
                    ext == "pdf" -> "\uD83D\uDCC4"
                    cats["Video"]!!.contains(ext) -> "\uD83C\uDFAC"
                    cats["Audio"]!!.contains(ext) -> "\uD83C\uDFB5"
                    cats["Images"]!!.contains(ext) -> "\uD83D\uDDBC"
                    cats["Archives/Apps"]!!.contains(ext) -> "\uD83D\uDCE6"
                    else -> "\uD83D\uDCDD"
                }
                val col = row.getChildAt(1) as LinearLayout
                (col.getChildAt(0) as TextView).text = r.name
                (col.getChildAt(1) as TextView).text = Formatter.formatShortFileSize(this@MainActivity, r.size) + "  \u00B7  " + android.text.format.DateUtils.getRelativeTimeSpanString(r.date)
                return row
            }
        }
        lv.setOnItemClickListener { _, _, p, _ -> openFile(shown[p]) }
        lv.setOnItemLongClickListener { _, _, p, _ ->
            val r = shown[p]
            sheet(r.name.take(60), listOf<Pair<String, () -> Unit>>(
                "\u25B6  Open" to { openFile(r) },
                "\uD83D\uDCE4  Share" to { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(r.mime).putExtra(Intent.EXTRA_STREAM, r.uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Share").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) },
                "\u270F  Rename" to { renameFile(r) { load(); refresh() } },
                "\u2139  Details" to { AlertDialog.Builder(this).setTitle(r.name).setMessage("Size: " + Formatter.formatFileSize(this, r.size) + "\nType: " + r.mime + "\nSaved: " + java.util.Date(r.date) + "\nFolder: Downloads/AYB+").setPositiveButton("OK", null).show(); Unit },
                "\uD83D\uDDD1  Delete" to {
                    AlertDialog.Builder(this).setTitle("Delete this file?").setMessage(r.name).setPositiveButton("Delete") { _, _ ->
                        try { contentResolver.delete(r.uri, null, null) } catch (_: Exception) {}
                        downloads.removeAll { it.uri == r.uri }; load(); refresh()
                    }.setNegativeButton("Cancel", null).show(); Unit
                }))
            true
        }
        val sortBtn = TextView(this).apply { text = "\u21C5 Sort"; textSize = 13f; setTextColor(accC); setPadding(dp(12), dp(16), dp(10), 0); setOnClickListener { sort = (sort + 1) % 3; refresh() } }
        val closeBtn = TextView(this).apply { text = "\u2716"; textSize = 18f; setTextColor(inkC); setPadding(dp(10), dp(14), dp(18), 0); setOnClickListener { dlg.dismiss() } }
        val topRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        topRow.addView(head, LinearLayout.LayoutParams(0, -2, 1f)); topRow.addView(sortBtn); topRow.addView(closeBtn)
        val colV = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bgC) }
        colV.addView(topRow); colV.addView(sub)
        colV.addView(search, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), 0, dp(16), dp(8)) })
        colV.addView(HorizontalScrollView(this).apply { addView(chips); isHorizontalScrollBarEnabled = false; setPadding(dp(16), 0, dp(8), dp(8)) })
        colV.addView(lv, LinearLayout.LayoutParams(-1, 0, 1f))
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { q = s?.toString() ?: ""; refresh() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        dlg.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dlg.setContentView(colV)
        load(); styleChips(); refresh()
        dlg.show(); dlg.window?.setLayout(-1, -1)
    }

    // ---------- clean links: skip "you are leaving this site" wrappers, strip trackers, unshorten ----------
    private val trackParams = setOf("fbclid", "gclid", "dclid", "msclkid", "igshid", "mc_eid", "mc_cid", "yclid", "_hsenc", "_hsmi", "vero_id", "twclid", "ttclid")

    private fun clean(u: String): String {
        return try {
            val x = Uri.parse(u); val keys = x.queryParameterNames
            if (keys.none { it in trackParams || it.startsWith("utm_") }) u
            else {
                val b = x.buildUpon().clearQuery()
                for (k in keys) if (k !in trackParams && !k.startsWith("utm_")) x.getQueryParameters(k).forEach { b.appendQueryParameter(k, it) }
                b.build().toString()
            }
        } catch (e: Exception) { u }
    }

    // real destination hidden inside the redirect wrappers Google, Facebook, YouTube, Reddit, LinkedIn, Steam, Instagram and Slack use
    private fun unwrap(u: String): String? {
        val x = Uri.parse(u); val h = x.host ?: return null; val p = x.path ?: ""
        val key = when {
            h.endsWith("google.com") && p == "/url" -> if (x.getQueryParameter("q") != null) "q" else "url"
            h == "l.facebook.com" || h == "lm.facebook.com" || h == "l.instagram.com" -> "u"
            h.endsWith("youtube.com") && p == "/redirect" -> "q"
            h == "out.reddit.com" -> "url"
            h.endsWith("linkedin.com") && p.startsWith("/redir/") -> "url"
            h == "steamcommunity.com" && p.startsWith("/linkfilter") -> if (x.getQueryParameter("u") != null) "u" else "url"
            h == "slack-redir.net" -> "url"
            h.endsWith("google.com") && p.startsWith("/amp/s/") -> return "https://" + p.removePrefix("/amp/s/")
            else -> return null
        }
        return x.getQueryParameter(key)?.takeIf { it.startsWith("http") }
    }

    // follows a short link hop by hop (bit.ly, t.co, tinyurl...) and shows every step before you open it
    private fun resolveChain(u0: String, ua: String, done: (List<String>) -> Unit) {
        Thread {
            val chain = arrayListOf(u0); var cur = u0
            try {
                for (i in 0 until 10) {
                    val c = java.net.URL(cur).openConnection() as java.net.HttpURLConnection
                    c.instanceFollowRedirects = false; c.connectTimeout = 10000; c.readTimeout = 10000; c.setRequestProperty("User-Agent", ua)
                    val code = c.responseCode; val loc = c.getHeaderField("Location"); c.disconnect()
                    if (code in 300..399 && loc != null) { cur = java.net.URL(java.net.URL(cur), loc).toString(); chain.add(clean(unwrap(cur) ?: cur)); cur = chain.last() } else break
                }
            } catch (e: Exception) { chain.add("(stopped: " + (e.message ?: "error") + ")") }
            runOnUiThread { done(chain) }
        }.start()
    }

    private fun unshortenDialog(prefill: String) {
        val et = EditText(this).apply { hint = "Paste a short or wrapped link"; setText(prefill) }
        AlertDialog.Builder(this).setTitle("\uD83D\uDD17 Unshorten link").setView(et).setPositiveButton("Resolve") { _, _ ->
            val ua = web.settings.userAgentString
            Toast.makeText(this, "Following redirects...", Toast.LENGTH_SHORT).show()
            resolveChain(et.text.toString().trim(), ua) { chain ->
                val real = chain.lastOrNull { it.startsWith("http") } ?: chain.last()
                AlertDialog.Builder(this).setTitle("Real destination").setMessage(chain.joinToString("\n\u2193\n"))
                    .setPositiveButton("Open") { _, _ -> newTab(real) }
                    .setNeutralButton("Copy") { _, _ -> (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("link", real)) }
                    .setNegativeButton("Close", null).show()
            }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun findDialog() {
        val et = EditText(this).apply { hint = "Find in page"; isSingleLine = true }
        AlertDialog.Builder(this).setTitle("Find in page").setView(et)
            .setPositiveButton("Find") { _, _ -> web.findAllAsync(et.text.toString()) }
            .setNeutralButton("Clear") { _, _ -> web.clearMatches() }.show()
    }

    private fun bookmarks() {
        val l = loadBm()
        if (l.isEmpty()) { Toast.makeText(this, "No bookmarks yet", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle("Bookmarks").setItems(l.map { it[0].take(40) }.toTypedArray()) { _, k ->
            AlertDialog.Builder(this).setItems(arrayOf("Open", "Open in new tab", "Delete")) { _, j ->
                when (j) {
                    0 -> web.loadUrl(l[k][1])
                    1 -> newTab(l[k][1])
                    else -> { l.removeAt(k); saveBm(l) }
                }
                Unit
            }.show()
        }.show()
    }

    private fun schedule() {
        val sp = getSharedPreferences("gp", 0)
        AlertDialog.Builder(this).setTitle("Start new downloads at a set time").setItems(arrayOf("Pick a time...", "Turn schedule off")) { _, i ->
            if (i == 1) { sp.edit().putLong("sched", 0L).apply(); Toast.makeText(this, "Schedule off", Toast.LENGTH_SHORT).show() }
            else android.app.TimePickerDialog(this, { _, h, m ->
                val c = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, h); set(java.util.Calendar.MINUTE, m); set(java.util.Calendar.SECOND, 0) }
                if (c.timeInMillis < System.currentTimeMillis()) c.add(java.util.Calendar.DAY_OF_YEAR, 1)
                sp.edit().putLong("sched", c.timeInMillis).apply()
                Toast.makeText(this, "New downloads will start at %02d:%02d".format(h, m), Toast.LENGTH_LONG).show()
            }, 2, 0, true).show()
        }.show()
    }

    override fun onPause() { super.onPause(); Engine.save(this) }

    private fun sheet(title: String, rows: List<Pair<String, () -> Unit>>) {
        val dlg = android.app.Dialog(this)
        dlg.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val r = dp(28).toFloat()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(14), dp(20), dp(20))
            background = GradientDrawable().apply { setColor(bgC); cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f) }
        }
        col.addView(View(this).apply { background = rr(0xFF80868B.toInt(), 3) }, LinearLayout.LayoutParams(dp(40), dp(5)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(12) })
        col.addView(TextView(this).apply { text = title; textSize = 17f; setTextColor(inkC); setTypeface(null, Typeface.BOLD); maxLines = 2; setPadding(0, 0, 0, dp(6)) })
        val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for ((label, act) in rows) inner.addView(TextView(this).apply {
            text = label; textSize = 15.5f; setTextColor(inkC); setPadding(dp(16), dp(14), dp(16), dp(14)); background = rr(pillC, 14)
            setOnClickListener { dlg.dismiss(); act() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        col.addView(ScrollView(this).apply { addView(inner) }, LinearLayout.LayoutParams(-1, -2))
        dlg.setContentView(col)
        dlg.window?.apply { setLayout(-1, -2); setGravity(Gravity.BOTTOM); setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0)); setDimAmount(0.5f) }
        dlg.show()
    }

    // the built-in video/audio downloader (YouTube and 1000+ sites) - no separate tool, it lives behind the floating button
    private fun ytDialog(u: String, title: String) {
        val modes = listOf("best", "1080", "720", "480", "mp3", "L1080", "Lmp3")
        val names = listOf("\uD83C\uDFAC  Best quality \u00B7 MP4", "\u2728  1080p Full HD", "\u25B6  720p HD", "\uD83D\uDCF1  480p", "\uD83C\uDFB5  Audio only \u00B7 MP3",
            "\uD83D\uDCDA  Whole playlist \u00B7 1080p", "\uD83C\uDFA7  Whole playlist \u00B7 MP3")
        val rows = mutableListOf<Pair<String, () -> Unit>>()
        names.forEachIndexed { i, n ->
            rows.add(n to { val d = Item(u, title, u, "", CookieManager.getInstance().getCookie(u) ?: "", modes[i]); downloads.add(d); Engine.start(this, d); showList(true) })
        }
        rows.add("\uD83D\uDCCB  Paste many links \u00B7 1080p" to { batch("1080") })
        rows.add("\uD83D\uDCCB  Paste many links \u00B7 MP3" to { batch("mp3") })
        if (found.isNotEmpty() && u == web.url) rows.add("\uD83D\uDCC1  Other files on this page (${found.size})" to { grabDialog("all") })
        rows.add("\uD83D\uDD04  Update video engine" to { updateEngine() })
        sheet(title.take(70), rows)
    }

    private fun updateEngine() {
        Toast.makeText(this, "Updating video engine...", Toast.LENGTH_SHORT).show()
        Thread {
            val ok = try { YoutubeDL.getInstance().updateYoutubeDL(this); true } catch (e: Exception) { false }
            runOnUiThread { Toast.makeText(this, if (ok) "Engine updated" else "Update failed", Toast.LENGTH_SHORT).show() }
        }.start()
    }

    private fun batch(mode: String) {
        val et = EditText(this).apply { hint = "One link per line"; minLines = 5; gravity = android.view.Gravity.TOP }
        AlertDialog.Builder(this).setTitle("Paste links").setView(et).setPositiveButton("Download") { _, _ ->
            Regex("https?://\\S+").findAll(et.text).forEach { m ->
                val d = Item(m.value, m.value, m.value, "", CookieManager.getInstance().getCookie(m.value) ?: "", mode)
                downloads.add(d); Engine.start(this, d)
            }
            showList(true)
        }.show()
    }

    private fun settings() {
        val sp = getSharedPreferences("gp", 0)
        val cur = BooleanArray(prefKeys.size) { sp.getBoolean(prefKeys[it], prefDefs[it]) }
        AlertDialog.Builder(this).setTitle("Settings").setMultiChoiceItems(prefLabels, cur) { _, i, c -> cur[i] = c }
            .setNeutralButton("Schedule") { _, _ -> schedule() }.setPositiveButton("Save") { _, _ ->
                val e = sp.edit(); prefKeys.forEachIndexed { i, k -> e.putBoolean(k, cur[i]) }; e.apply()
                adOn = cur[5]; applyDesktop(true)
            }.show()
    }

    private fun applyDesktop(reload: Boolean) {
        val sp = getSharedPreferences("gp", 0)
        val d = sp.getBoolean("desktop", false); val dk = sp.getBoolean("dark", true)
        for (t in tabs) {
            t.settings.userAgentString = if (d) "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36" else null
            t.settings.useWideViewPort = d; t.settings.loadWithOverviewMode = d
            if (android.os.Build.VERSION.SDK_INT >= 33) t.settings.isAlgorithmicDarkeningAllowed = dk
            else t.settings.forceDark = if (dk) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
        }
        if (reload) web.reload()
    }

    // offers to download a link you just copied (clipboard is only readable while the app has focus)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        if (pendingSearch) { pendingSearch = false; bar.requestFocus(); (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showSoftInput(bar, 0); return }
        if (pendingPaste) {
            pendingPaste = false
            val c = (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString() ?: ""
            val u = Regex("https?://\\S+").find(c)?.value
            if (u != null) { lastClip = u; ytDialog(u, "Video") } else Toast.makeText(this, "Copy a link first, then tap again", Toast.LENGTH_LONG).show()
            return
        }
        val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val t = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString() ?: return
        val u = Regex("https?://\\S+").find(t)?.value ?: return
        if (u == lastClip || u == web.url || downloads.any { it.url == u }) return
        lastClip = u
        AlertDialog.Builder(this).setTitle("Link in clipboard").setMessage(u.take(120))
            .setPositiveButton("Download") { _, _ -> ytDialog(u, "video") }.setNegativeButton("Ignore", null).show()
    }

    private fun showList(show: Boolean) { list.visibility = if (show) View.VISIBLE else View.GONE; web.visibility = if (show) View.GONE else View.VISIBLE; if (show) grab.visibility = View.GONE else badge(found.size) }

    @Deprecated("fine")
    override fun onBackPressed() {
        when { list.visibility == View.VISIBLE -> showList(false); web.canGoBack() -> web.goBack(); else -> super.onBackPressed() }
    }
}
