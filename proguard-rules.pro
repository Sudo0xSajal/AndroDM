# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Room entity classes
-keep class com.vmate.downloader.domain.models.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
