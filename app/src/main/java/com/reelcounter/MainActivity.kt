package com.reelcounter

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    private lateinit var countTv: TextView
    private lateinit var statusTv: TextView
    private lateinit var statusDot: View
    private lateinit var permBtn: Button

    private val cOrange = Color.parseColor("#F58529")
    private val cPink = Color.parseColor("#DD2A7B")
    private val cPurple = Color.parseColor("#8134AF")
    private val cCard = Color.parseColor("#1E1E1E")
    private val cCardLight = Color.parseColor("#2A2A2A")
    private val cMuted = Color.parseColor("#B3B3B3")
    private val cGreen = Color.parseColor("#3DDC84")
    private val cRed = Color.parseColor("#FF5A5F")

    private val GITHUB_OWNER = "superday962-lab"
    private val GITHUB_REPO = "reelcounter-app2"

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var updateRow: LinearLayout
    private lateinit var updateBtn: Button
    private lateinit var versionTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(ReelAccessibilityService.PREFS, MODE_PRIVATE)

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(48), dp(24), dp(32))
        }

        val title = TextView(this).apply {
            text = "🎬 Reel Counter"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
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
            background = GradientDrawable().apply {
                setColor(cCard)
                cornerRadius = dp(22).toFloat()
            }
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        countTv = TextView(this).apply {
            textSize = 64f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val countLabel = TextView(this).apply {
            text = "REELS COUNTED"
            textSize = 12f
            setTextColor(cMuted)
            gravity = Gravity.CENTER
            letterSpacing = 0.15f
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
                gravity = Gravity.CENTER_VERTICAL
                marginEnd = dp(8)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(cRed)
            }
        }
        statusTv = TextView(this).apply {
            textSize = 14f
            setTextColor(cMuted)
        }
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
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }

        val howToBtn = Button(this).apply {
            text = "❓  Permission kaise du? (steps dekho)"
            setTextColor(Color.WHITE)
            textSize = 14f
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(cCardLight)
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(12)
            layoutParams = lp
            setOnClickListener { showHowTo() }
        }

        val settingsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cCard)
                cornerRadius = dp(18).toFloat()
            }
            setPadding(dp(18), dp(16), dp(18), dp(16))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(24)
            layoutParams = lp
        }

        val strictRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val strictLabel = TextView(this).apply {
            text = "Sirf Reels screen me count karo"
            textSize = 14f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val strict = Switch(this).apply {
            isChecked = prefs.getBoolean(ReelAccessibilityService.KEY_STRICT, true)
            setOnCheckedChangeListener { _, on ->
                prefs.edit().putBoolean(ReelAccessibilityService.KEY_STRICT, on).apply()
            }
        }
        strictRow.addView(strictLabel)
        strictRow.addView(strict)

        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#333333"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ).apply { topMargin = dp(14); bottomMargin = dp(14) }
        }

        val resetBtn = Button(this).apply {
            text = "🔄  Count reset karo"
            setTextColor(cRed)
            textSize = 14f
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(cCardLight)
                cornerRadius = dp(14).toFloat()
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Reset karein?")
                    .setMessage("Count wapas 0 ho jayega.")
                    .setPositiveButton("Haan, reset karo") { _, _ ->
                        prefs.edit().putInt(ReelAccessibilityService.KEY_COUNT, 0).apply()
                        refresh()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        settingsCard.addView(strictRow)
        settingsCard.addView(divider)
        settingsCard.addView(resetBtn)

        updateRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cCard)
                cornerRadius = dp(18).toFloat()
            }
            setPadding(dp(18), dp(16), dp(18), dp(16))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
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
            background = GradientDrawable().apply {
                setColor(cCardLight)
                cornerRadius = dp(14).toFloat()
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
            setOnClickListener { checkForUpdate(showNoUpdateMessage = true) }
        }
        updateRow.addView(versionTv)
        updateRow.addView(updateBtn)

        val footer = TextView(this).apply {
            text = "Koi data internet pe nahi jata. Sab kuch sirf tumhare phone me rehta hai."
            textSize = 12f
            setTextColor(cMuted)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(28), dp(8), 0)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(countCard)
        root.addView(statusRow)
        root.addView(permBtn)
        root.addView(howToBtn)
        root.addView(settingsCard)
        root.addView(updateRow)
        root.addView(footer)

        checkForUpdate(showNoUpdateMessage = false)

        scroll.addView(root)
        outer.addView(scroll)
        setContentView(outer)
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
            .setPositiveButton("Accessibility settings kholo") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Band karo", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val prefs = getSharedPreferences(ReelAccessibilityService.PREFS, MODE_PRIVATE)
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
                conn.inputStream.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                mainHandler.post {
                    progress.dismiss()
                    installApk(file)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    progress.dismiss()
                    toastLike("Download fail ho gaya, dobara try karo.")
                }
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
