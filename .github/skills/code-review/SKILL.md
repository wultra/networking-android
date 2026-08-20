# Networking Android code review

Review `networking-android` as the published `com.wultra.android.powerauth:powerauth-networking` AAR. Start by confirming the repository, PR target, PR head, and checked-out commit (`git remote -v`, `git branch --show-current`, `git status --short`, and `git log -1 --oneline`). This repository is normally reviewed against `develop`; treat `release/*` as a release base. Do not assume the local checkout is the PR head.

## Review decision and comments

- Default to **approve**. Request changes only for a concrete, demonstrated regression, security flaw, compatibility break, or missing required release artifact.
- Every finding must identify the changed `path:line`, the observable impact, and a specific correction. Do not speculate.
- Do not make style, formatting, naming, refactoring, or CI-configuration comments. Do not report hypothetical test coverage gaps.
- Never post a review, comment, or other GitHub content without explicit user approval. If content is explicitly approved for posting, prefix every postable comment with `🤖`.
- Check grammar only in changed public README/docs/Javadoc/KDoc, and only when the PR base is **not** `release/*`. Do not comment on private comments.

## Published surface and compatibility

The only Android module is `library`; its namespace is `com.wultra.android.powerauth`, its manifest is `library/src/main/AndroidManifest.xml`, and the consumer rules are `library/consumer-proguard-rules.pro`. Treat public Kotlin declarations beneath `library/src/main/java/com/wultra/android/powerauth/networking/` as binary/source API:

- `Api.kt` (`Api`, `IApiCallResponseListener`, request execution and time synchronization) and `Endpoint.kt`;
- `ECIESInterceptor.kt`, `data/Requests.kt`, `data/Responses.kt`, and `processing/GsonResponseBodyConverter.kt` / `GsonRequestBodyBytes.kt`;
- `tokens/IPowerAuthTokenProvider.kt`, `tokens/IPowerAuthTokenListener.kt`, and `tokens/TokenManager.kt`;
- `ssl/ISSLPinningProvider.kt`, `ssl/SSLValidationStrategy.kt`, and `ssl/TrustAllCertsTrustManager.kt`;
- `error/ApiError.kt`, `ApiErrorCode.kt`, `ApiErrorException.kt`, `ApiHttpException.kt`, `ErrorResponse.kt`, and `ErrorResponseObject.kt`;
- `log/WPNLogger.kt` and `log/WPNLogListener.kt`.

Flag changed method signatures, visibility, nullability, error mapping, serialized field names, exception behavior, or callback threading/number-of-invocations when a consumer can observe the difference. For callback APIs, ensure every terminal request path invokes exactly one success-or-failure completion, errors retain their documented type, cancellation/connection failures do not leave a request pending, and callbacks are not invoked after a terminal result.

## Security and networking focus

Follow changed request bytes through `Api.kt`, `Endpoint.kt`, `ECIESInterceptor.kt`, Gson converters, and token providers. Report only proven cases where signing, ECIES encryption, authorization-token attachment, endpoint method/path/header construction, response parsing, or HTTP error conversion changes incompatibly or exposes data. Treat SSL/pinning changes as security critical: `TrustAllCertsTrustManager` must not become reachable except under the explicitly supported validation strategy, and `ISSLPinningProvider`/`SSLValidationStrategy` must not silently weaken host or certificate validation. Do not allow request payloads, authorization tokens, headers, decrypted responses, or key material to reach `WPNLogger`.

## Version, release, docs, and validation

`library/gradle.properties` declares `VERSION_NAME`; `library/build.gradle.kts` exports it in `BuildConfig`. For a release-to-`develop` change, every declared development version must be exactly `0.0.1-dev`, including `library/gradle.properties` and any changed release metadata. Do not invent a release version.

Public usage belongs in `README.md`; release metadata is `.prepare-release.json` and `scripts/prepare-release.sh`. Require a matching public documentation update and release/changelog entry only when a changed public behavior or API actually warrants one. Build, lint, and test automation is in `.github/workflows/{build,lint,tests}.yml` and `scripts/{build-and-publish,lint,test,prepare-release}.sh`; use it to select evidence, but do not give CI advice.

Relevant regression suites are `library/src/test/java/com/wultra/android/powerauth/networking/` (including SSL, Gson, errors, endpoints, headers, and tokens) and `library/src/androidTest/java/com/wultra/android/powerauth/networking/` (MockWebServer and real-server flows). When validation is needed, use the repository commands documented in `.github/copilot-instructions.md`: `./gradlew clean build`, `./scripts/lint.sh`, and the relevant Gradle test task.
