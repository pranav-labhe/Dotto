# Preserve common attributes for libraries that use reflection/annotations
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

# AdMob Specific Rules (Safety for GMS)
-keep public class com.google.android.gms.ads.** {
   public *;
}

# Room Database Safety
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity

# Keep your data models if they are ever used with JSON or database reflection
-keep class com.pranav.dotto.infrastructure.persistence.** { *; }
-keep class com.pranav.dotto.domain.model.** { *; }

# Prevent R8 from removing Logcat logs if you need them for debugging release builds
# (Optional: remove this in the final store version if you want absolute privacy)
-dontwarn android.util.Log
