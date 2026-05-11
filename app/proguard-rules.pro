# Default Android optimize rules apply.
# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# Room
-keep class androidx.room.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.fatimagames.app.**$$serializer { *; }
-keepclassmembers class com.fatimagames.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.fatimagames.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
