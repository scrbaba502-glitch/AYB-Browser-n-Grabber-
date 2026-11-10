package com.grabpro

import android.content.Context
import java.io.File
import java.net.URL

/** Host-based ad/tracker/malware blocker. Downloads a public hosts list (refreshed weekly) and checks every sub-request against it. */
object AdBlock {
    @Volatile private var hosts: HashSet<String> = hashSetOf("doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
        "popads.net", "propellerads.com", "adsterra.com", "exoclick.com", "taboola.com", "outbrain.com", "adnxs.com", "criteo.com", "scorecardresearch.com")

    private val lists = listOf("hosts.txt" to "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
        "hosts2.txt" to "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/hosts/pro.txt")

    fun init(ctx: Context) {
        for ((name, link) in lists) {
            val f = File(ctx.filesDir, name)
            if (f.exists()) load(f)
            if (!f.exists() || System.currentTimeMillis() - f.lastModified() > 7 * 86400000L) Thread {
                try {
                    val tmp = File(ctx.filesDir, "$name.tmp")
                    URL(link).openStream().use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                    tmp.renameTo(f); load(f)
                } catch (_: Exception) {}
            }.start()
        }
    }

    @Synchronized private fun load(f: File) {
        val s = HashSet<String>(hosts)
        f.forEachLine { l ->
            val t = l.trim()
            if (t.startsWith("0.0.0.0 ") || t.startsWith("127.0.0.1 ")) {
                val h = t.substringAfter(' ').substringBefore('#').trim()
                if (h.contains('.') && h != "0.0.0.0") s.add(h)
            }
        }
        hosts = s
    }

    fun blocked(host: String?): Boolean {
        var h = host ?: return false
        val s = hosts
        while (true) {
            if (s.contains(h)) return true
            val i = h.indexOf('.')
            if (i < 0 || h.indexOf('.', i + 1) < 0) return false
            h = h.substring(i + 1)
        }
    }
}
