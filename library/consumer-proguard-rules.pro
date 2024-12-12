# there's usage of GSON's @SerializedName
-keepattributes *Annotation*

-keepclasseswithmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

-keepclasseswithmembers, allowobfuscation class com.wultra.android.powerauth.networking.data.** { *; }
-keepclasseswithmembers, allowobfuscation class com.wultra.android.powerauth.networking.error.** { *; }