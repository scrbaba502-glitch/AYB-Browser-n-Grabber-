package com.grabpro

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Home-screen widget: tap the search pill to type, or tap Paste to download the link you just copied. */
class Widget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, m: AppWidgetManager, ids: IntArray) {
        fun pi(a: String, rc: Int) = PendingIntent.getActivity(ctx, rc, Intent(ctx, MainActivity::class.java).setAction(a)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget)
            rv.setOnClickPendingIntent(R.id.w_search, pi("com.a2.ayb.SEARCH", 1))
            rv.setOnClickPendingIntent(R.id.w_paste, pi("com.a2.ayb.PASTE", 2))
            m.updateAppWidget(id, rv)
        }
    }
}
