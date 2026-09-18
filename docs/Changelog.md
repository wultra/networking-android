# Changelog

## TBA
- Added `Api.concurrencyStrategy` (of type `RequestConcurrencyStrategy`) to control how requests authenticated with a PowerAuth authentication code (`EndpointAuthenticated`) are dispatched. Its default value, `SERIAL_AUTHENTICATED`, serializes these requests via `PowerAuthSDK.getSerialExecutor()` so the underlying signature counter always reaches the server in the order it was assigned. **This changes the default runtime behavior** for existing apps: `EndpointAuthenticated` requests that used to run fully concurrently now run one at a time. To restore the previous behavior, set `concurrencyStrategy = RequestConcurrencyStrategy.CONCURRENT_ALL`. `EndpointBasic` and `EndpointAuthenticatedWithToken` requests are unaffected and always dispatched concurrently. [(#49)](https://github.com/wultra/networking-android/issues/49)
    - [Migration guide](Migration-3.0.md)
- Removed `inline`/`reified` generics from `Api.post()` and its internal helpers to fix ABI compatibility issues (internal implementation details no longer leak into consumer bytecode). `Endpoint` now carries an explicit `Class<TResponseData>` token instead. [(#98)](https://github.com/wultra/networking-android/issues/98)
    - [Migration guide](Migration-3.0.md)
- Token-authenticated requests now use `PowerAuthSDK.tokenStore` directly. The `tokenProvider` constructor parameter and the `IPowerAuthTokenProvider`/`IPowerAuthTokenListener` interfaces are deprecated and retained only for source/binary compatibility; they will be removed in a future major version. [(#97)](https://github.com/wultra/networking-android/pull/97)

## 2.0.0

- Upgraded PowerAuth SDK to `2.0.0`
- Bumped AGP to `8.9.1`
- Upgraded targetSdk to `36`, raise minSdk to `23`, build tools to `36.0.0`
- Updated request signing and E2EE encryption/decryption to match PowerAuth SDK 2.0 APIs (`CoreEncryptor` replaces `EciesEncryptor`)
- Improved error handling — signing and encryption failures are now properly reported via `onFailure` callback

## 1.5.1

- Fixed time synchronization for token-based authorization headers

## 1.5.0

- Upgrade PowerAuth SDK to `1.9.0+`
- Deprecated default SSL behavior in favor of the "system" name

## 1.4.0

- Logging improvements

## 1.3.1

- Added new `ApiErrorCodes`
- Update targetSdk to `33`; updated dependencies
- Update AGP to `8.1.4`
- Raise minSdk to `21`
- Improved JavaDoc
- Introduced this documentation

## 1.3.0

- Upgrade PowerAuth SDK to `1.8.0`
