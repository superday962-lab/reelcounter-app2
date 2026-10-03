package com.reelcounter

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReelAccessibilityService : AccessibilityService() {

    companion object {
        const val IG = "com.instagram.android"
        const val PREFS = "reel_prefs"
        const val KEY_COUNT = "count"
        const val KEY_STRICT = "strict"
        const val KEY_TODAY_COUNT = "today_count"
        const val KEY_LAST_DATE = "last_date"
        const val KEY_LIMIT = "daily_limit"
        const val KEY_LIMIT_NOTIFIED_DATE = "limit_notified_date"
        const val KEY_THEME = "theme"
        const val CHANNEL_ID = "reel_limit_channel"

        fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        fun dayCountKey(date: String) = "day_$date"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private var wm: WindowManager? = null
    private var overlay: TextView? = null
    private var lastCountTime = 0L
    private val settled = Runnable { onScrollSettled() }

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        resetDailyIfNeeded()
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val pkg = e.packageName?.toString() ?: return
        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (pkg == IG) showOverlay()
                else if (pkg != "com.android.systemui" && !pkg.contains("inputmethod")) hideOverlay()
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (pkg == IG) {
                    showOverlay()
                    if (isReelsScreen()) {
                        handler.removeCallbacks(settled)
                        handler.postDelayed(settled, 500)
                    }
                }
            }
        }
    }

    private fun onScrollSettled() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastCountTime < 700) return
        lastCountTime = now

        resetDailyIfNeeded()

        val total = prefs.getInt(KEY_COUNT, 0) + 1
        val today = prefs.getInt(KEY_TODAY_COUNT, 0) + 1
        val dKey = dayCountKey(todayKey())
        val dayTotal = prefs.getInt(dKey, 0) + 1

        prefs.edit()
            .putInt(KEY_COUNT, total)
            .putInt(KEY_TODAY_COUNT, today)
            .putInt(dKey, dayTotal)
            .apply()

        overlay?.text = label()
        updateWidget()
        checkLimit(today)
    }

    private fun resetDailyIfNeeded() {
        val last = prefs.getString(KEY_LAST_DATE, "")
        val today = todayKey()
        if (last != today) {
            prefs.edit()
                .putString(KEY_LAST_DATE, today)
                .putInt(KEY_TODAY_COUNT, 0)
                .apply()
        }
    }

    private fun checkLimit(todayCount: Int) {
        val limit = prefs.getInt(KEY_LIMIT, 0)
        if (limit <= 0) return
        if (todayCount < limit) return
        val notifiedDate = prefs.getString(KEY_LIMIT_NOTIFIED_DATE, "")
        val today = todayKey()
        if (notifiedDate == today) return
        prefs.edit().putString(KEY_LIMIT_NOTIFIED_DATE, today).apply()
        sendLimitNotification(todayCount, limit)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID, "Reel limit alerts", NotificationManager.IMPORTANCE_DEFAULT
            )
            mgr?.createNotificationChannel(channel)
        }
    }

    private fun sendLimitNotification(count: Int, limit: Int) {
        try {
            val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("⏰ Reel limit pura ho gaya!")
                .setContentText("Aaj $count reels dekh li (limit: $limit). Thoda break le lo 😊")
                .setAutoCancel(true)
                .build()
            val mgr = getSystemService(NotificationManager::class.java)
            mgr?.notify(1001, notif)
        } catch (_: Exception) {}
    }

    private fun updateWidget() {
        try {
            val mgr = AppWidgetManager.getInstance(this)
            val ids = mgr.getAppWidgetIds(ComponentName(this, ReelCounterWidget::class.java))
            if (ids.isNotEmpty()) {
                val intent = Intent(this, ReelCounterWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                sendBroadcast(intent)
            }
        } catch (_: Exception) {}
    }

    private fun isReelsScreen(): Boolean {
        if (!prefs.getBoolean(KEY_STRICT, true)) return true
        return hasClipsView(rootInActiveWindow, 0)
    }

    private fun hasClipsView(n: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (n == null || depth > 12) return false
        val id = n.viewIdResourceName
        if (id != null && id.contains("clips_viewer")) return true
        for (i in 0 until n.childCount) {
            if (hasClipsView(n.getChild(i), depth + 1)) return true
        }
        return false
    }

    private fun label() = "🎬 " + prefs.getInt(KEY_COUNT, 0)

    private fun showOverlay() {
        if (overlay != null) return
        val tv = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(40, 16, 40, 16)
            background = GradientDrawable().apply {
                cornerRadius = 60f
                setColor(0xCC000000.toInt())
            }
            text = label()
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }
        try { wm?.addView(tv, lp); overlay = tv } catch (_: Exception) {}
    }

    private fun hideOverlay() {
        overlay?.let { try { wm?.removeView(it) } catch (_: Exception) {} }
        overlay = null
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        hideOverlay()
        return super.onUnbind(intent)
    }
}
