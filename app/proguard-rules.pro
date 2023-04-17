# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate
#-renamesourcefileattribute SourceFile
#-keepattributes SourceFile,LineNumberTable

-dontwarn com.arialyy.aria.**
-keep class com.arialyy.aria.**{*;}
-keep class **$$DownloadListenerProxy{ *; }
-keep class **$$UploadListenerProxy{ *; }
-keep class **$$DownloadGroupListenerProxy{ *; }
-keep class **$$DGSubListenerProxy{ *; }
-keep class com.hzy.lib7z.**{*;}
-keepclasseswithmembernames class * {
    @Download.* <methods>;
    @Upload.* <methods>;
    @DownloadGroup.* <methods>;
}

# LocalePlugin 混淆规则
-keep class com.mallotec.reb.localeplugin.** { *; }
-dontwarn com.mallotec.reb.localeplugin.**

-keep class com.termux.zerocore.ftp.new_ftp.** { *; }
-dontwarn com.termux.zerocore.ftp.new_ftp.**

-keep class org.apache.mina.** { *; }
-dontwarn org.apache.mina.**

-keep class org.apache.ftpserver.** { *; }
-dontwarn org.apache.ftpserver.**
