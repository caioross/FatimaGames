# Default Android optimize rules apply.
# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# Room
-keep class androidx.room.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, RuntimeVisibleAnnotations, AnnotationDefault
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.fatimagames.app.**$$serializer { *; }
-keepclassmembers class com.fatimagames.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.fatimagames.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Mantém as classes @Serializable e seus campos (estados de jogo salvos)
-keep @kotlinx.serialization.Serializable class com.fatimagames.app.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.fatimagames.app.** {
    <fields>;
}
# Enums (serializados por nome) — preserva values()/valueOf()
-keepclassmembers enum com.fatimagames.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
