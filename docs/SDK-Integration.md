# SDK Integration

## Requirements

- Android 6.0+ (API level 23+)
- [PowerAuth Mobile SDK](https://github.com/wultra/powerauth-mobile-sdk) needs to be implemented in your project

## Gradle

To use the SDK in your Android application, include the following dependency to your Gradle file.

```groovy
repositories {
    mavenCentral() // if not defined elsewhere...
}

// WPN_VERSION is your wanted target version
implementation "com.wultra.android.powerauth:powerauth-networking:${WPN_VERSION}"
```

## Open Source Code

The code of the library is open source, and you can freely browse it in our GitHub at [https://github.com/wultra/networking-android](https://github.com/wultra/networking-android/#docucheck-keep-link)
