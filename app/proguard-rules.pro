# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.tierone.manager.**$$serializer { *; }
-keepclassmembers class com.tierone.manager.** {
    *** Companion;
}
-keepclasseswithmembers class com.tierone.manager.** {
    kotlinx.serialization.KSerializer serializer(...);
}
