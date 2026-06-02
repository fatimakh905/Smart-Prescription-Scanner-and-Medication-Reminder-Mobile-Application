# MediRemind ProGuard Rules

# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Gson ──────────────────────────────────────────────────────────────────────
# Keep all model classes used with Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.mediremind.models.** { *; }

# ── Glide ─────────────────────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}

# ── AndroidX & Material ───────────────────────────────────────────────────────
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# ── SQLite helper ─────────────────────────────────────────────────────────────
-keep class com.mediremind.database.** { *; }

# ── Receivers and Services ────────────────────────────────────────────────────
-keep class com.mediremind.receivers.** { *; }
-keep class com.mediremind.notifications.** { *; }

# ── BuildConfig ───────────────────────────────────────────────────────────────
-keep class com.mediremind.BuildConfig { *; }
