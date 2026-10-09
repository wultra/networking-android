# Migration from 2.x to 3.0.x (Android)

This guide provides instructions for migrating from **Wultra PowerAuth Networking SDK for Android** version `2.x` to version `3.0.x`.

Version `3.0.x` removes `inline`/`reified` generics from `Api.post()` and its internal helpers. `inline` functions are copied directly into the caller's compiled bytecode, which means:

- Consumers who don't recompile against a new version of this library will crash at runtime: the properties that inline call sites depended on (`baseUrl`, `powerAuthSDK`, `gsonBuilder`, `userAgent`, and several private helper methods) are no longer `@PublishedApi internal` but `private`, so a call site compiled against an older version is no longer binary-compatible and fails with a `NoSuchFieldError`/`NoSuchMethodError`.
- Before this fix, those same internals had to stay exposed as `@PublishedApi internal` just so inline call sites could reach them, permanently widening the library's binary surface.

Removing `inline` fixes both problems, but `reified` generics require `inline` to work (it's how the JVM recovers an erased generic type at runtime). To make up for that, `Endpoint` now carries both the request and response type as explicit `Class<TRequestData>`/`Class<TResponseData>` arguments.

---

### `Endpoint` Declarations Now Require Request/Response `Class`es

Every `EndpointBasic`, `EndpointAuthenticated`, and `EndpointAuthenticatedWithToken` declaration needs two new arguments — the `Class` of its request data and the `Class` of its response data (e.g. `MyRequest::class.java`, `MyResponse::class.java`) — inserted right after the existing identifying parameter(s) and before the optional `e2eeConfiguration`, in that order.

**`EndpointBasic`**

Before (2.x):
```kotlin
val myBasicEndpoint = EndpointBasic<MyRequest, MyResponse>("api/my/endpoint/path", E2EEConfiguration.NOT_ENCRYPTED)
```

After (3.0.x):
```kotlin
val myBasicEndpoint = EndpointBasic("api/my/endpoint/path", MyRequest::class.java, MyResponse::class.java, E2EEConfiguration.NOT_ENCRYPTED)
```

**`EndpointAuthenticated`**

Before (2.x):
```kotlin
val myAuthenticatedEndpoint = EndpointAuthenticated<MyRequest, MyResponse>("api/my/endpoint/path", "/endpoint/uriId", E2EEConfiguration.NOT_ENCRYPTED)
```

After (3.0.x):
```kotlin
val myAuthenticatedEndpoint = EndpointAuthenticated("api/my/endpoint/path", "/endpoint/uriId", MyRequest::class.java, MyResponse::class.java, E2EEConfiguration.NOT_ENCRYPTED)
```

**`EndpointAuthenticatedWithToken`**

Before (2.x):
```kotlin
val myTokenEndpoint = EndpointAuthenticatedWithToken<MyRequest, MyResponse>("api/my/endpoint/path", "possession_universal", E2EEConfiguration.NOT_ENCRYPTED)
```

After (3.0.x):
```kotlin
val myTokenEndpoint = EndpointAuthenticatedWithToken("api/my/endpoint/path", "possession_universal", MyRequest::class.java, MyResponse::class.java, E2EEConfiguration.NOT_ENCRYPTED)
```

<!-- begin box info -->
`Api.post(...)` call sites themselves are **unaffected** — only the `Endpoint` declaration changes.
<!-- end -->

---

### `tokenProvider` Removed

The `tokenProvider` constructor parameter and the `IPowerAuthTokenProvider`/`IPowerAuthTokenListener` interfaces have been removed. Token-authenticated requests now always use `PowerAuthSDK.tokenStore` directly. Remove the `tokenProvider` argument from the `Api` constructor call and delete any custom `IPowerAuthTokenProvider` implementation.

---

### `EndpointAuthenticated` Requests Are Now Serialized By Default

`Api.concurrencyStrategy` is a new property, defaulting to `RequestConcurrencyStrategy.SERIAL_AUTHENTICATED`. This is a **runtime behavior change**, not a source or binary break, so it applies even if you don't otherwise touch your code: `EndpointAuthenticated` requests, which used to always run fully concurrently, are now serialized one at a time via `PowerAuthSDK.getSerialExecutor()`. PowerAuth authentication codes use a counter as a representation of logical time, so the order in which signed requests are validated on the server matters — serializing them keeps that order guaranteed instead of leaving it to chance when multiple are fired at once.

`EndpointBasic` and `EndpointAuthenticatedWithToken` requests are unaffected and keep running concurrently.

If your app depends on `EndpointAuthenticated` requests running concurrently — for example, because you already serialize them yourself — restore the previous behavior:

```kotlin
api.concurrencyStrategy = RequestConcurrencyStrategy.CONCURRENT_ALL
```

See [Creating an HTTP request](Creating-an-HTTP-Request.md#request-concurrency-strategy) for details.

---

### Migration Checklist

- Add `Class<TRequestData>` and `Class<TResponseData>` arguments (e.g. `MyRequest::class.java`, `MyResponse::class.java`) to every `EndpointBasic`, `EndpointAuthenticated`, and `EndpointAuthenticatedWithToken` declaration in your project.
- No changes are needed at `Api.post(...)` call sites.
- Remove the `tokenProvider` argument from the `Api` constructor call and delete any custom `IPowerAuthTokenProvider` implementation.
- If your app relies on `EndpointAuthenticated` requests running concurrently, set `concurrencyStrategy = RequestConcurrencyStrategy.CONCURRENT_ALL` — the new default now serializes them.
