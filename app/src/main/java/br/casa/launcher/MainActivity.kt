package br.casa.launcher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : Activity() {

    private class Item(
        val name: String,
        val color: Int,
        val glyph: String,
        val pkgs: List<String> = emptyList(),
        val action: String? = null
    )

    private val items = listOf(
        Item("YouTube", 0xFFC4302B.toInt(), "▶", listOf("com.teamsmart.videomanager.tv", "com.liskovsoft.smarttubetv.beta", "com.google.android.youtube.tv", "com.google.android.youtube")),
        Item("Netflix", 0xFFB20710.toInt(), "N", listOf("com.netflix.ninja", "com.netflix.mediaclient")),
        Item("Disney+", 0xFF113CCF.toInt(), "D+", listOf("com.disney.disneyplus")),
        Item("HBO Max", 0xFF5A2CA0.toInt(), "M", listOf("com.wbd.stream", "com.hbo.hbonow")),
        Item("JW Media", 0xFF4A6DA7.toInt(), "JW", listOf("au.com.dok.mjd.mpfjwb", "org.jw.jwlibrary.mobile")),
        Item("Alinhar tela", 0xFF8A5A00.toInt(), "▣", action = "keystone"),
        Item("Espelhar", 0xFF0F7B6C.toInt(), "⇆", action = "cast"),
        Item("Ajustes", 0xFF3B4252.toInt(), "⚙", action = "settings")
    )

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val IDLE_MS = 120000L

    private val handler = Handler(Looper.getMainLooper())
    private var W = 0
    private var H = 0
    private lateinit var home: FrameLayout
    private lateinit var saver: FrameLayout
    private lateinit var saverBox: LinearLayout
    private lateinit var saverDate: TextView
    private lateinit var hello: TextView
    private lateinit var dateView: TextView
    private val tiles = ArrayList<View>()

    private val sleepTask = Runnable { showSaver() }
    private val moveTask = object : Runnable {
        override fun run() { moveSaver(); handler.postDelayed(this, 20000) }
    }
    private val tickTask = object : Runnable {
        override fun run() { updateTexts(); handler.postDelayed(this, 30000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dm = resources.displayMetrics
        W = dm.widthPixels
        H = dm.heightPixels
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        home = buildHome()
        saver = buildSaver()
        root.addView(home, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(saver, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)
        hideBars()
    }

    override fun onResume() {
        super.onResume()
        hideBars()
        hideSaver()
        updateTexts()
        handler.removeCallbacks(tickTask)
        handler.postDelayed(tickTask, 30000)
        resetIdle()
        home.alpha = 0f
        home.animate().alpha(1f).setDuration(500).start()
        tiles.forEachIndexed { i, t ->
            t.translationY = H * 0.06f
            t.animate().setStartDelay(i * 60L).setDuration(380).translationY(0f).start()
        }
        if (currentFocus == null) tiles.firstOrNull()?.requestFocus()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(sleepTask)
        handler.removeCallbacks(moveTask)
        handler.removeCallbacks(tickTask)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {}

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val wasOn = saver.visibility == View.VISIBLE
        resetIdle()
        if (wasOn) {
            if (e.action == KeyEvent.ACTION_DOWN) hideSaver()
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    private fun hideBars() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    private fun px(f: Float) = (W * f).toInt()

    private fun tv(text: String, size: Float, alpha: Float = 1f, light: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            this.alpha = alpha
            setTextSize(TypedValue.COMPLEX_UNIT_PX, W * size)
            if (light) typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }

    private fun clockView(size: Float, alpha: Int): TextClock = TextClock(this).apply {
        format24Hour = "HH:mm"
        format12Hour = "HH:mm"
        setTextSize(TypedValue.COMPLEX_UNIT_PX, W * size)
        setTextColor(Color.argb(alpha, 255, 255, 255))
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }

    private fun buildHome(): FrameLayout {
        val f = FrameLayout(this)
        f.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFF24396B.toInt(), 0xFF0A101D.toInt())
        )
        hello = tv("", 0.026f, 0.85f)
        dateView = tv("", 0.02f, 0.65f)
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(hello)
            addView(dateView)
        }
        f.addView(left, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.START).apply {
            setMargins(px(0.05f), px(0.04f), 0, 0)
        })
        f.addView(clockView(0.085f, 255), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.END).apply {
            setMargins(0, px(0.02f), px(0.05f), 0)
        })
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val gap = px(0.012f)
        val tileW = ((W * 0.9f - gap * (items.size - 1)) / items.size).toInt()
        items.forEachIndexed { idx, item ->
            val t = buildTile(item, tileW)
            val lp = LinearLayout.LayoutParams(tileW, (tileW * 1.05f).toInt())
            if (idx < items.size - 1) lp.marginEnd = gap
            row.addView(t, lp)
            tiles.add(t)
        }
        f.addView(row, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM).apply {
            setMargins(px(0.05f), 0, px(0.05f), (H * 0.14f).toInt())
        })
        return f
    }

    private fun findIcon(item: Item): Drawable? {
        for (p in item.pkgs) {
            try {
                return packageManager.getApplicationBanner(p) ?: packageManager.getApplicationIcon(p)
            } catch (e: Exception) { }
        }
        return null
    }

    private fun buildTile(item: Item, w: Int): View {
        val card = FrameLayout(this)
        val icon = findIcon(item)
        val bg = GradientDrawable().apply {
            cornerRadius = w * 0.1f
            setColor(if (icon != null) 0xFF1B2640.toInt() else item.color)
        }
        card.background = bg
        card.isFocusable = true
        if (icon != null) {
            val iv = ImageView(this)
            iv.setImageDrawable(icon)
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            card.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH).apply {
                setMargins(w / 8, w / 8, w / 8, w / 4)
            })
        } else {
            val g = tv(item.glyph, 0.035f)
            g.typeface = Typeface.DEFAULT_BOLD
            card.addView(g, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER).apply {
                bottomMargin = w / 8
            })
            if (item.pkgs.isNotEmpty()) card.alpha = 0.55f
        }
        val label = tv(item.name, 0.014f)
        card.addView(label, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = w / 14
        })
        card.setOnFocusChangeListener { v, has ->
            val s = if (has) 1.12f else 1f
            v.animate().setStartDelay(0).setDuration(140).scaleX(s).scaleY(s).start()
            v.translationZ = if (has) 16f else 0f
            bg.setStroke(if (has) (W * 0.004f).toInt().coerceAtLeast(2) else 0, Color.WHITE)
        }
        card.setOnClickListener { run(item) }
        return card
    }

    private fun run(item: Item) {
        when (item.action) {
            "keystone" -> {
                if (!tryComponent("com.softwinner.tcorrection", "com.softwinner.tcorrection.projection.ProjectionSettingActivity"))
                    Toast.makeText(this, "Ajuste de tela não disponível", Toast.LENGTH_LONG).show()
                return
            }
            "settings" -> { tryStart(Settings.ACTION_SETTINGS); return }
            "cast" -> { if (!tryStart("android.settings.CAST_SETTINGS")) tryStart(Settings.ACTION_SETTINGS); return }
        }
        for (p in item.pkgs) {
            val i = packageManager.getLaunchIntentForPackage(p)
                ?: packageManager.getLeanbackLaunchIntentForPackage(p)
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                return
            }
        }
        Toast.makeText(this, item.name + " não está instalado", Toast.LENGTH_LONG).show()
    }

    private fun tryComponent(pkg: String, cls: String): Boolean = try {
        startActivity(Intent().setClassName(pkg, cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (e: Exception) { false }

    private fun tryStart(action: String): Boolean = try {
        startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (e: Exception) { false }

    private fun buildSaver(): FrameLayout {
        val f = FrameLayout(this)
        f.setBackgroundColor(Color.BLACK)
        f.visibility = View.GONE
        saverDate = tv("", 0.025f, 0.6f)
        saverBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(clockView(0.11f, 215))
            addView(saverDate)
        }
        f.addView(saverBox, FrameLayout.LayoutParams(WRAP, WRAP))
        return f
    }

    private fun resetIdle() {
        handler.removeCallbacks(sleepTask)
        handler.postDelayed(sleepTask, IDLE_MS)
    }

    private fun showSaver() {
        saver.visibility = View.VISIBLE
        handler.removeCallbacks(moveTask)
        handler.post(moveTask)
    }

    private fun hideSaver() {
        saver.visibility = View.GONE
        handler.removeCallbacks(moveTask)
    }

    private fun moveSaver() {
        saverBox.translationX = Random.nextFloat() * W * 0.5f
        saverBox.translationY = Random.nextFloat() * H * 0.55f
    }

    private fun updateTexts() {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        hello.text = if (h < 12) "Bom dia" else if (h < 18) "Boa tarde" else "Boa noite"
        val d = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(Date())
            .replaceFirstChar { it.uppercase() }
        dateView.text = d
        saverDate.text = d
    }
}
