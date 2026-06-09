package com.smkn19jkt.cbtbrowser

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Pengontrol alarm pelanggaran.
 *
 * Tujuan: alarm terdengar KERAS dan TIDAK BISA dikecilkan siswa.
 *
 * Tiga lapis:
 * 1. Diputar di stream ALARM dengan benar (AudioAttributes diset SEBELUM prepare),
 *    sehingga tombol volume media tidak memengaruhinya.
 * 2. Volume stream ALARM dipaksa ke MAKSIMUM secara berkala selama alarm berbunyi,
 *    jadi kalaupun siswa menekan volume-turun, langsung dikembalikan ke maksimum.
 * 3. (Opsional) menonaktifkan Do Not Disturb sementara jika izin tersedia, agar
 *    alarm tetap terdengar.
 *
 * Pemakaian: panggil [start] saat pelanggaran, dan [stop] saat selesai.
 */
class AlarmController(private val context: Context) {

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var previousDndFilter: Int? = null

    private val volumeEnforcer = object : Runnable {
        override fun run() {
            forceMaxAlarmVolume()
            handler.postDelayed(this, VOLUME_ENFORCE_INTERVAL_MS)
        }
    }

    /**
     * Mulai alarm.
     * @param looping true untuk berputar terus (mis. layar penalti).
     */
    fun start(looping: Boolean) {
        stop()

        relaxDndIfPossible()
        forceMaxAlarmVolume()

        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                val raw = "android.resource://${context.packageName}/${R.raw.alarm_pelanggaran}"
                setDataSource(context, Uri.parse(raw))
                isLooping = looping
                setVolume(1.0f, 1.0f)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    if (!looping) stop()
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            player = null
        }

        // Mulai enforcer volume maksimum berkala.
        handler.removeCallbacks(volumeEnforcer)
        handler.postDelayed(volumeEnforcer, VOLUME_ENFORCE_INTERVAL_MS)
    }

    /** Hentikan alarm dan kembalikan kondisi (enforcer + DND). */
    fun stop() {
        handler.removeCallbacks(volumeEnforcer)
        try {
            player?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            // ignore
        }
        player = null
        restoreDnd()
    }

    /** Naikkan volume stream ALARM ke maksimum. */
    private fun forceMaxAlarmVolume() {
        try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
        } catch (e: Exception) {
            // beberapa perangkat membatasi; abaikan
        }
    }

    /** Nonaktifkan DND sementara jika izin Notification Policy tersedia. */
    private fun relaxDndIfPossible() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                previousDndFilter = nm.currentInterruptionFilter
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    /** Kembalikan setelan DND seperti semula. */
    private fun restoreDnd() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val prev = previousDndFilter ?: return
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(prev)
            }
        } catch (e: Exception) {
            // ignore
        }
        previousDndFilter = null
    }

    companion object {
        private const val VOLUME_ENFORCE_INTERVAL_MS = 500L
    }
}
