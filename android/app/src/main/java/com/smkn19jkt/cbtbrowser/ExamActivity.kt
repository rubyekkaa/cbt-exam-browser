package com.smkn19jkt.cbtbrowser

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.Ringtone
import android.media.RingtoneManager
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
        const val EXTRA_EXIT_PIN = "extra_exit_pin"
        private const val ZOOM_STEP = 10
        private const val MIN_ZOOM = 50
        private const val MAX_ZOOM = 200
    }

    private lateinit var binding: ActivityExamBinding
    private lateinit var penaltyManager: PenaltyManager
    private var currentZoom = 100
    private var exitPin = ""
    private val handler = Handler(Looper.getMainLooper())

    /** True selama sesi ujian terkunci. Menjadi false hanya saat keluar SAH (via PIN). */
    private var isLocked = true

    /** True jika siswa keluar dengan sah (PIN benar) sehingga tidak kena penalti. */
    private var legitimateExit = false

    /** Penanda apakah halaman soal sudah termuat (penalti hanya berlaku setelah ini). */
    private var enteredExam = false

    private var forcedExitRingtone: Ringtone? = null

    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cegah screenshot.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityExamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        penaltyManager = PenaltyManager(this)

        // Jika masih ada penalti aktif, jangan izinkan ujian.
        if (penaltyManager.isPenaltyActive()) {
            startActivity(Intent(this, PenaltyActivity::class.java))
            finish()
            return
        }

        val url = intent.getStringExtra(EXTRA_URL) ?: run {
            finish()
            return
        }
        exitPin = intent.getStringExtra(EXTRA_EXIT_PIN) ?: ""

        // Cegah sentuhan ketika layar tertutup overlay aplikasi lain (anti app virtual/floating).
        binding.root.filterTouchesWhenObscured = true

        setupFullscreen()
        setupWebView(url)
        setupToolbar()
        startTimeUpdater()

        // LANGSUNG pin aplikasi tanpa konfirmasi.
        enableKioskMode()
    }

    /**
     * Aktifkan kiosk mode. Jika app adalah Device Owner, lock task menjadi penuh
     * (Home/Recent mati total, overlay diblok sistem). Jika tidak, jatuh ke
     * screen pinning bawaan (best-effort).
     */
    private fun enableKioskMode() {
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(this, AdminReceiver::class.java)
            if (dpm.isDeviceOwnerApp(packageName)) {
                dpm.setLockTaskPackages(admin, arrayOf(packageName))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dpm.setLockTaskFeatures(
                        admin,
                        DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                    )
                }
            }
        } catch (e: Exception) {
            // Bukan device owner; lanjut dengan screen pinning biasa.
        }

        try {
            startLockTask()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Mode kunci tidak aktif. Aktifkan 'Sematkan jendela' di Pengaturan keamanan.",
                Toast.LENGTH_LONG
            ).show()
        }
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
    private fun setupWebView(url: String) {
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
            }

            // Aktifkan cookies.
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
                    // Siswa sudah masuk ke halaman soal -> mulai berlaku penalti.
                    if (!enteredExam) {
                        enteredExam = true
                        penaltyManager.markEnteredExam()
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val requestUrl = request?.url?.toString() ?: return false
                    if (requestUrl.startsWith("http://") || requestUrl.startsWith("https://")) {
                        return false
                    }
                    return true // Blokir URL non-http (mencegah buka app lain).
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

                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: android.os.Message?
                ): Boolean = false
            }

            setOnLongClickListener { true }
            isLongClickable = false
            isHapticFeedbackEnabled = false

            loadUrl(url)
        }
    }

    private fun setupToolbar() {
        binding.btnZoomIn.setOnClickListener {
            if (currentZoom < MAX_ZOOM) {
                currentZoom += ZOOM_STEP
                applyZoom()
            }
        }
        binding.btnZoomOut.setOnClickListener {
            if (currentZoom > MIN_ZOOM) {
                currentZoom -= ZOOM_STEP
                applyZoom()
            }
        }
        binding.btnRefresh.setOnClickListener { binding.webView.reload() }
        binding.btnBack.setOnClickListener {
            if (binding.webView.canGoBack()) binding.webView.goBack()
        }
        binding.btnForward.setOnClickListener {
            if (binding.webView.canGoForward()) binding.webView.goForward()
        }
        binding.btnExit.setOnClickListener { showExitDialog() }
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

    /**
     * Tombol Exit. Keluar HANYA bisa lewat PIN guru/pengawas.
     * Keluar via PIN benar = SAH, tidak kena penalti.
     */
    private fun showExitDialog() {
        if (exitPin.isEmpty()) {
            // Tidak ada PIN diset: keluar tetap butuh konfirmasi, dianggap sah (oleh pengawas).
            AlertDialog.Builder(this)
                .setTitle("Keluar Ujian")
                .setMessage("Keluar dari ujian harus seizin pengawas.\n\nLanjut keluar?")
                .setPositiveButton("Keluar") { _, _ -> exitLegitimately() }
                .setNegativeButton("Batal", null)
                .setCancelable(false)
                .show()
            return
        }

        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Masukkan PIN pengawas"
        }
        AlertDialog.Builder(this)
            .setTitle("Keluar Ujian")
            .setMessage("Masukkan PIN pengawas untuk keluar tanpa pelanggaran:")
            .setView(editText)
            .setPositiveButton("Keluar") { _, _ ->
                if (editText.text.toString() == exitPin) {
                    exitLegitimately()
                } else {
                    Toast.makeText(this, "PIN salah! Keluar paksa akan dikenai penalti.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Batal", null)
            .setCancelable(false)
            .show()
    }

    /** Keluar sah via PIN: bersihkan sesi, tidak ada penalti. */
    private fun exitLegitimately() {
        legitimateExit = true
        isLocked = false
        penaltyManager.clearExamSession()
        stopLockTaskSafely()
        finish()
    }

    private fun stopLockTaskSafely() {
        try {
            stopLockTask()
        } catch (e: Exception) {
            // Mungkin tidak dalam mode lock task.
        }
    }

    /**
     * Dipanggil saat terdeteksi keluar paksa (tanpa PIN). Catat pelanggaran,
     * bunyikan alarm, dan paksa kembali ke foreground.
     */
    private fun handleForcedExit() {
        if (legitimateExit || !isLocked) return

        val card = penaltyManager.registerForcedExit()
        if (card != PenaltyManager.CardType.NONE) {
            playForcedExitAlarm()
        }

        // Paksa aplikasi kembali ke depan.
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.appTasks.firstOrNull()?.moveToFront()
    }

    private fun playForcedExitAlarm() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            forcedExitRingtone = RingtoneManager.getRingtone(applicationContext, uri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build()
                }
                play()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    // Tombol back dinonaktifkan total selama ujian.
    override fun onBackPressed() {
        Toast.makeText(this, "Tombol kembali dinonaktifkan selama ujian!", Toast.LENGTH_SHORT).show()
    }

    // Deteksi keluar paksa (Home/Recent/overlay membuat activity ter-pause).
    override fun onPause() {
        super.onPause()
        if (isLocked && !legitimateExit) {
            handleForcedExit()
        }
    }

    override fun onResume() {
        super.onResume()
        setupFullscreen()
        // Hentikan alarm keluar paksa begitu kembali ke ujian.
        forcedExitRingtone?.stop()
        forcedExitRingtone = null

        // Jika penalti menjadi aktif (akibat keluar paksa), alihkan ke layar penalti.
        if (isLocked && penaltyManager.isPenaltyActive()) {
            isLocked = false
            stopLockTaskSafely()
            startActivity(Intent(this, PenaltyActivity::class.java))
            finish()
        }
    }

    // Blokir tombol fisik (Home, Recent, Menu, Volume untuk kombinasi screenshot).
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
        } else if (!hasFocus && isLocked && !legitimateExit) {
            // Kehilangan fokus saat terkunci = kemungkinan overlay/notification shade -> tutup.
            try {
                @Suppress("DEPRECATION")
                sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeUpdateRunnable)
        forcedExitRingtone?.stop()
        forcedExitRingtone = null
        binding.webView.apply {
            stopLoading()
            destroy()
        }
        super.onDestroy()
    }
}
