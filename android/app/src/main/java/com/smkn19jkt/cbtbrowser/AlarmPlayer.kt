package com.smkn19jkt.cbtbrowser

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager

/**
 * Pemutar alarm pelanggaran. Memakai file kustom di res/raw/alarm_pelanggaran.
 * Jika file kustom gagal dimuat, jatuh ke nada alarm bawaan sistem.
 */
object AlarmPlayer {

    /**
     * Mainkan alarm. Pemanggil bertanggung jawab memanggil [MediaPlayer.release]
     * pada objek yang dikembalikan saat sudah tidak dipakai.
     *
     * @param looping true untuk memutar berulang (mis. layar penalti).
     */
    fun play(context: Context, looping: Boolean): MediaPlayer? {
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
}
