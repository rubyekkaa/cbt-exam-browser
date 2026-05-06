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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("cbt_prefs", Context.MODE_PRIVATE)

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
        val exitPin = prefs.getString("exit_pin", "") ?: ""
        binding.etExitPin.setText(exitPin)

        val customUserAgent = prefs.getBoolean("custom_user_agent", true)
        binding.switchCustomUserAgent.isChecked = customUserAgent

        val blockScreenshot = prefs.getBoolean("block_screenshot", true)
        binding.switchBlockScreenshot.isChecked = blockScreenshot

        val blockBackButton = prefs.getBoolean("block_back_button", true)
        binding.switchBlockBack.isChecked = blockBackButton

        val enableKiosk = prefs.getBoolean("enable_kiosk", true)
        binding.switchKioskMode.isChecked = enableKiosk

        val defaultUrl = prefs.getString("default_url", "") ?: ""
        binding.etDefaultUrl.setText(defaultUrl)
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        binding.btnClearHistory.setOnClickListener {
            prefs.edit().remove("recent_urls").apply()
            Toast.makeText(this, "Riwayat URL dihapus", Toast.LENGTH_SHORT).show()
        }

        binding.btnResetSettings.setOnClickListener {
            prefs.edit().clear().apply()
            loadSettings()
            Toast.makeText(this, "Pengaturan direset", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings() {
        prefs.edit().apply {
            putString("exit_pin", binding.etExitPin.text.toString().trim())
            putBoolean("custom_user_agent", binding.switchCustomUserAgent.isChecked)
            putBoolean("block_screenshot", binding.switchBlockScreenshot.isChecked)
            putBoolean("block_back_button", binding.switchBlockBack.isChecked)
            putBoolean("enable_kiosk", binding.switchKioskMode.isChecked)
            putString("default_url", binding.etDefaultUrl.text.toString().trim())
            apply()
        }
        Toast.makeText(this, "Pengaturan tersimpan", Toast.LENGTH_SHORT).show()
        finish()
    }
}
