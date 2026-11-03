# Room Proguard Keep Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Room Entities & Data Models (Prevents R8 from stripping or renaming columns)
-keepclassmembers class com.anasdev.notificationsaver.data.model.** { *; }
-keep class com.anasdev.notificationsaver.data.model.** { *; }

# Domain Models (Prevents field renaming in Kotlin StateFlow/Domain representations)
-keepclassmembers class com.anasdev.notificationsaver.domain.model.** { *; }
-keep class com.anasdev.notificationsaver.domain.model.** { *; }

# Hilt Proguard Keep Rules
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager
