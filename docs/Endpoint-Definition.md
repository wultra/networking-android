# Endpoint Definition

Each endpoint you will target with your project must be defined for the service as an `Endpoint` instance. There are several types of endpoints based on the PowerAuth authentication code that is required.

## End-To-End Encryption

If the endpoint is end-to-end encrypted, you need to configure it in the constructor. Default value is set to `E2EEConfiguration.NOT_ENCRYPTED`.

Possible values are:

```kotlin
/** End-to-end encryption configuration for an endpoint. */
enum class E2EEConfiguration {
    /** Endpoint is encrypted with the application scope. */
    APPLICATION_SCOPE,
    /** Endpoint is encrypted with the activation scope. */
    ACTIVATION_SCOPE,
    /** Endpoint is not encrypted. */
    NOT_ENCRYPTED
}
```

<!-- begin box info -->
Whether an endpoint is encrypted or not is based on its backend definition.
<!-- end -->

## Authenticated endpoint `EndpointAuthenticated`

For endpoints that are __authenticated__ by PowerAuth authentication code and can be end-to-end encrypted.

Example:

```kotlin
val myAuthenticatedEndpoint = EndpointAuthenticated<MyRequest, MyResponse>("api/my/endpoint/path", "/endpoint/uriId", MyRequest::class.java, MyResponse::class.java, E2EEConfiguration.NOT_ENCRYPTED)
// uriId is defined by the endpoint issuer - ask your server developer/provider
```

## Authenticated endpoint with Token `EndpointAuthenticatedWithToken`

For endpoints that are __authenticated by token__ by PowerAuth authentication code and can be end-to-end encrypted.

More info for token-based authentication [can be found here](https://github.com/wultra/powerauth-mobile-sdk/blob/develop/docs/PowerAuth-SDK-for-Android.md#token-based-authentication)

Example:

```kotlin
val myTokenEndpoint = EndpointAuthenticatedWithToken<MyRequest, MyResponse>("api/my/endpoint/path", "possession_universal", MyRequest::class.java, MyResponse::class.java, E2EEConfiguration.NOT_ENCRYPTED)

// token name (`possession_universal` in this case) is the name of the token as stored in the PowerAuthSDK
// more info can be found in the PowerAuthSDK documentation
// https://github.com/wultra/powerauth-mobile-sdk/blob/develop/docs/PowerAuth-SDK-for-Android.md#token-based-authentication

```

## Basic endpoint (not authenticated) `EndpointBasic`

For endpoints that are __not authenticated__ by PowerAuth authentication code but can be end-to-end encrypted.

Example:

```kotlin
val myBasicEndpoint = EndpointBasic<MyRequest, MyResponse>("api/my/endpoint/path", MyRequest::class.java, MyResponse::class.java, E2EEConfiguration.NOT_ENCRYPTED)
```
