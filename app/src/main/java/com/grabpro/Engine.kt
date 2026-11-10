package com.grabpro

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class Item(val url: String, val name: String, val referer: String, val ua: String, val cookie: String, val ytmode: String = "") {
    @Volatile var status = "Queued"
    @Volatile var paused = false
    @Volatile var total = 0L
    @Volatile var segDone = 0
    @Volatile var segs = 0
    var ranged = false
    var uri: Uri? = null
    var starts = LongArray(0); var ends = LongArray(0); var got = LongArray(0)
    val done = AtomicLong()
    @Volatile var startAt = 0L
    @Volatile var spdText = ""
    @Volatile var bps = 0L
    private var lastBytes = 0L; private var lastT = 0L
    fun tickSpeed() {
        val n = System.currentTimeMillis()
        if (lastT == 0L) { lastT = n; lastBytes = done.get() }
        else if (n - lastT >= 1000) { bps = (done.get() - lastBytes) * 1000 / (n - lastT); lastBytes = done.get(); lastT = n }
    }
    fun pct(): Int = if (segs > 0) segDone * 100 / segs else if (total > 0) (done.get() * 100 / total).toInt() else -1
}

object Engine {
    val all = ArrayList<Item>()
    fun pref(ctx: Context, k: String, def: Boolean = false) = ctx.getSharedPreferences("gp", 0).getBoolean(k, def)
    fun save(ctx: Context) {
        val a = org.json.JSONArray()
        for (d in all.takeLast(200)) a.put(org.json.JSONObject().put("u", d.url).put("n", d.name).put("r", d.referer).put("a", d.ua)
            .put("c", d.cookie).put("y", d.ytmode).put("s", d.status).put("m", d.uri?.toString() ?: "").put("d", d.done.get()).put("t", d.startAt))
        ctx.getSharedPreferences("gp", 0).edit().putString("hist", a.toString()).apply()
    }

    fun load(ctx: Context) {
        if (all.isNotEmpty()) return
        try {
            val a = org.json.JSONArray(ctx.getSharedPreferences("gp", 0).getString("hist", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val d = Item(o.getString("u"), o.getString("n"), o.getString("r"), o.getString("a"), o.getString("c"), o.getString("y"))
                val s = o.getString("s")
                d.status = if (s == "Done" || s == "Scheduled") s else "Paused"
                if (o.getString("m").isNotEmpty()) d.uri = Uri.parse(o.getString("m"))
                d.startAt = o.getLong("t")
                if (s == "Done") { d.done.set(o.getLong("d")); d.total = d.done.get(); d.segs = 100; d.segDone = 100 }
                all.add(d)
            }
        } catch (_: Exception) {}
    }

    private fun onWifi(ctx: Context): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        return cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
    private val pool = Executors.newCachedThreadPool()
    private val jobs = Executors.newFixedThreadPool(3) // 2 files at once, 8 connections each

    private fun conn(d: Item, u: String, range: String? = null): HttpURLConnection {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 20000; c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", d.ua)
        if (d.referer.isNotEmpty()) c.setRequestProperty("Referer", d.referer)
        if (d.cookie.isNotEmpty()) c.setRequestProperty("Cookie", d.cookie)
        if (range != null) c.setRequestProperty("Range", range)
        return c
    }

    fun create(ctx: Context, name: String): Uri {
        val ext = name.substringAfterLast('.', "").lowercase()
        val mime = if (ext == "ts") "video/mp2t" else MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        val cv = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name); put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/AYB+"); put(MediaStore.Downloads.IS_PENDING, 1)
        }
        return ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)!!
    }

    fun start(ctx: Context, d: Item) {
        if (d.status == "Downloading") return
        if (pref(ctx, "wifi") && !onWifi(ctx)) { d.status = "Error: waiting for Wi-Fi (tap to retry)"; return }
        val at = ctx.getSharedPreferences("gp", 0).getLong("sched", 0L)
        if (d.startAt == 0L && at > System.currentTimeMillis()) {
            d.startAt = at; d.status = "Scheduled"; ctx.startForegroundService(Intent(ctx, Svc::class.java)); return
        }
        d.paused = false; d.status = "Downloading"
        ctx.startForegroundService(Intent(ctx, Svc::class.java))
        jobs.execute {
            if (d.ytmode.isNotEmpty()) { runYt(ctx, d); return@execute }
            var pfd: ParcelFileDescriptor? = null
            try {
                if (d.uri == null) d.uri = create(ctx, d.name)
                pfd = ctx.contentResolver.openFileDescriptor(d.uri!!, "rw")!!
                val ch = FileOutputStream(pfd.fileDescriptor).channel
                if (d.url.substringBefore('?').endsWith(".m3u8")) hls(d, ch) else http(d, ch)
                if (d.paused) d.status = "Paused" else {
                    ctx.contentResolver.update(d.uri!!, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
                    d.status = "Done"
                }
            } catch (e: Exception) {
                d.status = if (d.paused) "Paused" else "Error: " + (e.cause?.message ?: e.message)
            } finally { try { pfd?.close() } catch (_: Exception) {} }
        }
    }

    fun pause(d: Item) {
        d.paused = true
        if (d.ytmode.isNotEmpty()) try { YoutubeDL.getInstance().destroyProcessById(d.hashCode().toString()) } catch (_: Exception) {}
    }

    // ---- yt-dlp (YouTube + 1000+ sites), on-device; playlists, cookies, subtitles, SponsorBlock ----
    private fun runYt(ctx: Context, d: Item) {
        val dir = File(ctx.cacheDir, "yt/" + d.hashCode()).apply { mkdirs() }
        try {
            while (!App.ready) { App.initError?.let { throw IOException("yt-dlp failed to start: $it") }; Thread.sleep(300) }
            val pl = d.ytmode.startsWith("L"); val m = d.ytmode.removePrefix("L")
            val r = YoutubeDLRequest(d.url)
            r.addOption("-o", dir.absolutePath + (if (pl) "/%(playlist_index)03d - %(title).90B.%(ext)s" else "/%(title).100B.%(ext)s"))
            r.addOption(if (pl) "--yes-playlist" else "--no-playlist")
            if (pl) r.addOption("--ignore-errors")
            r.addOption("--no-mtime"); r.addOption("--continue"); r.addOption("--concurrent-fragments", "16")
            if (d.referer.isNotBlank() && d.referer != d.url) r.addOption("--referer", d.referer)
            if (d.ua.isNotBlank()) r.addOption("--user-agent", d.ua)
            r.addOption("--retries", "10"); r.addOption("--fragment-retries", "10")
            val meta = pref(ctx, "meta", true)
            if (meta) r.addOption("--embed-metadata")
            if (pref(ctx, "sb")) r.addOption("--sponsorblock-remove", "sponsor,selfpromo,interaction")
            if (d.cookie.isNotBlank()) { // log-in state from the in-app browser (private / members / age-gated content you can access)
                val host = URL(d.url).host.removePrefix("www.")
                val cf = File(dir, "cookies.txt")
                cf.writeText("# Netscape HTTP Cookie File\n" + d.cookie.split(";").mapNotNull { c ->
                    val kv = c.trim().split("=", limit = 2)
                    if (kv.size == 2) ".$host\tTRUE\t/\tTRUE\t2147483647\t${kv[0]}\t${kv[1]}" else null
                }.joinToString("\n"))
                r.addOption("--cookies", cf.absolutePath)
            }
            if (m == "mp3") {
                r.addOption("-x"); r.addOption("--audio-format", "mp3"); r.addOption("--audio-quality", "0")
                if (meta) r.addOption("--embed-thumbnail")
            } else {
                val h = if (m == "best") 4320 else m.toInt()
                r.addOption("-f", "bv*+ba/b"); r.addOption("-S", "res:$h,vcodec:h264,acodec:m4a"); r.addOption("--merge-output-format", "mp4")
                if (pref(ctx, "subs")) { r.addOption("--write-subs"); r.addOption("--write-auto-subs"); r.addOption("--sub-langs", "en.*"); r.addOption("--embed-subs") }
            }
            d.segs = 100
            YoutubeDL.getInstance().execute(r, d.hashCode().toString()) { p, _, line -> d.segDone = p.toInt(); Regex("at\\s+(\\S+/s)").find(line ?: "")?.let { d.spdText = it.groupValues[1] } }
            val skip = listOf(".part", ".ytdl", ".vtt", ".webp", ".jpg", ".temp")
            val files = dir.listFiles()!!.filter { f -> f.name != "cookies.txt" && skip.none { f.name.endsWith(it) } }.sortedBy { it.name }
            if (files.isEmpty()) throw IOException("Nothing was downloaded")
            var tot = 0L
            for (f in files) {
                val uri = create(ctx, f.name); d.uri = uri
                ctx.contentResolver.openOutputStream(uri)!!.use { o -> f.inputStream().use { it.copyTo(o) } }
                ctx.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
                tot += f.length()
            }
            d.done.set(tot); d.segDone = 100; d.status = "Done"; dir.deleteRecursively()
        } catch (e: Exception) {
            d.status = if (d.paused) "Paused" else "Error: " + (e.message ?: "failed").trim().lines().last().take(160)
        }
    }

    // ---- normal files: 8 parallel ranged connections, resumable ----
    private fun http(d: Item, ch: FileChannel) {
        if (d.starts.isEmpty()) {
            val c = conn(d, d.url, "bytes=0-0")
            val code = c.responseCode
            if (code >= 400) throw IOException("HTTP $code")
            val cr = c.getHeaderField("Content-Range")
            d.total = if (code == 206 && cr != null) cr.substringAfter('/').toLongOrNull() ?: 0L else c.contentLengthLong
            d.ranged = code == 206 && d.total > 0
            c.disconnect()
            val n = if (d.ranged && d.total > (1 shl 20)) 16 else 1
            val sz = d.total / n
            d.starts = LongArray(n) { it * sz }
            d.ends = LongArray(n) { if (it == n - 1) d.total - 1 else (it + 1) * sz - 1 }
            d.got = LongArray(n)
        }
        (0 until d.starts.size).map { i -> pool.submit(Runnable { part(d, ch, i) }) }.forEach { it.get() }
    }

    private fun part(d: Item, ch: FileChannel, i: Int) {
        var tries = 0
        while (!d.paused && !(d.ranged && d.starts[i] + d.got[i] > d.ends[i])) {
            try {
                val s = d.starts[i] + d.got[i]
                val c = conn(d, d.url, if (d.ranged) "bytes=$s-${d.ends[i]}" else null)
                if (d.ranged && c.responseCode != 206) throw IOException("Server ignored range")
                var fin = false
                c.inputStream.use { inp ->
                    val buf = ByteArray(262144)
                    while (!d.paused) {
                        val n = inp.read(buf)
                        if (n < 0) { fin = true; break }
                        ch.write(ByteBuffer.wrap(buf, 0, n), d.starts[i] + d.got[i])
                        d.got[i] += n.toLong(); d.done.addAndGet(n.toLong())
                    }
                }
                if (fin && !d.ranged) return
                if (fin) tries++
            } catch (e: IOException) { tries++; if (tries > 5) throw e }
            if (tries > 5) throw IOException("Stalled")
        }
    }

    // ---- HLS (.m3u8): picks best quality, fetches segments in parallel, writes a .ts file ----
    private fun fetch(d: Item, u: String) = conn(d, u).inputStream.bufferedReader().use { it.readText() }

    private fun bytes(d: Item, u: String): ByteArray {
        for (t in 0..3) { try { return conn(d, u).inputStream.use { it.readBytes() } } catch (e: IOException) { if (t == 3) throw e } }
        throw IOException("Segment failed")
    }

    fun getBytes(d: Item): ByteArray = bytes(d, d.url)

    private fun hls(d: Item, ch: FileChannel) {
        var url = d.url
        var text = fetch(d, url)
        if (text.contains("#EXT-X-STREAM-INF")) {
            val lines = text.lines(); var best = -1L; var pick = ""
            for (k in lines.indices) if (lines[k].startsWith("#EXT-X-STREAM-INF") && k + 1 < lines.size) {
                val bw = Regex("BANDWIDTH=(\\d+)").find(lines[k])?.groupValues?.get(1)?.toLong() ?: 0L
                if (bw > best) { best = bw; pick = lines[k + 1].trim() }
            }
            url = URL(URL(url), pick).toString(); text = fetch(d, url)
        }
        if (text.contains("METHOD=AES-128")) throw IOException("Encrypted HLS not supported")
        val segs = text.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.map { URL(URL(url), it).toString() }
        d.segs = segs.size
        var off = d.done.get()
        while (d.segDone < segs.size && !d.paused) {
            val batch = segs.subList(d.segDone, minOf(d.segDone + 6, segs.size))
            val fs = batch.map { u -> pool.submit<ByteArray> { bytes(d, u) } }
            for (f in fs) { val b = f.get(); ch.write(ByteBuffer.wrap(b), off); off += b.size; d.done.addAndGet(b.size.toLong()) }
            d.segDone += batch.size
        }
    }
}
