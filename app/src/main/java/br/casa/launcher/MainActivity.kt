package br.casa.launcher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView

class MainActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.setBackgroundColor(Color.BLACK)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("file:///android_asset/index.html")
        setContentView(web)
        hideBars()
    }

    private fun hideBars() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun onResume() { super.onResume(); hideBars(); web.evaluateJavascript("window.onResumeHome && onResumeHome()", null) }

    // Tela inicial: o botão voltar não fecha o launcher
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {}

    inner class Bridge {
        @JavascriptInterface
        fun launch(pkg: String): Boolean {
            val i = packageManager.getLaunchIntentForPackage(pkg)
                ?: packageManager.getLeanbackLaunchIntentForPackage(pkg) ?: return false
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
            return true
        }

        @JavascriptInterface
        fun settings() = open(Settings.ACTION_SETTINGS)

        @JavascriptInterface
        fun cast() {
            if (!open("android.settings.CAST_SETTINGS")) open(Settings.ACTION_SETTINGS)
        }

        private fun open(action: String): Boolean = try {
            startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
        } catch (e: Exception) { false }
    }
}
