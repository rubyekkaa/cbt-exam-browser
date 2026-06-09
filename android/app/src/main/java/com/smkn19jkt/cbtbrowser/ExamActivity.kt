package com.smkn19jkt.cbtbrowser

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
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
        private const val ZOOM_STEP = 10
        private const val MIN_ZOOM = 50
        private const val MAX_ZOOM = 200
    }

    private lateinit var binding: ActivityExamBinding
    private lateinit var penaltyManager: PenaltyManager
    private var currentZoom = 100
    private val handler = Handler(Looper.getMainLooper())

    /** True selama sesi ujian terkunci. Menjadi false hanya saat keluar yang sah. */
    private var isLocked = true

    /** True jika siswa keluar dengan sah (PIN benar) sehingga tidak kena penalti. */
    private var legitimateExit = false

    /** Penanda apakah halaman soal sudah termuat (penalti hanya berlaku setelah ini). */
    private var enteredExam = false

    /** True jika ujian sudah selesai (mencapai halaman review/submit final). */
    private var examFinished = false

    /** True jika sedang menampilkan overlay koneksi terputus. */
    private var isConnectionLost = false

    /** URL terakhir yang dimuat (untuk reload saat koneksi kembali). */
    private var lastExamUrl: String = ""

    private var forcedExitPlayer: MediaPlayer? = null

    private val networkRetryRunnable = object : Runnable {
        override fun run() {
            if (isConnectionLost) {
                binding.webView.reload()
                handler.postDelayed(this, ExamConfig.NETWORK_RETRY_INTERVAL_MS)
            }
        }
    }

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
                    if (!url.isNullOrEmpty() && !ExamConfig.isExamFinishedUrl(url)) {
                        lastExamUrl = url
                    }
                    // Deteksi sedini mungkin: jika sudah masuk halaman review = ujian selesai.
                    if (ExamConfig.isExamFinishedUrl(url)) {
                        onExamFinished()
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                    // Halaman berhasil dimuat -> koneksi pulih, sembunyikan overlay.
                    if (isConnectionLost) {
                        hideConnectionLost()
                    }
                    // Siswa sudah masuk ke halaman soal -> mulai berlaku penalti.
                    if (!enteredExam) {
                        enteredExam = true
                        penaltyManager.markEnteredExam()
                    }
                    // Konfirmasi ulang status selesai saat halaman tuntas dimuat.
                    if (ExamConfig.isExamFinishedUrl(url)) {
                        onExamFinished()
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
                    request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    // Hanya tangani error pada main frame (halaman utama), bukan resource kecil.
                    if (request?.isForMainFrame == true) {
                        showConnectionLost()
                    }
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

        // Pintu darurat pengawas: tahan jam selama 5 detik -> input kode rahasia.
        setupEmergencyExit()

        // Tombol coba lagi pada overlay koneksi terputus.
        binding.btnRetryConnection.setOnClickListener {
            binding.webView.reload()
        }
    }

    /**
     * Pintu darurat pengawas. Tahan (long press) pada jam/timer selama [EMERGENCY_HOLD_MS],
     * lalu muncul input kode. Kode benar = keluar bersih tanpa penalti.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupEmergencyExit() {
        var pending: Runnable? = null
        binding.tvTime.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    pending = Runnable { showEmergencyExitDialog() }
                    handler.postDelayed(pending!!, ExamConfig.EMERGENCY_HOLD_MS)
                    true
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    pending?.let { handler.removeCallbacks(it) }
                    pending = null
                    v.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun showEmergencyExitDialog() {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Kode pengawas"
        }
        AlertDialog.Builder(this)
            .setTitle("Keluar Darurat (Pengawas)")
            .setMessage("Masukkan kode pengawas untuk keluar tanpa penalti:")
            .setView(editText)
            .setPositiveButton("Keluar") { _, _ ->
                if (editText.text.toString() == ExamConfig.EMERGENCY_EXIT_CODE) {
                    exitLegitimately()
                } else {
                    Toast.makeText(this, "Kode salah.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .setCancelable(false)
            .show()
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
     * Tombol Exit.
     * - Jika ujian SUDAH selesai (mencapai halaman review) -> keluar bebas, tanpa penalti.
     * - Jika BELUM selesai -> dianggap keluar paksa: peringatan, lalu alarm + penalti.
     */
    private fun showExitDialog() {
        if (examFinished) {
            AlertDialog.Builder(this)
                .setTitle("Keluar Ujian")
                .setMessage("Ujian sudah selesai. Anda dapat keluar dari aplikasi.")
                .setPositiveButton("Keluar") { _, _ -> exitLegitimately() }
                .setNegativeButton("Batal", null)
                .setCancelable(false)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Peringatan Pelanggaran")
            .setMessage(
                "Ujian BELUM selesai.\n\n" +
                "Jika Anda tetap keluar sekarang, ini dianggap PELANGGARAN dan Anda akan " +
                "dikenai penalti (kartu kuning/merah) beserta alarm.\n\n" +
                "Tetap keluar?"
            )
            .setPositiveButton("Tetap Keluar") { _, _ -> exitAsForced() }
            .setNegativeButton("Lanjut Ujian", null)
            .setCancelable(false)
            .show()
    }

    /** Keluar paksa lewat tombol Exit sebelum ujian selesai: catat pelanggaran + alarm. */
    private fun exitAsForced() {
        val card = penaltyManager.registerForcedExit()
        if (card != PenaltyManager.CardType.NONE) {
            playForcedExitAlarm()
        }
        isLocked = false
        stopLockTaskSafely()
        if (penaltyManager.isPenaltyActive()) {
            startActivity(Intent(this, PenaltyActivity::class.java))
        }
        finish()
    }

    /** Keluar sah via PIN: bersihkan sesi, tidak ada penalti. */
    private fun exitLegitimately() {
        legitimateExit = true
        isLocked = false
        penaltyManager.clearExamSession()
        stopLockTaskSafely()
        finish()
    }

    /**
     * Dipanggil saat WebView mencapai halaman review (ujian selesai/submit final).
     *
     * PENTING: sematan kiosk TIDAK langsung dilepas di sini (mencegah celah anak
     * kabur di jeda setelah submit). Kita hanya menandai "selesai" dan membersihkan
     * sesi penalti. Kunci baru dilepas saat siswa menekan tombol Keluar.
     */
    private fun onExamFinished() {
        if (examFinished) return
        examFinished = true
        // Bersihkan sesi agar keluar setelah ini tidak dihitung pelanggaran.
        penaltyManager.clearExamSession()
        Toast.makeText(
            this,
            "Ujian selesai. Tekan tombol Keluar (X) di pojok kanan untuk mengakhiri.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun stopLockTaskSafely() {
        try {
            stopLockTask()
        } catch (e: Exception) {
            // Mungkin tidak dalam mode lock task.
        }
    }

    /** Tampilkan overlay koneksi terputus + mulai auto-retry. Tetap terkunci. */
    private fun showConnectionLost() {
        if (isConnectionLost) return
        isConnectionLost = true
        binding.connectionOverlay.visibility = View.VISIBLE
        handler.postDelayed(networkRetryRunnable, ExamConfig.NETWORK_RETRY_INTERVAL_MS)
    }

    /** Sembunyikan overlay koneksi terputus (koneksi pulih). */
    private fun hideConnectionLost() {
        isConnectionLost = false
        handler.removeCallbacks(networkRetryRunnable)
        binding.connectionOverlay.visibility = View.GONE
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
        forcedExitPlayer?.release()
        forcedExitPlayer = AlarmPlayer.play(this, looping = false)
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
        forcedExitPlayer?.release()
        forcedExitPlayer = null

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
        handler.removeCallbacks(networkRetryRunnable)
        forcedExitPlayer?.release()
        forcedExitPlayer = null
        binding.webView.apply {
            stopLoading()
            destroy()
        }
        super.onDestroy()
    }
}
