package com.smkn19jkt.cbtbrowser

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smkn19jkt.cbtbrowser.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var penaltyManager: PenaltyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("cbt_prefs", Context.MODE_PRIVATE)
        penaltyManager = PenaltyManager(this)

        setupToolbar()
        loadSettings()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Pengaturan"
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadSettings() {
        binding.etExitPin.setText(prefs.getString("exit_pin", "") ?: "")
        binding.switchBlockScreenshot.isChecked = prefs.getBoolean("block_screenshot", true)
        binding.switchBlockBack.isChecked = prefs.getBoolean("block_back_button", true)
        binding.switchKioskMode.isChecked = prefs.getBoolean("enable_kiosk", true)
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener { saveSettings() }

        binding.btnResetPenalty.setOnClickListener {
            penaltyManager.resetAll()
            Toast.makeText(this, "Penalti siswa direset", Toast.LENGTH_SHORT).show()
        }

        binding.btnResetSettings.setOnClickListener {
            prefs.edit().clear().apply()
            penaltyManager.resetAll()
            loadSettings()
            Toast.makeText(this, "Pengaturan direset", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings() {
        prefs.edit().apply {
            putString("exit_pin", binding.etExitPin.text.toString().trim())
            putBoolean("block_screenshot", binding.switchBlockScreenshot.isChecked)
            putBoolean("block_back_button", binding.switchBlockBack.isChecked)
            putBoolean("enable_kiosk", binding.switchKioskMode.isChecked)
            apply()
        }
        Toast.makeText(this, "Pengaturan tersimpan", Toast.LENGTH_SHORT).show()
        finish()
    }
}
