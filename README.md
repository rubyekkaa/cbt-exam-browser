# CBT Exam Browser - SMKN 19 Jakarta

Aplikasi browser khusus ujian (Computer Based Test) yang mengunci perangkat siswa agar fokus mengerjakan ujian dan meminimalisir kecurangan. Terinspirasi dari [Exambro](https://play.google.com/store/apps/details?id=com.cbt.exam.browser).

## Fitur

| No | Fitur | Android | iOS |
|----|-------|---------|-----|
| 1 | Input URL / Scan QR Code | Ya | Ya |
| 2 | Fullscreen / Kiosk Mode | Ya (Screen Pinning) | Ya (Guided Access) |
| 3 | Blokir Screenshot | Ya (FLAG_SECURE) | Ya (Secure TextField) |
| 4 | Sembunyikan URL Server | Ya | Ya |
| 5 | Timer/Jam | Ya | Ya |
| 6 | Zoom In/Out | Ya | Ya |
| 7 | Navigasi (Back/Forward/Refresh) | Ya | Ya |
| 8 | Tombol Exit dengan Konfirmasi | Ya (+ PIN opsional) | Ya |
| 9 | Custom User Agent "cbt-exam-browser" | Ya | Ya |
| 10 | Blokir Buka Tab/Window Baru | Ya | Ya |
| 11 | Blokir Tombol Back/Home/Recent | Ya | Ya |
| 12 | Riwayat URL | Ya | Ya |

## Screenshot

### Halaman Utama
- Input URL test center
- Tombol Scan QR Code
- Toggle Custom User Agent
- Tombol Mulai Ujian

### Mode Ujian
- Fullscreen tanpa status bar
- Toolbar: [Back] [Forward] [Refresh] — [Zoom-] [Zoom+] [Timer] [Exit]
- WebView menampilkan halaman ujian

## Cara Install

### Android (APK)

**Opsi 1: Download dari GitHub Actions**
1. Buka tab [Actions](../../actions) di repository ini
2. Klik workflow "Build Android APK" terbaru
3. Download artifact `cbt-exam-browser-debug` atau `cbt-exam-browser-release`
4. Install APK di perangkat Android

**Opsi 2: Build sendiri**
```bash
cd android
./gradlew assembleDebug
# APK ada di: android/app/build/outputs/apk/debug/
```

### iOS
1. Buka folder `ios/` di Xcode (memerlukan Mac)
2. Pilih target CBTExamBrowser
3. Build dan run di perangkat/simulator

## Cara Pakai

### Untuk Guru/Admin
1. Buka aplikasi CBT Exam Browser
2. Masukkan URL test center sekolah (contoh: `http://116.197.135.140/testcenter`)
3. Atau scan QR Code yang berisi URL
4. Aktifkan "Custom User Agent" jika server ujian memerlukan
5. (Opsional) Atur PIN keluar di Pengaturan agar siswa tidak bisa keluar tanpa izin
6. Tekan "MULAI UJIAN"

### Untuk Siswa
1. Masukkan URL yang diberikan guru
2. Tekan "MULAI UJIAN"
3. Kerjakan ujian
4. Untuk keluar, tekan tombol Exit (ikon X merah) di toolbar atas

### Custom User Agent
Untuk memastikan server ujian hanya bisa diakses dari CBT Exam Browser, tambahkan pengecekan user agent di server:

```php
// Contoh pengecekan di PHP
if (strpos($_SERVER['HTTP_USER_AGENT'], 'cbt-exam-browser') === false) {
    die('Akses hanya diizinkan dari CBT Exam Browser');
}
```

## Pengaturan

Tersedia di menu Pengaturan:
- **PIN Keluar**: Set PIN 4-6 digit agar siswa tidak bisa keluar tanpa izin guru
- **Blokir Screenshot**: Mencegah siswa mengambil tangkapan layar
- **Blokir Tombol Kembali**: Mencegah siswa keluar via tombol back
- **Mode Kiosk**: Mengunci layar agar siswa tidak bisa buka app lain
- **Custom User Agent**: Gunakan user agent khusus "cbt-exam-browser"
- **URL Default**: Set URL default yang otomatis terisi saat buka app

## Struktur Project

```
cbt-exam-browser/
├── android/                          # Project Android (Kotlin)
│   ├── app/src/main/
│   │   ├── java/.../cbtbrowser/
│   │   │   ├── MainActivity.kt       # Halaman utama
│   │   │   ├── ExamActivity.kt       # Mode ujian (WebView + kiosk)
│   │   │   └── SettingsActivity.kt   # Halaman pengaturan
│   │   ├── res/layout/               # Layout XML
│   │   ├── res/values/               # Colors, strings, themes
│   │   ├── res/drawable/             # Icons
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── gradle/
├── ios/                              # Project iOS (Swift)
│   └── CBTExamBrowser/
│       ├── AppDelegate.swift
│       ├── SceneDelegate.swift
│       ├── MainViewController.swift   # Halaman utama
│       ├── ExamViewController.swift   # Mode ujian (WKWebView)
│       └── Info.plist
├── .github/workflows/
│   └── build-android.yml             # CI/CD auto-build APK
└── README.md
```

## Requirements

### Android
- Android 7.0 (API 24) ke atas
- Koneksi internet

### iOS
- iOS 14.0 ke atas
- Koneksi internet

### Build Requirements
- Android: JDK 17, Android SDK 34
- iOS: Xcode 15+, macOS

## Tech Stack

- **Android**: Kotlin, AndroidX, Material Design, WebView, ZXing (QR Scanner)
- **iOS**: Swift, UIKit, WKWebView

## Lisensi

MIT License - Bebas digunakan untuk keperluan pendidikan.
