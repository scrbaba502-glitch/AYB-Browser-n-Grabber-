package com.grabpro

import android.app.Application
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class App : Application() {
    companion object { @Volatile var ready = false; @Volatile var initError: String? = null }
    override fun onCreate() {
        super.onCreate()
        AdBlock.init(this)
        Thread {
            try { YoutubeDL.getInstance().init(this); FFmpeg.getInstance().init(this); ready = true
                val sp = getSharedPreferences("gp", 0)
                if (System.currentTimeMillis() - sp.getLong("upd", 0L) > 3 * 86400000L) { YoutubeDL.getInstance().updateYoutubeDL(this); sp.edit().putLong("upd", System.currentTimeMillis()).apply() }
            }
            catch (e: Exception) { initError = e.message ?: "init failed" }
        }.start()
    }
}
