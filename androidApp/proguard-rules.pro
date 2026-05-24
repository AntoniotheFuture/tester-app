# Kotlin Multiplatform
-keepattributes *Annotation*

# Compose
-dontwarn androidx.compose.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.antoniofuture.testerapp.**$$serializer { *; }
-keepclassmembers class com.antoniofuture.testerapp.** {
    *** Companion;
}
-keepclasseswithmembers class com.antoniofuture.testerapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Android
-keep class * extends android.app.Activity
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
