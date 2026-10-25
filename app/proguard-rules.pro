# Room Proguard Keep Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt Proguard Keep Rules
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager
