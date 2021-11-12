# there's usage of GSON's @SerializedName
-keepattributes *Annotation*

-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

-keepclassmembers class com.wultra.android.powerauth.networking.data.** { *; }
-keepclassmembers class com.wultra.android.powerauth.networking.error.** { *; }