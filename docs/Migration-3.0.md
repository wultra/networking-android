# Migration from 2.x to 3.0.x (Android)

This guide provides instructions for migrating from **Wultra PowerAuth Networking SDK for Android** version `2.x` to version `3.0.x`.

Version `3.0.x` removes `inline`/`reified` generics from `Api.post()` and its internal helpers. `inline` functions are copied directly into the caller's compiled bytecode, which means:

- Consumers who don't recompile against a new version of this library keep running whatever implementation was inlined into their app at their last compile — a fix or behavior change shipped in a minor release would silently not apply to them, breaking the usual binary-compatibility contract.
- A number of library internals (`baseUrl`, `powerAuthSDK`, `gsonBuilder`, `tokenProvider`, `userAgent`, and several private helper methods) had to be exposed as `@PublishedApi internal` just so inline call sites could reach them, permanently widening the library's binary surface.

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
val myBasicEndpoint = EndpointBasic<MyRequest, MyResponse>("api/my/endpoint/path", MyRequest::class.java, MyResponse::class.java, E2EEConfiguration.NOT_ENCRYPTED)
```

**`EndpointAuthenticated`**

Before (2.x):
```kotlin
val myAuthenticatedEndpoint = EndpointAuthenticated<MyRequest, MyResponse>("api/my/endpoint/path", "/endpoint/uriId", E2EEConfiguration.NOT_ENCRYPTED)
```

After (3.0.x):
```kotlin
val myAuthenticatedEndpoint = EndpointAuthenticated<MyRequest, MyResponse>("api/my/endpoint/path", "/endpoint/uriId", MyRequest::class.java, MyResponse::class.java, E2EEConfiguration.NOT_ENCRYPTED)
```

**`EndpointAuthenticatedWithToken`**

Before (2.x):
```kotlin
val myTokenEndpoint = EndpointAuthenticatedWithToken<MyRequest, MyResponse>("api/my/endpoint/path", "possession_universal", E2EEConfiguration.NOT_ENCRYPTED)
```

After (3.0.x):
```kotlin
val myTokenEndpoint = EndpointAuthenticatedWithToken<MyRequest, MyResponse>("api/my/endpoint/path", "possession_universal", MyRequest::class.java, MyResponse::class.java, E2EEConfiguration.NOT_ENCRYPTED)
```

<!-- begin box info -->
`Api.post(...)` call sites themselves are **unaffected** — only the `Endpoint` declaration changes.
<!-- end -->

---

### `tokenProvider` Deprecated

The `tokenProvider` constructor parameter and the `IPowerAuthTokenProvider`/`IPowerAuthTokenListener` interfaces are deprecated and no longer consulted. Stop passing `tokenProvider` to the `Api` constructor and remove any custom `IPowerAuthTokenProvider` implementation.

---

### Migration Checklist

- Add `Class<TRequestData>` and `Class<TResponseData>` arguments (e.g. `MyRequest::class.java`, `MyResponse::class.java`) to every `EndpointBasic`, `EndpointAuthenticated`, and `EndpointAuthenticatedWithToken` declaration in your project.
- No changes are needed at `Api.post(...)` call sites.
- Stop passing `tokenProvider` to the `Api` constructor and remove any custom `IPowerAuthTokenProvider` implementation.
