package com.smkn19jkt.cbtbrowser

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Device Admin / Device Owner receiver.
 *
 * Jika aplikasi dijadikan Device Owner via ADB (sekali setup per HP):
 *
 *   adb shell dpm set-device-owner com.smkn19jkt.cbtbrowser/.AdminReceiver
 *
 * maka kiosk mode (startLockTask) menjadi PENUH: tombol Home/Recent
 * benar-benar mati, status bar terkunci, dan overlay aplikasi lain diblok.
 *
 * Tanpa Device Owner, aplikasi tetap berjalan secara best-effort
 * memakai screen pinning bawaan Android.
 */
class AdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }
}
