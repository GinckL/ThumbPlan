# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Room
-keep class com.ginckl.thumbplan.data.local.entity.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Hilt
-keep,allowobfuscation,allowshrinking @interface dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.managers.FragmentComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.managers.ServiceComponentManager { *; }
# Room: entities + DAO result classes are read by name/reflection-free generated code, but keep for safety
-keep class com.ginckl.thumbplan.data.local.dao.CoinHistoryItem { *; }

# WorkManager + Hilt workers are instantiated through the Hilt worker factory
-keep class * extends androidx.work.ListenableWorker { *; }
-keep @androidx.hilt.work.HiltWorker class * { *; }

# Manifest components
-keep class com.ginckl.thumbplan.services.alarm.** { *; }

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
