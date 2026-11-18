package com.grabpro

import android.content.Context
import java.io.File
import java.net.URL

/** Host-based ad/tracker/malware blocker. Cached lists are loaded away from the UI thread. */
object AdBlock {
    @Volatile private var hosts: HashSet<String> = hashSetOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
        "popads.net", "propellerads.com", "adsterra.com", "exoclick.com", "taboola.com",
        "outbrain.com", "adnxs.com", "criteo.com", "scorecardresearch.com"
    )

    private val lists = listOf(
        "hosts.txt" to "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
        "hosts2.txt" to "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/hosts/pro.txt"
    )

    fun init(ctx: Context) {
        val appContext = ctx.applicationContext
        Thread({
            for ((name, link) in lists) {
                val file = File(appContext.filesDir, name)
                if (file.isFile) {
                    try { load(file) } catch (_: Exception) { }
                }
                if (!file.isFile || System.currentTimeMillis() - file.lastModified() > 7 * 86400000L) {
                    try {
                        val tmp = File(appContext.filesDir, "$name.tmp")
                        URL(link).openStream().use { input ->
                            tmp.outputStream().use { output -> input.copyTo(output) }
                        }
                        if (tmp.isFile && tmp.length() > 0L) {
                            if (file.exists()) file.delete()
                            if (tmp.renameTo(file)) load(file) else tmp.delete()
                        } else tmp.delete()
                    } catch (_: Exception) {
                        File(appContext.filesDir, "$name.tmp").delete()
                    }
                }
            }
        }, "AYB-AdBlock-Init").apply { isDaemon = true; start() }
    }

    @Synchronized private fun load(file: File) {
        val next = HashSet(hosts)
        file.forEachLine { line ->
            val value = line.trim()
            if (value.startsWith("0.0.0.0 ") || value.startsWith("127.0.0.1 ")) {
                val host = value.substringAfter(' ').substringBefore('#').trim().lowercase()
                if (host.contains('.') && host != "0.0.0.0") next.add(host)
            }
        }
        hosts = next
    }

    fun blocked(host: String?): Boolean {
        var current = host?.lowercase() ?: return false
        val snapshot = hosts
        while (true) {
            if (snapshot.contains(current)) return true
            val dot = current.indexOf('.')
            if (dot < 0 || current.indexOf('.', dot + 1) < 0) return false
            current = current.substring(dot + 1)
        }
    }
}
