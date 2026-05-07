package com.smkn19jkt.cbtbrowser

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.zxing.integration.android.IntentIntegrator
import com.smkn19jkt.cbtbrowser.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startQRScanner()
        } else {
            Toast.makeText(this, "Izin kamera diperlukan untuk scan QR", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("cbt_prefs", Context.MODE_PRIVATE)

        setupUI()
        loadSavedSettings()
    }

    private fun setupUI() {
        // Start Exam button
        binding.btnStartExam.setOnClickListener {
            val url = binding.etUrl.text.toString().trim()
            if (url.isEmpty()) {
                binding.etUrl.error = "Masukkan URL test center"
                return@setOnClickListener
            }

            val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "http://$url"
            } else {
                url
            }

            saveSettings(finalUrl)
            showConfirmationDialog(finalUrl)
        }

        // QR Scanner button
        binding.btnScanQr.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                startQRScanner()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // Custom User Agent toggle
        binding.switchUserAgent.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("custom_user_agent", isChecked).apply()
            binding.tvUserAgentStatus.text = if (isChecked) "User Agent: cbt-exam-browser" else "User Agent: Default"
        }

        // Recent URLs
        binding.btnRecentUrls.setOnClickListener {
            showRecentUrls()
        }

        // App version
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            binding.tvVersion.text = "v${pInfo.versionName}"
        } catch (e: Exception) {
            binding.tvVersion.text = "v1.0.0"
        }
    }

    private fun loadSavedSettings() {
        val savedUrl = prefs.getString("last_url", "") ?: ""
        if (savedUrl.isNotEmpty()) {
            binding.etUrl.setText(savedUrl)
        }

        val customUserAgent = prefs.getBoolean("custom_user_agent", true)
        binding.switchUserAgent.isChecked = customUserAgent
        binding.tvUserAgentStatus.text = if (customUserAgent) "User Agent: cbt-exam-browser" else "User Agent: Default"
    }

    private fun saveSettings(url: String) {
        prefs.edit().apply {
            putString("last_url", url)
            // Save to recent URLs
            val recentUrls = prefs.getStringSet("recent_urls", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            recentUrls.add(url)
            if (recentUrls.size > 10) {
                recentUrls.remove(recentUrls.first())
            }
            putStringSet("recent_urls", recentUrls)
            apply()
        }
    }

    private fun showConfirmationDialog(url: String) {
        AlertDialog.Builder(this)
            .setTitle("Mulai Ujian")
            .setMessage("Anda akan memulai ujian.\n\nServer: ${maskUrl(url)}\n\nPastikan:\n• Koneksi internet stabil\n• Baterai cukup\n• Jangan keluar dari aplikasi\n\nLanjutkan?")
            .setPositiveButton("Mulai") { _, _ ->
                startExam(url)
            }
            .setNegativeButton("Batal", null)
            .setCancelable(false)
            .show()
    }

    private fun maskUrl(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            "${uri.scheme}://${uri.host}/***"
        } catch (e: Exception) {
            "***"
        }
    }

    private fun startExam(url: String) {
        val useCustomUserAgent = prefs.getBoolean("custom_user_agent", true)
        val exitPin = prefs.getString("exit_pin", "") ?: ""

        val intent = Intent(this, ExamActivity::class.java).apply {
            putExtra(ExamActivity.EXTRA_URL, url)
            putExtra(ExamActivity.EXTRA_CUSTOM_USER_AGENT, useCustomUserAgent)
            putExtra(ExamActivity.EXTRA_EXIT_PIN, exitPin)
        }
        startActivity(intent)
    }

    private fun startQRScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Arahkan kamera ke QR Code URL ujian")
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(true)
        integrator.initiateScan()
    }

    @Deprecated("Use ActivityResultContracts")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                binding.etUrl.setText(result.contents)
                Toast.makeText(this, "QR Code berhasil dipindai", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun showRecentUrls() {
        val recentUrls = prefs.getStringSet("recent_urls", emptySet())?.toList() ?: emptyList()
        if (recentUrls.isEmpty()) {
            Toast.makeText(this, "Belum ada riwayat URL", Toast.LENGTH_SHORT).show()
            return
        }

        val maskedUrls = recentUrls.map { maskUrl(it) }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("URL Terakhir")
            .setItems(maskedUrls) { _, which ->
                binding.etUrl.setText(recentUrls[which])
            }
            .setNegativeButton("Tutup", null)
            .show()
    }
}
