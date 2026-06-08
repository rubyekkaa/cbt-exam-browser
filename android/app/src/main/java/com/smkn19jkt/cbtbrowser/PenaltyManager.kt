package com.smkn19jkt.cbtbrowser

import android.content.Context
import android.content.SharedPreferences

/**
 * Mengelola sistem penalti keluar paksa.
 *
 * Aturan:
 * - Penalti HANYA berlaku jika siswa sudah masuk ke halaman soal
 *   (ditandai lewat [markEnteredExam]).
 * - Keluar paksa (tanpa PIN guru) menambah jumlah pelanggaran.
 * - Keluar via PIN guru/pengawas TIDAK menambah pelanggaran (panggil [clearViolationFlag] saja).
 * - 1..3x pelanggaran  -> KARTU KUNING, durasi naik bertahap (2 -> 3.5 -> 5 menit).
 * - lebih dari 3x      -> KARTU MERAH, durasi 10-15 menit.
 *
 * Status penalti disimpan sebagai waktu kedaluwarsa (epoch millis) sehingga
 * tetap berlaku meskipun aplikasi ditutup paksa atau perangkat di-restart.
 */
class PenaltyManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    enum class CardType { NONE, YELLOW, RED }

    /** Jumlah keluar paksa yang sudah tercatat. */
    val violationCount: Int
        get() = prefs.getInt(KEY_VIOLATION_COUNT, 0)

    /** Waktu (epoch millis) saat penalti berakhir. 0 = tidak ada penalti. */
    private val penaltyUntil: Long
        get() = prefs.getLong(KEY_PENALTY_UNTIL, 0L)

    /** Jenis kartu penalti yang sedang aktif. */
    val activeCardType: CardType
        get() = if (isPenaltyActive()) {
            CardType.valueOf(prefs.getString(KEY_CARD_TYPE, CardType.NONE.name) ?: CardType.NONE.name)
        } else {
            CardType.NONE
        }

    /** True jika masih dalam masa penalti. */
    fun isPenaltyActive(): Boolean = remainingMillis() > 0

    /** Sisa waktu penalti dalam milidetik (0 jika sudah selesai). */
    fun remainingMillis(): Long {
        val diff = penaltyUntil - System.currentTimeMillis()
        return if (diff > 0) diff else 0
    }

    /**
     * Tandai bahwa siswa sudah benar-benar masuk ke halaman soal.
     * Mulai titik ini, keluar paksa akan dihitung sebagai pelanggaran.
     */
    fun markEnteredExam() {
        prefs.edit().putBoolean(KEY_IN_EXAM, true).apply()
    }

    /** Apakah siswa sedang berada di dalam sesi soal. */
    fun isInExam(): Boolean = prefs.getBoolean(KEY_IN_EXAM, false)

    /**
     * Dipanggil saat terdeteksi keluar paksa (tanpa PIN guru).
     * Menambah pelanggaran dan mengaktifkan kartu sesuai jumlahnya.
     *
     * Tidak melakukan apa-apa jika siswa belum masuk ke soal.
     *
     * @return jenis kartu yang diberikan, atau NONE jika tidak ada penalti.
     */
    fun registerForcedExit(): CardType {
        if (!isInExam()) return CardType.NONE

        val newCount = violationCount + 1
        val (cardType, durationMs) = resolvePenalty(newCount)
        val until = System.currentTimeMillis() + durationMs

        prefs.edit()
            .putInt(KEY_VIOLATION_COUNT, newCount)
            .putString(KEY_CARD_TYPE, cardType.name)
            .putLong(KEY_PENALTY_UNTIL, until)
            .apply()

        return cardType
    }

    /**
     * Tentukan jenis kartu + durasi berdasarkan jumlah pelanggaran.
     * Kartu kuning durasinya naik bertahap dari MIN ke MAX seiring pelanggaran 1->3.
     */
    private fun resolvePenalty(count: Int): Pair<CardType, Long> {
        return if (count > ExamConfig.RED_CARD_THRESHOLD) {
            // Kartu merah: durasi acak 10-15 menit.
            val range = ExamConfig.RED_MAX_MS - ExamConfig.RED_MIN_MS
            val duration = ExamConfig.RED_MIN_MS + (Math.random() * range).toLong()
            CardType.RED to duration
        } else {
            // Kartu kuning bertahap. count=1 -> MIN, count=3 -> MAX.
            val steps = ExamConfig.RED_CARD_THRESHOLD - 1 // 2 langkah dari 1..3
            val fraction = if (steps > 0) (count - 1).toFloat() / steps else 0f
            val duration = ExamConfig.YELLOW_MIN_MS +
                ((ExamConfig.YELLOW_MAX_MS - ExamConfig.YELLOW_MIN_MS) * fraction).toLong()
            CardType.YELLOW to duration
        }
    }

    /**
     * Keluar yang sah (via PIN guru). Tidak menambah pelanggaran,
     * cukup tandai bahwa sesi soal sudah berakhir.
     */
    fun clearExamSession() {
        prefs.edit().putBoolean(KEY_IN_EXAM, false).apply()
    }

    /** Reset penalti aktif (dipakai setelah masa penalti selesai). */
    fun clearActivePenalty() {
        prefs.edit()
            .putString(KEY_CARD_TYPE, CardType.NONE.name)
            .putLong(KEY_PENALTY_UNTIL, 0L)
            .apply()
    }

    /** Reset total (untuk admin/guru): hapus semua pelanggaran & penalti. */
    fun resetAll() {
        prefs.edit()
            .putInt(KEY_VIOLATION_COUNT, 0)
            .putString(KEY_CARD_TYPE, CardType.NONE.name)
            .putLong(KEY_PENALTY_UNTIL, 0L)
            .putBoolean(KEY_IN_EXAM, false)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "cbt_penalty"
        private const val KEY_VIOLATION_COUNT = "violation_count"
        private const val KEY_PENALTY_UNTIL = "penalty_until"
        private const val KEY_CARD_TYPE = "card_type"
        private const val KEY_IN_EXAM = "in_exam"
    }
}
