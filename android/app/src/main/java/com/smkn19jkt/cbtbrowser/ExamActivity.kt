package com.smkn19jkt.cbtbrowser

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.smkn19jkt.cbtbrowser.databinding.ActivityExamBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExamActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_CUSTOM_USER_AGENT = "extra_custom_user_agent"
        const val EXTRA_EXIT_PIN = "extra_exit_pin"
        private const val CUSTOM_USER_AGENT = "cbt-exam-browser"
        private const val ZOOM_STEP = 10
        private const val MIN_ZOOM = 50
        private const val MAX_ZOOM = 200
    }

    private lateinit var binding: ActivityExamBinding
    private var currentZoom = 100
    private var exitPin = ""
    private val handler = Handler(Looper.getMainLooper())
    private var isLocked = true
    private var penaltyCount = 0

    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityExamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra(EXTRA_URL) ?: run {
            finish()
            return
        }
        val useCustomUserAgent = intent.getBooleanExtra(EXTRA_CUSTOM_USER_AGENT, true)
        exitPin = intent.getStringExtra(EXTRA_EXIT_PIN) ?: ""

        setupFullscreen()
        setupWebView(url, useCustomUserAgent)
        setupToolbar()
        startTimeUpdater()

        // Start screen pinning (kiosk mode)
        startLockTask()
    }

    private fun setupFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(url: String, useCustomUserAgent: Boolean) {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = false
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                if (useCustomUserAgent) {
                    userAgentString = CUSTOM_USER_AGENT
                }
            }

            // Enable cookies
            val webViewRef = this
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(webViewRef, true)
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    // Prevent opening external apps/browsers
                    val requestUrl = request?.url?.toString() ?: return false
                    if (requestUrl.startsWith("http://") || requestUrl.startsWith("https://")) {
                        return false // Load in WebView
                    }
                    return true // Block non-http URLs
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Toast.makeText(
                        this@ExamActivity,
                        "Error: $description",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    binding.progressBar.progress = newProgress
                    if (newProgress == 100) {
                        binding.progressBar.visibility = View.GONE
                    }
                }

                // Prevent opening new windows
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: android.os.Message?
                ): Boolean = false
            }

            // Disable long press (prevent copy/paste)
            setOnLongClickListener { true }
            isLongClickable = false
            isHapticFeedbackEnabled = false

            loadUrl(url)
        }
    }

    private fun setupToolbar() {
        // Zoom In
        binding.btnZoomIn.setOnClickListener {
            if (currentZoom < MAX_ZOOM) {
                currentZoom += ZOOM_STEP
                applyZoom()
            }
        }

        // Zoom Out
        binding.btnZoomOut.setOnClickListener {
            if (currentZoom > MIN_ZOOM) {
                currentZoom -= ZOOM_STEP
                applyZoom()
            }
        }

        // Refresh
        binding.btnRefresh.setOnClickListener {
            binding.webView.reload()
        }

        // Back
        binding.btnBack.setOnClickListener {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            }
        }

        // Forward
        binding.btnForward.setOnClickListener {
            if (binding.webView.canGoForward()) {
                binding.webView.goForward()
            }
        }

        // Exit
        binding.btnExit.setOnClickListener {
            showExitDialog()
        }

        // Zoom level display
        binding.tvZoomLevel.text = "$currentZoom%"
    }

    private fun applyZoom() {
        binding.webView.settings.textZoom = currentZoom
        binding.tvZoomLevel.text = "$currentZoom%"
    }

    private fun updateTime() {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        binding.tvTime.text = sdf.format(Date())
    }

    private fun startTimeUpdater() {
        handler.post(timeUpdateRunnable)
    }

    private fun showExitDialog() {
        if (exitPin.isNotEmpty()) {
            // PIN protected exit
            val editText = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                hint = "Masukkan PIN"
            }

            AlertDialog.Builder(this)
                .setTitle("Keluar Ujian")
                .setMessage("Masukkan PIN untuk keluar dari ujian:")
                .setView(editText)
                .setPositiveButton("Keluar") { _, _ ->
                    if (editText.text.toString() == exitPin) {
                        exitExam()
                    } else {
                        Toast.makeText(this, "PIN salah!", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Batal", null)
                .setCancelable(false)
                .show()
        } else {
            // Simple confirmation
            AlertDialog.Builder(this)
                .setTitle("Keluar Ujian")
                .setMessage("Apakah Anda yakin ingin keluar dari ujian?\n\nPeringatan: Keluar saat ujian berlangsung dapat mempengaruhi nilai Anda.")
                .setPositiveButton("Keluar") { _, _ ->
                    exitExam()
                }
                .setNegativeButton("Lanjut Ujian", null)
                .setCancelable(false)
                .show()
        }
    }

    private fun exitExam() {
        isLocked = false
        try {
            stopLockTask()
        } catch (e: Exception) {
            // May not be in lock task mode
        }
        finish()
    }

    // Prevent back button
    override fun onBackPressed() {
        penaltyCount++
        Toast.makeText(
            this,
            "Tombol kembali dinonaktifkan selama ujian! (Peringatan: $penaltyCount)",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Prevent recent apps button
    override fun onPause() {
        super.onPause()
        if (isLocked) {
            penaltyCount++
            // Bring back to foreground immediately
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                am.appTasks.firstOrNull()?.moveToFront()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupFullscreen()
    }

    // Block volume keys to prevent screenshot combinations
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && isLocked) {
            setupFullscreen()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeUpdateRunnable)
        binding.webView.apply {
            stopLoading()
            destroy()
        }
        super.onDestroy()
    }
}
