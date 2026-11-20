package com.tim.eyecomfort

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager

class OverlayService : Service() {

    private var overlay: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Prefs.setEnabled(this, false)
            removeOverlay()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForeground()
        applyOverlay()
        return START_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Фильтр экрана", NotificationManager.IMPORTANCE_MIN)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Фильтр экрана включён")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Выключить", stop).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    private fun applyOverlay() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = overlay ?: View(this).also {
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            wm.addView(it, lp)
            overlay = it
        }
        view.setBackgroundColor(computeColor(Prefs.warmth(this), Prefs.dim(this)))
    }

    // Тёплый слой (оранжевый) + чёрный слой сверху, склеены в один цвет.
    private fun computeColor(warmth: Int, dim: Int): Int {
        val a1 = warmth / 100f * 0.55f   // макс. 55% тёплого оттенка
        val a2 = dim / 100f * 0.70f      // макс. 70% затемнения
        val outA = a2 + a1 * (1 - a2)
        if (outA <= 0f) return Color.TRANSPARENT
        val k = a1 * (1 - a2) / outA
        return Color.argb(
            (outA * 255).toInt(),
            (255 * k).toInt(),
            (110 * k).toInt(),
            0
        )
    }

    private fun removeOverlay() {
        overlay?.let {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(it)
        }
        overlay = null
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "filter"
        const val ACTION_STOP = "com.tim.eyecomfort.STOP"
    }
}
