package com.smkn19jkt.cbtbrowser

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.smkn19jkt.cbtbrowser.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var penaltyManager: PenaltyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        penaltyManager = PenaltyManager(this)

        buildServerList()
        showVersion()
    }

    override fun onResume() {
        super.onResume()
        // Refresh status penalti & ketersediaan tombol setiap kembali ke layar ini.
        refreshPenaltyState()
    }

    /** Bangun tombol pilihan server dari daftar URL yang ditanam di ExamConfig. */
    private fun buildServerList() {
        val container = binding.serverListContainer
        container.removeAllViews()

        ExamConfig.EXAM_SERVERS.forEach { server ->
            val button = MaterialButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(12) }
                text = server.label
                textSize = 16f
                gravity = Gravity.CENTER
                setOnClickListener { startExam(server) }
            }
            container.addView(button)
        }
    }

    private fun startExam(server: ExamServer) {
        // Jika sedang kena penalti, blokir dan tampilkan layar penalti.
        if (penaltyManager.isPenaltyActive()) {
            startActivity(Intent(this, PenaltyActivity::class.java))
            return
        }

        val intent = Intent(this, ExamActivity::class.java).apply {
            putExtra(ExamActivity.EXTRA_URL, server.url)
        }
        startActivity(intent)
    }

    /** Tampilkan / sembunyikan banner penalti dan aktif/nonaktifkan tombol server. */
    private fun refreshPenaltyState() {
        val active = penaltyManager.isPenaltyActive()

        // Nonaktifkan semua tombol server selama penalti.
        for (i in 0 until binding.serverListContainer.childCount) {
            binding.serverListContainer.getChildAt(i).isEnabled = !active
        }

        if (active) {
            val card = penaltyManager.activeCardType
            val cardName = if (card == PenaltyManager.CardType.RED) "KARTU MERAH" else "KARTU KUNING"
            binding.tvPenaltyInfo.text =
                "$cardName\nAnda melanggar dengan keluar paksa dari ujian " +
                "(${penaltyManager.violationCount}x).\n" +
                "Sisa waktu penalti: ${formatRemaining(penaltyManager.remainingMillis())}\n" +
                "Tunggu hingga selesai sebelum melanjutkan ujian."
            binding.cardPenaltyInfo.visibility = android.view.View.VISIBLE

            // Buka langsung layar penalti agar siswa melihat hitungan mundur.
            startActivity(Intent(this, PenaltyActivity::class.java))
        } else {
            binding.cardPenaltyInfo.visibility = android.view.View.GONE
        }
    }

    private fun formatRemaining(ms: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun showVersion() {
        binding.tvVersion.text = try {
            "v${packageManager.getPackageInfo(packageName, 0).versionName}"
        } catch (e: Exception) {
            "v1.0.0"
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
