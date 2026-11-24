# ==============================================================================
# KronoGram / Telegram R8 & ProGuard Rules (aligned with official DrKLO/Telegram)
# ==============================================================================

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions,SourceFile,LineNumberTable

# Strip verbose/debug Android Log calls in Release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Keep @Keep annotated classes and members
-keep @interface androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <init>(...);
}

# Google Play Services & Firebase
-keep public class com.google.android.gms.* { public *; }
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

# Manifest-referenced Cast OptionsProvider (meta-data class name string)
-keep class org.telegram.messenger.chromecast.ChromecastOptionsProvider { *; }

# WebRTC & VoIP native JNI bridges
-keep class org.webrtc.** { *; }
-keep class org.telegram.messenger.voip.** { *; }

# Native JNI methods & official Telegram core messenger packages (single-star)
-keepclasseswithmembers,includedescriptorclasses class org.telegram.** {
    native <methods>;
}
-keep class org.telegram.messenger.* { *; }
-keep class org.telegram.messenger.camera.* { *; }
-keep class org.telegram.messenger.secretmedia.* { *; }
-keep class org.telegram.messenger.support.* { *; }
-keep class org.telegram.messenger.time.* { *; }
-keep class org.telegram.messenger.video.* { *; }

# SQLite & TGNet JNI entry points (without keeping all unused TLRPC classes)
-keep class org.telegram.SQLite.** { *; }
-keep class org.telegram.tgnet.ConnectionsManager { *; }
-keep class org.telegram.tgnet.NativeByteBuffer { *; }
-keep class org.telegram.tgnet.RequestTimeDelegate { *; }
-keep class org.telegram.tgnet.RequestDelegate { *; }
-keep class org.telegram.tgnet.RequestDelegateTimestamp { *; }
-keep class org.telegram.tgnet.WriteToSocketDelegate { *; }
-keep class org.telegram.tgnet.QuickAckDelegate { *; }

# Media3 FFmpeg JNI bridges (supplementing module consumer rules)
-keep class androidx.media3.decoder.VideoDecoderOutputBuffer { *; }
-keep class androidx.media3.decoder.ffmpeg.FfmpegAudioDecoder { *; }
-keep class androidx.media3.decoder.ffmpeg.FfmpegLibrary { *; }
-keep class androidx.mediarouter.app.MediaRouteButton { *; }

# Verified UI & Utils JNI bridges (instead of keeping all of ui.** / utils.**)
-keep class org.telegram.ui.Stories.recorder.FfmpegAudioWaveformLoader { *; }
-keep class org.telegram.ui.Components.RLottieNative { *; }
-keep class org.telegram.utils.code.highlight.PrismaHighlighter { *; }
-keep class org.telegram.utils.twidget.WidgetSandboxNative { *; }
-keepclasseswithmembers,includedescriptorclasses class org.telegram.utils.twidget.WidgetClientNative {
    native <methods>;
}
-keep class org.telegram.utils.twidget.WidgetSandboxBackend { *; }
-keep class org.telegram.utils.twidget.WidgetEngineBackend { *; }
-keep class org.telegram.utils.recorder.audio.AudioRecorderNative { *; }
-keep class org.telegram.utils.recorder.audio.AudioNoiseSuppressor { *; }

# WebView JavascriptInterfaces
-keepclassmembers class ** {
    @android.webkit.JavascriptInterface <methods>;
}

# ML Kit Language ID JNI
-keep class com.google.mlkit.nl.languageid.internal.LanguageIdentificationJni { *; }

# JLatexMath (macro/atom classes loaded reflectively via Class.forName)
-keep class io.nano.tex.** { *; }
-keep class org.scilab.forge.jlatexmath.** { *; }
-keep class ru.noties.jlatexmath.** { *; }

# Custom Views instantiated or animated via reflection / ObjectAnimator
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    void set*(***);
    *** get*();
}

# Vosk & JNA (Offline Voice-to-Text)
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }

# Android Auto / Car App Library
-keep class org.telegram.messenger.car.** { *; }
-keep class androidx.car.app.** { *; }

# Suppress warnings from optional / compileOnly / third-party transitive references
-dontwarn **
