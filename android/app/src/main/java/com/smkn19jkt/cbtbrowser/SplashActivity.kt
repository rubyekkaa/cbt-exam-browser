package com.smkn19jkt.cbtbrowser

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

/**
 * Layar pembuka (splash) yang menampilkan logo sekolah saat aplikasi dibuka,
 * lalu otomatis masuk ke halaman utama.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, SPLASH_DURATION_MS)
    }

    companion object {
        private const val SPLASH_DURATION_MS = 1500L
    }
}
