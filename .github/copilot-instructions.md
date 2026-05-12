# Copilot Instructions for networking-android

## Project Overview

This is **Wultra PowerAuth Networking (WPN)** — an Android library (AAR) that provides a high-level HTTP client layer on top of PowerAuth SDK for request signing and end-to-end encryption. It is published to Maven Central as `com.wultra.android.powerauth:powerauth-networking`.

## Build & Lint

```bash
# Build the library (requires Java 17)
./gradlew clean build

# Run ktlint only
./scripts/lint.sh
```

There are no unit tests in this project. CI runs build on macOS and lint on Ubuntu.

## Architecture

Single-module Gradle project (`library/`) with source in `library/src/main/java/com/wultra/android/powerauth/networking/`.

### Core classes

- **`Api`** — Abstract base class that consumers inherit to create service APIs. Provides `post()` methods for three endpoint types: basic, signed, and token-signed. Uses OkHttp for HTTP and Gson for serialization.
- **`Endpoint`** hierarchy — `EndpointBasic`, `EndpointSigned`, `EndpointSignedWithToken` define endpoint metadata (URL path, uriId/tokenName, E2EE config).
- **`BaseRequest` / `ObjectRequest<T>`** — All request bodies extend `BaseRequest`. Payloads use `ObjectRequest` with a `requestObject` field.
- **`StatusResponse` / `ObjectResponse<T>`** — All responses extend `StatusResponse` (has `status: OK|ERROR`). Payloads use `ObjectResponse` with a `responseObject` field.
- **`ApiError` / `ApiErrorCode`** — Error model. `ApiError` wraps exceptions; `ApiErrorCode` enumerates known server error codes.
- **`WPNLogger`** — Logging utility with configurable verbosity levels.

### Key packages

- `tokens/` — Token management for `EndpointSignedWithToken` endpoints
- `error/` — Error types and response parsing
- `data/` — Request/response base classes
- `processing/` — Gson serialization helpers
- `ssl/` — SSL validation strategies
- `utils/` — App/device info utilities

## Conventions

- **Kotlin** with JVM target 11, min SDK 21, compile SDK 35.
- **Ktlint 0.49.1** is enforced — runs automatically before build. Config is in `.editorconfig` (trailing commas disabled, wildcard imports allowed, colon-spacing rule disabled).
- Build constants (SDK versions, Android API levels) are centralized in `buildSrc/src/main/kotlin/Constants.kt`.
- Uses `@SerializedName` annotations for all JSON fields (Gson).
- The library uses `compileOnly` for PowerAuth SDK and `rest-model-base` — consumers provide these at runtime.
- All source files include an Apache 2.0 copyright header (Wultra s.r.o.).
- Asynchronous results use the `IApiCallResponseListener<T>` callback interface (not coroutines).
