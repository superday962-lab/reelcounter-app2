package com.reelcounter

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    // ---- colors (filled in onCreate based on theme) ----
    private var cBg = Color.parseColor("#121212")
    private var cCard = Color.parseColor("#1E1E1E")
    private var cCardLight = Color.parseColor("#2A2A2A")
    private var cText = Color.WHITE
    private var cMuted = Color.parseColor("#B3B3B3")
    private val cOrange = Color.parseColor("#F58529")
    private val cPink = Color.parseColor("#DD2A7B")
    private val cPurple = Color.parseColor("#8134AF")
    private val cGreen = Color.parseColor("#3DDC84")
    private val cRed = Color.parseColor("#FF5A5F")

    private val GITHUB_OWNER = "superday962-lab"
    private val GITHUB_REPO = "reelcounter-app2"

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var prefs: android.content.SharedPreferences

    // top tab bar
    private lateinit var tabHomeBtn: Button
    private lateinit var tabStatsBtn: Button
    private lateinit var tabSettingsBtn: Button
    private lateinit var homeContainer: LinearLayout
    private lateinit var statsContainer: LinearLayout
    private lateinit var settingsContainer: LinearLayout

    // home tab views
    private lateinit var countTv: TextView
    private lateinit var statusTv: TextView
    private lateinit var statusDot: View
    private lateinit var permBtn: Button

    // stats tab views
    private lateinit var todayTv: TextView
    private lateinit var weekTv: TextView
    private lateinit var avgTv: TextView
    private lateinit var timeTv: TextView
    private lateinit var limitInput: EditText
    private lateinit var limitStatusTv: TextView

    // settings tab views
    private lateinit var updateBtn: Button
    private lateinit var versionTv: TextView
    private lateinit var themeSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(ReelAccessibilityService.PREFS, MODE_PRIVATE)
        applyThemeColors()

        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cBg)
        }

        // ---------- top tab bar ----------
        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(40), dp(12), dp(8))
        }
        tabHomeBtn = tabButton("🏠 Home")
        tabStatsBtn = tabButton("📊 Stats")
        tabSettingsBtn = tabButton("⚙️ Settings")
        tabHomeBtn.setOnClickListener { switchTab(0) }
        tabStatsBtn.setOnClickListener { switchTab(1) }
        tabSettingsBtn.setOnClickListener { switchTab(2) }
        tabBar.addView(tabHomeBtn)
        tabBar.addView(tabStatsBtn)
        tabBar.addView(tabSettingsBtn)

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(32))
        }

        homeContainer = buildHomeTab()
        statsContainer = buildStatsTab()
        settingsContainer = buildSettingsTab()

        root.addView(homeContainer)
        root.addView(statsContainer)
        root.addView(settingsContainer)

        scroll.addView(root)
        outer.addView(tabBar)
        outer.addView(scroll)
        setContentView(outer)

        switchTab(0)
        checkForUpdate(showNoUpdateMessage = false)
    }

    // ================= THEME =================

    private fun applyThemeColors() {
        val isLight = prefs.getString(ReelAccessibilityService.KEY_THEME, "dark") == "light"
        if (isLight) {
            cBg = Color.parseColor("#F5F5F5")
            cCard = Color.parseColor("#FFFFFF")
            cCardLight = Color.parseColor("#ECECEC")
            cText = Color.parseColor("#1A1A1A")
            cMuted = Color.parseColor("#666666")
        } else {
            cBg = Color.parseColor("#121212")
            cCard = Color.parseColor("#1E1E1E")
            cCardLight = Color.parseColor("#2A2A2A")
            cText = Color.WHITE
            cMuted = Color.parseColor("#B3B3B3")
        }
    }

    // ================= TAB BAR =================

    private fun tabButton(label: String): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 13f
            setTextColor(cMuted)
            background = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = lp
        }
    }

    private fun switchTab(index: Int) {
        homeContainer.visibility = if (index == 0) View.VISIBLE else View.GONE
        statsContainer.visibility = if (index == 1) View.VISIBLE else View.GONE
        settingsContainer.visibility = if (index == 2) View.VISIBLE else View.GONE

        tabHomeBtn.setTextColor(if (index == 0) cOrange else cMuted)
        tabStatsBtn.setTextColor(if (index == 1) cOrange else cMuted)
        tabSettingsBtn.setTextColor(if (index == 2) cOrange else cMuted)

        if (index == 1) refreshStats()
    }

    // ================= HOME TAB =================

    private fun buildHomeTab(): LinearLayout {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val title = TextView(this).apply {
            text = "🎬 Reel Counter"
            textSize = 26f
            setTextColor(cText)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(4))
        }
        val subtitle = TextView(this).apply {
            text = "Instagram Reels auto counter"
            textSize = 14f
            setTextColor(cMuted)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(28))
        }

        val countCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(cOrange, cPink, cPurple)
            ).apply { cornerRadius = dp(28).toFloat() }
            setPadding(dp(24), dp(32), dp(24), dp(32))
        }
        val countInner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(cCard); cornerRadius = dp(22).toFloat() }
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        countTv = TextView(this).apply {
            textSize = 64f
            setTextColor(cText)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val countLabel = TextView(this).apply {
            text = "REELS COUNTED (ALL TIME)"
            textSize = 11f
            setTextColor(cMuted)
            gravity = Gravity.CENTER
            letterSpacing = 0.1f
            setPadding(0, dp(4), 0, 0)
        }
        countInner.addView(countTv)
        countInner.addView(countLabel)
        countCard.addView(countInner)

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(24))
        }
        statusDot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply {
                gravity = Gravity.CENTER_VERTICAL; marginEnd = dp(8)
            }
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(cRed) }
        }
        statusTv = TextView(this).apply { textSize = 14f; setTextColor(cMuted) }
        statusRow.addView(statusDot)
        statusRow.addView(statusTv)

        permBtn = Button(this).apply {
            text = "🔓  Permission Allow karo"
            setTextColor(Color.WHITE)
            textSize = 15f
            isAllCaps = false
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(cOrange, cPink, cPurple)
            ).apply { cornerRadius = dp(16).toFloat() }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnClickListener { showPreWarningExplainer() }
        }

        val howToBtn = Button(this).apply {
            text = "❓  Permission kaise du? (steps dekho)"
            setTextColor(cText)
            textSize = 14f
            isAllCaps = false
            background = GradientDrawable().apply { setColor(cCardLight); cornerRadius = dp(16).toFloat() }
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(12)
            layoutParams = lp
            setOnClickListener { showHowTo() }
        }

        val resetBtn = Button(this).apply {
            text = "🔄  Count reset karo (sab kuch)"
            setTextColor(cRed)
            textSize = 14f
            isAllCaps = false
            background = GradientDrawable().apply { setColor(cCardLight); cornerRadius = dp(14).toFloat() }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(16)
            layoutParams = lp
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Reset karein?")
                    .setMessage("Total, aaj ka aur hafte ka sab count 0 ho jayega.")
                    .setPositiveButton("Haan, reset karo") { _, _ ->
                        val today = ReelAccessibilityService.todayKey()
                        prefs.edit()
                            .putInt(ReelAccessibilityService.KEY_COUNT, 0)
                            .putInt(ReelAccessibilityService.KEY_TODAY_COUNT, 0)
                            .putInt(ReelAccessibilityService.dayCountKey(today), 0)
                            .apply()
                        refresh()
                        refreshStats()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        container.addView(title)
        container.addView(subtitle)
        container.addView(countCard)
        container.addView(statusRow)
        container.addView(permBtn)
        container.addView(howToBtn)
        container.addView(resetBtn)
        return container
    }

    private fun showPreWarningExplainer() {
        val already = prefs.getBoolean("explainer_seen", false)
        if (already) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        val msg = """
            Agle screen pe Android ek laal warning dikhayega jaise:
            "This permission can put your personal and financial info at risk."

            🛡️ Ghabrao mat — ye warning Android HAR accessibility app ko dikhata hai, chahe app kuch bhi kare. Ye sirf ek general safety notice hai.

            Ye app sirf itna karti hai:
            • Check karti hai ki Instagram khula hai ya nahi
            • Scroll hone par ek number ginti hai

            ❌ Koi password, message, photo nahi padhti
            ❌ Internet pe koi data nahi bhejti
            ✅ Sab kuch sirf tumhare phone ke andar rehta hai

            Warning aane par 'Allow' / 'Turn on anyway' dabake aage badh jana.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("🛡️ Permission dene se pehle")
            .setMessage(msg)
            .setPositiveButton("Samajh gaya, aage badho") { _, _ ->
                prefs.edit().putBoolean("explainer_seen", true).apply()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun showHowTo() {
        val steps = """
            1️⃣  'Permission Allow karo' button dabao — Accessibility settings khulegi.

            2️⃣  Neeche scroll karke 'Downloaded apps' me 'Reel Counter' dhundo.

            3️⃣  Uspe tap karo aur switch ON karo. Warning aaye to 'Allow' / 'OK' dabao.

            4️⃣  Agar 'restricted setting' ka message aaye:
            Settings → Apps → Reel Counter → ⋮ (top right) → 'Allow restricted settings'.
            Phir wapas Accessibility me jaake switch ON karo.

            5️⃣  Is app me wapas aao. Status green ho jayega — matlab ready hai.

            6️⃣  Instagram Reels kholo aur scroll karo. Top pe count dikhega.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Permission kaise du?")
            .setMessage(steps)
            .setPositiveButton("Accessibility settings kholo") { _, _ -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            .setNegativeButton("Band karo", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        refresh()
        refreshStats()
    }

    private fun refresh() {
        countTv.text = prefs.getInt(ReelAccessibilityService.KEY_COUNT, 0).toString()
        val on = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.contains(packageName) == true
        if (on) {
            statusTv.text = "Chalu hai — Instagram Reels kholo"
            statusTv.setTextColor(cGreen)
            (statusDot.background as GradientDrawable).setColor(cGreen)
            permBtn.text = "✅  Permission already ON hai"
        } else {
            statusTv.text = "Permission abhi off hai"
            statusTv.setTextColor(cRed)
            (statusDot.background as GradientDrawable).setColor(cRed)
            permBtn.text = "🔓  Permission Allow karo"
        }
    }

    // ================= STATS TAB =================

    private fun buildStatsTab(): LinearLayout {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }

        val title = TextView(this).apply {
            text = "📊 Stats"
            textSize = 24f
            setTextColor(cText)
            setPadding(0, dp(8), 0, dp(20))
        }

        val statsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(cCard); cornerRadius = dp(18).toFloat() }
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        todayTv = statRow("Aaj", "0 reels")
        weekTv = statRow("Is hafte", "0 reels")
        avgTv = statRow("Daily average", "0 reels/din")
        timeTv = statRow("Andazan time (aaj)", "0 min")

        statsCard.addView(todayTv.tag as View)
        statsCard.addView(divider())
        statsCard.addView(weekTv.tag as View)
        statsCard.addView(divider())
        statsCard.addView(avgTv.tag as View)
        statsCard.addView(divider())
        statsCard.addView(timeTv.tag as View)

        val limitCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(cCard); cornerRadius = dp(18).toFloat() }
            setPadding(dp(18), dp(18), dp(18), dp(18))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(16)
            layoutParams = lp
        }
        val limitTitle = TextView(this).apply {
            text = "⏰ Daily Reel Limit"
            textSize = 15f
            setTextColor(cText)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val limitDesc = TextView(this).apply {
            text = "Limit set karo, poori hote hi notification milegi (0 = koi limit nahi)"
            textSize = 12f
            setTextColor(cMuted)
            setPadding(0, dp(4), 0, dp(12))
        }
        val limitRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        limitInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt(ReelAccessibilityService.KEY_LIMIT, 0).toString())
            setTextColor(cText)
            background = GradientDrawable().apply { setColor(cCardLight); cornerRadius = dp(12).toFloat() }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = lp
        }
        val saveLimitBtn = Button(this).apply {
            text = "Save"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(cOrange); cornerRadius = dp(12).toFloat()
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginStart = dp(10)
            layoutParams = lp
            setOnClickListener {
                val v = limitInput.text.toString().toIntOrNull() ?: 0
                prefs.edit().putInt(ReelAccessibilityService.KEY_LIMIT, v).apply()
                limitStatusTv.text = if (v > 0) "✅ Limit set: $v reels/din" else "Limit off hai"
            }
        }
        limitRow.addView(limitInput)
        limitRow.addView(saveLimitBtn)

        limitStatusTv = TextView(this).apply {
            val v = prefs.getInt(ReelAccessibilityService.KEY_LIMIT, 0)
            text = if (v > 0) "✅ Limit set: $v reels/din" else "Limit off hai"
            textSize = 12f
            setTextColor(cMuted)
            setPadding(0, dp(10), 0, 0)
        }

        limitCard.addView(limitTitle)
        limitCard.addView(limitDesc)
        limitCard.addView(limitRow)
        limitCard.addView(limitStatusTv)

        container.addView(title)
        container.addView(statsCard)
        container.addView(limitCard)
        return container
    }

    // helper: returns a TextView whose .tag holds the actual row View (hacky but keeps code short)
    private fun statRow(label: String, initialValue: String): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val labelTv = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(cMuted)
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = lp
        }
        val valueTv = TextView(this).apply {
            text = initialValue
            textSize = 15f
            setTextColor(cText)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        row.addView(labelTv)
        row.addView(valueTv)
        valueTv.tag = row
        return valueTv
    }

    private fun divider(): View {
        return View(this).apply {
            setBackgroundColor(cCardLight)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        }
    }

    private fun refreshStats() {
        val today = prefs.getInt(ReelAccessibilityService.KEY_TODAY_COUNT, 0)
        todayTv.text = "$today reels"

        var weekTotal = 0
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (i in 0 until 7) {
            val dateStr = fmt.format(cal.time)
            weekTotal += prefs.getInt(ReelAccessibilityService.dayCountKey(dateStr), 0)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        weekTv.text = "$weekTotal reels"
        val avg = weekTotal / 7.0
        avgTv.text = String.format(Locale.US, "%.1f reels/din", avg)

        val seconds = today * 20
        val minutes = seconds / 60
        timeTv.text = if (minutes > 0) "~$minutes min" else "~$seconds sec"
    }

    // ================= SETTINGS TAB =================

    private fun buildSettingsTab(): LinearLayout {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }

        val title = TextView(this).apply {
            text = "⚙️ Settings"
            textSize = 24f
            setTextColor(cText)
            setPadding(0, dp(8), 0, dp(20))
        }

        val settingsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(cCard); cornerRadius = dp(18).toFloat() }
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }

        val strictRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val strictLabel = TextView(this).apply {
            text = "Sirf Reels screen me count karo"
            textSize = 14f
            setTextColor(cText)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val strict = Switch(this).apply {
            isChecked = prefs.getBoolean(ReelAccessibilityService.KEY_STRICT, true)
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean(ReelAccessibilityService.KEY_STRICT, on).apply() }
        }
        strictRow.addView(strictLabel)
        strictRow.addView(strict)

        val themeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, 0)
        }
        val themeLabel = TextView(this).apply {
            text = "☀️ Light mode"
            textSize = 14f
            setTextColor(cText)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        themeSwitch = Switch(this).apply {
            isChecked = prefs.getString(ReelAccessibilityService.KEY_THEME, "dark") == "light"
            setOnCheckedChangeListener { _, on ->
                prefs.edit().putString(ReelAccessibilityService.KEY_THEME, if (on) "light" else "dark").apply()
                recreate()
            }
        }
        themeRow.addView(themeLabel)
        themeRow.addView(themeSwitch)

        settingsCard.addView(strictRow)
        settingsCard.addView(themeRow)

        val updateCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(cCard); cornerRadius = dp(18).toFloat() }
            setPadding(dp(18), dp(16), dp(18), dp(16))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(16)
            layoutParams = lp
        }
        versionTv = TextView(this).apply {
            text = "Version " + BuildConfig.VERSION_NAME
            textSize = 13f
            setTextColor(cMuted)
        }
        updateBtn = Button(this).apply {
            text = "🔍  Update check karo"
            setTextColor(Color.WHITE)
            textSize = 14f
            isAllCaps = false
            background = GradientDrawable().apply { setColor(cCardLight); cornerRadius = dp(14).toFloat() }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(10)
            layoutParams = lp
            setOnClickListener { checkForUpdate(showNoUpdateMessage = true) }
        }
        updateCard.addView(versionTv)
        updateCard.addView(updateBtn)

        val footer = TextView(this).apply {
            text = "Koi data internet pe nahi jata (sirf update check ke alawa). Sab kuch sirf tumhare phone me rehta hai."
            textSize = 12f
            setTextColor(cMuted)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(28), dp(8), 0)
        }

        container.addView(title)
        container.addView(settingsCard)
        container.addView(updateCard)
        container.addView(footer)
        return container
    }

    // ================= UPDATE CHECKER =================

    private fun checkForUpdate(showNoUpdateMessage: Boolean) {
        updateBtn.isEnabled = false
        updateBtn.text = "⏳  Check ho raha hai..."
        Thread {
            try {
                val url = URL("https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val code = conn.responseCode
                if (code != 200) {
                    mainHandler.post {
                        resetUpdateButton()
                        if (showNoUpdateMessage) toastLike("Update check nahi ho paya (koi release nahi mila).")
                    }
                    return@Thread
                }
                val bodyText = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(bodyText)
                val tag = json.optString("tag_name", "").removePrefix("v").trim()
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        val name = a.optString("name", "")
                        if (name.endsWith(".apk")) {
                            apkUrl = a.optString("browser_download_url")
                            break
                        }
                    }
                }
                mainHandler.post {
                    resetUpdateButton()
                    if (tag.isNotEmpty() && tag != BuildConfig.VERSION_NAME && apkUrl != null) {
                        showUpdateDialog(tag, apkUrl)
                    } else if (showNoUpdateMessage) {
                        toastLike("Aap latest version (${BuildConfig.VERSION_NAME}) pe ho ✅")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    resetUpdateButton()
                    if (showNoUpdateMessage) toastLike("Internet check karo, update fetch nahi hua.")
                }
            }
        }.start()
    }

    private fun resetUpdateButton() {
        updateBtn.isEnabled = true
        updateBtn.text = "🔍  Update check karo"
    }

    private fun toastLike(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show()
    }

    private fun showUpdateDialog(newVersion: String, apkUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("🎉 Naya version aa gaya!")
            .setMessage("Version $newVersion available hai (abhi ${BuildConfig.VERSION_NAME} hai).\n\nDownload karke install karein?")
            .setPositiveButton("Update karo") { _, _ -> downloadAndInstall(apkUrl) }
            .setNegativeButton("Baad me", null)
            .show()
    }

    private fun downloadAndInstall(apkUrl: String) {
        val progress = AlertDialog.Builder(this)
            .setTitle("Download ho raha hai...")
            .setView(ProgressBar(this).apply { setPadding(dp(24), dp(24), dp(24), dp(24)) })
            .setCancelable(false)
            .show()

        Thread {
            try {
                val url = URL(apkUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connect()
                val dir = getExternalFilesDir(null)
                val file = File(dir, "ReelCounter-update.apk")
                conn.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                mainHandler.post { progress.dismiss(); installApk(file) }
            } catch (e: Exception) {
                mainHandler.post { progress.dismiss(); toastLike("Download fail ho gaya, dobara try karo.") }
            }
        }.start()
    }

    private fun installApk(file: File) {
        val uri: Uri = FileProvider.getUriForFile(this, "com.reelcounter.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
