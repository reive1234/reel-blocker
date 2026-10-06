package com.example.reelblocker

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.CompoundButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var toggle: Switch
    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        toggle = findViewById(R.id.blockToggle)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                applyBlocker()
            }
        }

        val enabled = prefs.getBoolean("block_reels", true)
        toggle.isChecked = enabled

        toggle.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            prefs.edit().putBoolean("block_reels", isChecked).apply()
            applyBlocker()
        }

        webView.loadUrl("https://www.instagram.com/")
    }

    private fun applyBlocker() {
        val enabled = prefs.getBoolean("block_reels", true)
        val js = if (enabled) BLOCK_JS else UNBLOCK_JS
        webView.evaluateJavascript(js, null)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    companion object {
        private const val BLOCK_JS = """
        (function() {
            window.__reelBlockerEnabled = true;
            var style = document.getElementById('reel-blocker-style');
            if (!style) {
                style = document.createElement('style');
                style.id = 'reel-blocker-style';
                document.head.appendChild(style);
            }
            style.innerHTML = `
                a[href="/reels/"] { display: none !important; }
                article:has(a[href*="/reel/"]) { display: none !important; }
                button[aria-label*="Next"], button[aria-label*="Далі"],
                button[aria-label*="Previous"], button[aria-label*="Назад"]
                    { display: none !important; }
            `;

            if (!window.__reelBlockerInstalled) {
                window.__reelBlockerInstalled = true;

                window.addEventListener('wheel', function(e) {
                    if (!window.__reelBlockerEnabled) return;
                    if (location.pathname.indexOf('/reel/') === 0) e.preventDefault();
                }, { passive: false, capture: true });

                var startX = 0;
                window.addEventListener('touchstart', function(e) {
                    startX = e.touches[0].clientX;
                }, { capture: true });

                window.addEventListener('touchmove', function(e) {
                    if (!window.__reelBlockerEnabled) return;
                    if (location.pathname.indexOf('/reel/') !== 0) return;
                    var dx = Math.abs(e.touches[0].clientX - startX);
                    if (dx > 40) e.preventDefault();
                }, { passive: false, capture: true });
            }
        })();
        """

        private const val UNBLOCK_JS = """
        (function() {
            window.__reelBlockerEnabled = false;
            var style = document.getElementById('reel-blocker-style');
            if (style) style.remove();
        })();
        """
    }
}