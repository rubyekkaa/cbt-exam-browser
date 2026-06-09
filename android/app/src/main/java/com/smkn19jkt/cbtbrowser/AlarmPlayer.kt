package com.smkn19jkt.cbtbrowser

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager

/**
 * Pemutar alarm pelanggaran. Memakai file kustom di res/raw/alarm_pelanggaran.
 * Jika file kustom gagal dimuat, jatuh ke nada alarm bawaan sistem.
 *
 * Volume alarm dipaksa ke MAKSIMUM agar terdengar keras (efek jera bagi siswa
 * yang memaksa keluar/curang).
 */
object AlarmPlayer {

    /**
     * Mainkan alarm. Pemanggil bertanggung jawab memanggil [MediaPlayer.release]
     * pada objek yang dikembalikan saat sudah tidak dipakai.
     *
     * @param looping true untuk memutar berulang (mis. layar penalti).
     */
    fun play(context: Context, looping: Boolean): MediaPlayer? {
        // Paksa volume stream alarm ke maksimum.
        forceMaxAlarmVolume(context)

        return try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val player = MediaPlayer.create(
                context,
                R.raw.alarm_pelanggaran
            ) ?: MediaPlayer.create(
                context,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            )

            player?.apply {
                setAudioAttributes(attrs)
                setVolume(1.0f, 1.0f)
                isLooping = looping
                setOnCompletionListener {
                    if (!looping) {
                        it.release()
                    }
                }
                start()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Naikkan volume stream alarm perangkat ke level maksimum. */
    private fun forceMaxAlarmVolume(context: Context) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
        } catch (e: Exception) {
            // ignore (beberapa perangkat membatasi perubahan volume)
        }
    }
}

