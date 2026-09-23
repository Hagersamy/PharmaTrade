# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Preserve line numbers so Crashlytics can show real stack traces against the uploaded mapping file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization generates a $serializer companion for every @Serializable class at compile
# time and looks it up by name at runtime — R8 can rename/strip that link since nothing else
# references it directly, which breaks JSON decoding silently (wrong/empty fields) rather than
# crashing. Keeping serializable classes and their generated members avoids that.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.pharmatrade.**$$serializer { *; }
-keepclassmembers class com.pharmatrade.** {
    *** Companion;
}
-keepclasseswithmembers class com.pharmatrade.** {
    kotlinx.serialization.KSerializer serializer(...);
}