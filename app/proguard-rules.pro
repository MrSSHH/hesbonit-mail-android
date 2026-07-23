# =========================================================================
# Room Database Keep Rules
# =========================================================================
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.Dao *;
}

-keep class com.invoicemail.hesbonit.*_Impl { *; }

# =========================================================================
# Kotlin Coroutines & Flow
# =========================================================================
-dontwarn kotlinx.coroutines.**
-keepclassmembers class * {
    kotlinx.coroutines.internal.LockFreeLinkedListNode *;
}

# =========================================================================
# Jetpack Compose & Material 3
# =========================================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# =========================================================================
# ExifInterface & FileProvider
# =========================================================================
-keep class androidx.exifinterface.media.ExifInterface { *; }
-keep class androidx.core.content.FileProvider { *; }