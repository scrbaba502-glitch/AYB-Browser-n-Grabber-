package com.grabpro

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*

/** Foreground service: keeps the process alive and shows live progress while downloads run. */
class Svc : Service() {
    private val h = Handler(Looper.getMainLooper())
    private var wl: PowerManager.WakeLock? = null
    override fun onBind(i: Intent?): IBinder? = null

    private fun note(text: String, pct: Int): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, "dl").setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("AYB+")
            .setContentText(text).setProgress(100, maxOf(pct, 0), pct < 0).setOngoing(true).setContentIntent(pi).build()
    }

    private val was = HashSet<Item>()
    private fun doneNote(d: Item) {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        getSystemService(NotificationManager::class.java).notify(d.hashCode(), Notification.Builder(this, "dl2")
            .setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle("Download complete").setContentText(d.name).setAutoCancel(true).setContentIntent(pi).build())
    }

    private val tick = object : Runnable {
        override fun run() {
            Engine.all.forEach { if (it.status == "Downloading") was.add(it) else if (it.status == "Done" && was.remove(it)) doneNote(it) }
            val now = System.currentTimeMillis()
            Engine.all.filter { it.status == "Scheduled" && it.startAt in 1..now }.forEach { it.startAt = -1; Engine.start(this@Svc, it) }
            val act = Engine.all.filter { it.status == "Downloading" || it.status == "Queued" || it.status == "Scheduled" }
            if (act.isEmpty()) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return }
            val busy = act.filter { it.status == "Downloading" }
            if (busy.isNotEmpty() && wl?.isHeld != true) wl?.acquire(6 * 3600_000L) else if (busy.isEmpty() && wl?.isHeld == true) wl?.release()
            val pcs = busy.map { it.pct() }.filter { it >= 0 }
            val text = if (busy.isEmpty()) "${act.size} scheduled" else "${busy.size} active - ${busy.first().name}"
            getSystemService(NotificationManager::class.java).notify(1, note(text, if (pcs.isEmpty()) -1 else pcs.average().toInt()))
            h.postDelayed(this, 1000)
        }
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("dl", "Downloads", NotificationManager.IMPORTANCE_LOW))
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("dl2", "Finished downloads", NotificationManager.IMPORTANCE_DEFAULT))
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, note("Starting...", -1), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(1, note("Starting...", -1))
        if (wl == null) wl = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "grabpro:dl")
        h.removeCallbacksAndMessages(null); h.post(tick)
        return START_NOT_STICKY
    }

    override fun onDestroy() { h.removeCallbacksAndMessages(null); wl?.let { if (it.isHeld) it.release() }; super.onDestroy() }
}
