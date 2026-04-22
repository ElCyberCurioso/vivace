-keep class com.guitarchords.app.data.** { *; }
-keep class com.guitarchords.app.chords.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.guitarchords.app.**$$serializer { *; }
-keepclassmembers class com.guitarchords.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.guitarchords.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
