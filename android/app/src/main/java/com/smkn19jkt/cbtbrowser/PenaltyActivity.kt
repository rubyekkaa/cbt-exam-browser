package com.smkn19jkt.cbtbrowser

import android.media.RingtoneManager
import android.media.Ringtone
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.smkn19jkt.cbtbrowser.databinding.ActivityPenaltyBinding
import java.util.concurrent.TimeUnit

/**
 * Layar penalti (kartu kuning / merah).
 *
 * Ditampilkan ketika siswa mencoba masuk aplikasi saat masih dalam masa penalti
 * akibat keluar paksa dari ujian. Menampilkan hitungan mundur dan membunyikan
 * alarm. Siswa tidak bisa keluar dari layar ini sampai waktu penalti habis.
 */
class PenaltyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPenaltyBinding
    private lateinit var penaltyManager: PenaltyManager
    private var timer: CountDownTimer? = null
    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPenaltyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tampil di atas layar kunci & jaga layar tetap menyala.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        penaltyManager = PenaltyManager(this)

        if (!penaltyManager.isPenaltyActive()) {
            finish()
            return
        }

        setupCard()
        startAlarm()
        startCountdown(penaltyManager.remainingMillis())
    }

    private fun setupCard() {
        val card = penaltyManager.activeCardType
        if (card == PenaltyManager.CardType.RED) {
            binding.tvCardTitle.text = "KARTU MERAH"
            binding.penaltyRoot.setBackgroundColor(0xFFB00020.toInt())
        } else {
            binding.tvCardTitle.text = "KARTU KUNING"
            binding.penaltyRoot.setBackgroundColor(0xFFF9A825.toInt())
        }
        binding.tvViolationMsg.text =
            "Anda keluar paksa dari ujian sebanyak ${penaltyManager.violationCount}x.\n" +
            "Ini merupakan pelanggaran tata tertib ujian."
    }

    private fun startCountdown(durationMs: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvCountdown.text = format(millisUntilFinished)
            }

            override fun onFinish() {
                binding.tvCountdown.text = "00:00"
                stopAlarm()
                penaltyManager.clearActivePenalty()
                finish()
            }
        }.start()
    }

    private fun format(ms: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun startAlarm() {
        try {
            var alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = true
                }
                play()
            }
        } catch (e: Exception) {
            // Abaikan jika alarm tidak bisa diputar.
        }
    }

    private fun stopAlarm() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            // ignore
        }
        ringtone = null
    }

    // Cegah tombol back keluar dari layar penalti.
    override fun onBackPressed() {
        // Tidak melakukan apa-apa: siswa harus menunggu penalti selesai.
    }

    // Blokir tombol fisik (back/home/recent/volume).
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        stopAlarm()
        super.onDestroy()
    }
}
