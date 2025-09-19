# there's usage of GSON's @SerializedName
-keepattributes *Annotation*

-keepclasseswithmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# keeps fields in the class
-keepclassmembers class com.wultra.android.powerauth.networking.data.** { <fields>; }
# handle R8 full mode optimizations
-keep, allowobfuscation class com.wultra.android.powerauth.networking.data.**

# keeps fields in the class
-keepclassmembers class com.wultra.android.powerauth.networking.error.** { <fields>; }
# handle R8 full mode optimizations
-keep, allowobfuscation class com.wultra.android.powerauth.networking.error.**