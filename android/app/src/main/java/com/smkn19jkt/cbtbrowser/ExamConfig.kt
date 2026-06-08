package com.smkn19jkt.cbtbrowser

/**
 * Konfigurasi terpusat untuk CBT Exam Browser.
 *
 * URL ujian DITANAM di sini. Siswa hanya bisa MEMILIH dari daftar ini,
 * tidak bisa mengetik URL baru atau scan QR.
 *
 * Untuk mengubah URL: edit [EXAM_SERVERS] di bawah lalu build ulang APK.
 */
object ExamConfig {

    /**
     * Daftar server ujian yang ditanam.
     * Tambah/kurangi item di sini sesuai kebutuhan (saat ini 2 URL).
     */
    val EXAM_SERVERS: List<ExamServer> = listOf(
        ExamServer(
            label = "Server Ujian 1",
            url = "http://116.197.135.140/testcenter"
        ),
        ExamServer(
            label = "Server Ujian 2",
            url = "http://116.197.135.141/tc"
        )
    )

    // ---- Konfigurasi penalti (durasi dalam milidetik) ----

    /** Penalti diberikan HANYA jika siswa sudah masuk ke halaman soal. */

    // Kartu kuning - durasi naik bertahap sesuai jumlah pelanggaran (1..3 kali).
    const val YELLOW_MIN_MS = 2 * 60 * 1000L   // 2 menit
    const val YELLOW_MAX_MS = 5 * 60 * 1000L   // 5 menit

    // Kartu merah - keluar paksa lebih dari 3 kali.
    const val RED_MIN_MS = 10 * 60 * 1000L     // 10 menit
    const val RED_MAX_MS = 15 * 60 * 1000L     // 15 menit

    /** Ambang batas: lebih dari nilai ini -> kartu merah. */
    const val RED_CARD_THRESHOLD = 3
}

data class ExamServer(
    val label: String,
    val url: String
)
