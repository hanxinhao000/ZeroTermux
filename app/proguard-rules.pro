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
    @com.arialyy.annotations.Download.* <methods>;
    @com.arialyy.annotations.Upload.* <methods>;
    @com.arialyy.annotations.DownloadGroup.* <methods>;
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

# 保留自定义工具类
-keep class com.your.package.ContextUtil { *; }

# 忽略 AgentWeb 对支付宝 SDK 的引用
-dontwarn com.alipay.sdk.**
-dontwarn com.alipay.api.**

# 忽略 Log4j 对 Java Beans 的引用 (Android 不支持 java.beans)
-dontwarn java.beans.**

# 忽略 ZXing 引用 (防止 BGAQRCode 报错)
-dontwarn com.google.zxing.**

# 忽略 OkGo 可能引用的类
-dontwarn com.lzy.okgo.**

# 保护实体类不被混淆，否则 Gson 无法将 JSON 映射回对象
-keep class com.termux.zerocore.bean.** { *; }

# 保护泛型信息和注解（Gson 依赖这些）
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Termux-X11 使用 app_process 启动，非常依赖类名和反射，必须完全保留
-keep class com.termux.x11.** { *; }
-dontwarn com.termux.x11.**

# 保护 Android 系统隐藏 API (ActivityThread)
# CmdEntryPoint 需要通过反射调用这些类来获取 Context
-keep class android.app.ActivityThread {
    public static android.app.ActivityThread currentActivityThread();
    public android.app.ContextImpl getSystemContext();
    public android.app.Application getApplication();
    public static android.app.Application currentApplication();
}

# 保护 ContextImpl (Context 的具体实现)
-keep class android.app.ContextImpl {
    *;
}

# 保护 Looper 和 Handler (堆栈中涉及到了消息循环)
-keep class android.os.Looper {
    public static void loop();
    public static android.os.Looper myLooper();
    public static void prepare();
}

-keep class android.os.Handler {
    *;
}

# 保护 ServiceManager (有些版本可能需要)
-keep class android.os.ServiceManager {
    public static android.os.IBinder getService(java.lang.String);
}

# 保护 IWindowSession 等底层图形接口 (解决 gralloc-mapper warning 隐患)
-keep class android.view.** { *; }
-dontwarn android.view.**
