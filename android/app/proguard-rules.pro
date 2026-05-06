# Keep WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep ZXing classes
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.** { *; }
