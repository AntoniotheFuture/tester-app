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
-keep,includedescriptorclasses class com.apptester.**$$serializer { *; }
-keepclassmembers class com.apptester.** {
    *** Companion;
}
-keepclasseswithmembers class com.apptester.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Android
-keep class * extends android.app.Activity
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
