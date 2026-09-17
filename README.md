# PowerAuth Networking SDK for Android

<!-- begin remove -->
<p align="center"><img src="docs/intro.jpg" alt="Wultra Digital Onboarding for Android" width="100%" /></p>

[![build](https://github.com/wultra/networking-android/actions/workflows/build.yml/badge.svg)](https://github.com/wultra/networking-android/actions/workflows/build.yml)
[![maven](https://img.shields.io/maven-central/v/com.wultra.android.powerauth/powerauth-networking)](https://mvnrepository.com/artifact/com.wultra.android.powerauth/powerauth-networking)
![date](https://img.shields.io/github/release-date/wultra/networking-android)
[![license](https://img.shields.io/github/license/wultra/networking-android)](LICENSE)

__Wultra PowerAuth Networking__ (WPN) is a high-level SDK built on top of our [PowerAuth SDK](https://github.com/wultra/powerauth-mobile-sdk) that enables request signing and encryption.
<!-- end -->

## Introduction

You can imagine the purpose of this SDK as an __HTTP layer (client) that enables request signing and encryption__ via PowerAuth SDK based on its recommended implementation.

We use this SDK in our other open-source projects that you can take inspiration for example in:

- [Digital Onboarding SDK](https://github.com/wultra/digital-onboarding-android/blob/develop/library/src/main/java/com/wultra/android/digitalonboarding/networking/CustomerOnboardingApi.kt#L38)
- [Mobile Token SDK](https://github.com/wultra/mtoken-sdk-android/blob/develop/library/src/main/java/com/wultra/android/mtokensdk/api/operation/OperationApi.kt)

## Documentation

The documentation is available at the [Wultra Developer Portal](https://developers.wultra.com/components/networking-android) or inside the [docs folder](docs).

## License

All sources are licensed using the Apache 2.0 license. You can use them with no restrictions. If you are using this library, please let us know. We will be happy to share and promote your project.

## Contact

If you need any assistance, do not hesitate to drop us a line at [hello@wultra.com](mailto:hello@wultra.com) or our official [wultra.com/discord](https://wultra.com/discord) channel.

### Security Disclosure

If you believe you have identified a security vulnerability with this SDK, you should report it as soon as possible via email to [support@wultra.com](mailto:support@wultra.com). Please do not post it to a public issue tracker.
