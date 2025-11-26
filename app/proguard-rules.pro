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

# 保留 CmdEntryPoint 及其依赖
-keep class com.termux.x11.CmdEntryPoint { *; }
-keep class com.termux.x11.Loader { *; }

# 保留所有native方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留自定义异常类
-keep public class * extends java.lang.Exception

# 保留ActivityThread相关反射类
-keep class android.app.ActivityThread { *; }
-keepclassmembers class android.app.ActivityThread {
    public static *** currentActivityThread();
    public *** getSystemContext();
}

# 忽略无害警告 (减少构建噪音)
-dontwarn com.alipay.sdk.**
-dontwarn com.alipay.api.**
-dontwarn java.beans.**
-dontwarn com.google.zxing.**
-dontwarn com.lzy.okgo.**
-dontwarn cn.bingoogolapple.photopicker.**
-dontwarn com.draggable.library.extension.**

# 保护签名/注解信息（对 Gson/反射/注解框架关键）
-keepattributes Signature
-keepattributes *Annotation*

# 保护数据模型
-keep class com.termux.zerocore.bean.** { *; }

# 保护 TypeToken 的匿名子类，防止 R8 移除泛型信息导致运行时崩溃
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
