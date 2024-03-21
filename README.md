# PowerAuth Networking SDK for Android

<!-- begin remove -->
<p align="center"><img src="docs/intro.jpg" alt="Wultra Digital Onboarding for Android" width="100%" /></p>

[![build](https://github.com/wultra/networking-android/actions/workflows/build.yml/badge.svg)](https://github.com/wultra/networking-android/actions/workflows/build.yml)
[![maven](https://img.shields.io/maven-central/v/com.wultra.android.powerauth/powerauth-networking)](https://mvnrepository.com/artifact/com.wultra.android.powerauth/powerauth-networking)
![date](https://img.shields.io/github/release-date/wultra/networking-android)
[![license](https://img.shields.io/github/license/wultra/networking-android)](LICENSE)
<!-- end -->

__Wultra PowerAuth Networking__ (WPN) is a high-level SDK built on top of our [PowerAuth SDK](https://github.com/wultra/powerauth-mobile-sdk) that enables request signing and encryption.

<!-- begin box info -->
You can imagine the purpose of this SDK as an __HTTP layer (client) that enables request signing and encryption__ via PowerAuth SDK based on its recommended implementation.
<!-- end -->

We use this SDK in our other open-source projects that you can take inspiration for example in:  
- [Digital Onboarding SDK](https://github.com/wultra/digital-onboarding-android/blob/develop/library/src/main/java/com/wultra/android/digitalonboarding/networking/CustomerOnboardingApi.kt#L38)  
- [Mobile Token SDK](https://github.com/wultra/mtoken-sdk-android/blob/develop/library/src/main/java/com/wultra/android/mtokensdk/api/operation/OperationApi.kt)

<!-- begin remove -->
## Documentation Content
- [SDK Integration](#sdk-integration)
- [Open Source Code](#open-source-code)
- [Creating a Service API Class](#creating-a-service-api-class)
- [Endpoint Definition](#endpoint-definition)
- [Creating an HTTP request](#creating-an-http-request)
- [Error Handling](#error-handling)
- [Language Configuration](#language-configuration)
- [Logging](#logging)
<!-- end -->

## SDK Integration

### Requirements

- Android 5.0+ (API level 21+)
- [PowerAuth Mobile SDK](https://github.com/wultra/powerauth-mobile-sdk) needs to be implemented in your project

### Gradle

To use the SDK in your Android application include the following dependency to your gradle file.

```groovy
repositories {
    mavenCentral() // if not defined elsewhere...
}

implementation "com.wultra.android.powerauth:powerauth-networking:1.x.y"
```

## Open Source Code

The code of the library is open source and you can freely browse it in our GitHub at [https://github.com/wultra/networking-android](https://github.com/wultra/networking-android/#docucheck-keep-link)

## Creating a Service API Class

Everything you need is packed inside the single `com.wultra.android.powerauth.networking.Api` abstract class that provides all the necessary APIs for your networking.

This class takes several parameters:

- `baseUrl` - Base URL for endpoints. For example `https://myservice.com/my-controller/`
- `okHttpClient` - okhttp3 client that will be used for the networking. You can leverage all utilities that are provided by this client like timeout configuration, listeners, etc...
- `powerAuthSDK` - `PowerAuthSDK` instance that will sign requests
- `gsonBuilder` - GsonBuilder for (de)serialization
- `appContext` - Application Context
- `tokenProvider` - Optional Token Provider in case you have several services and want to share the token logic.
- `userAgent` - Custom user-agent that will be added as an HTTP header to each request.

<!-- begin box info -->
It is expected that you inherit this class and create your own APIs based on our needs.
<!-- end -->

Example MyServiceApi that will call 2 sample endpoints (one signed and one signed with token):

```kotlin
class MyServiceApi(
    okHttpClient: OkHttpClient,
    baseUrl: String,
    powerAuthSDK: PowerAuthSDK,
    appContext: Context
) : Api(baseUrl, okHttpClient, powerAuthSDK, GsonBuilder(), appContext) {

    class SampleRequestData(@SerializedName("uid") val userID: String)
    class SampleResponseData(@SerializedName("name") val username: String)

    class SampleRequest(requestObject: SampleRequestData): ObjectRequest<SampleRequestData>(requestObject)
    class SampleResponse(responseObject: SampleResponseData, status: Status): ObjectResponse<SampleResponseData>(responseObject, status)

    companion object {
        // This endpoint points to https://my.serviceurl.com/api/auth/token/app/user/sample
        private val sampleEndpoint1 = EndpointSigned<SampleRequest, SampleResponse>("api/my/endpoint/user/sample", "/user/get")
        // This endpoint points to https://my.serviceurl.com/api/auth/token/app/user/sample2
        private val sampleEndpoint2 = EndpointSignedWithToken<SampleRequest, SampleResponse>("api/my/endpoint/user/sample2", "possession_universal")
    }
    
    /** Get the username with a token-signed request. */
    fun sample1(userID: String, listener: IApiCallResponseListener<SampleResponse>) {
        post(SampleRequest(SampleRequestData(userID)), sampleEndpoint1, null, null, null, listener)
    }
    
    /** Get the username with a user-signed request. */
    fun sample2(userID: String, authentication: PowerAuthAuthentication, listener: IApiCallResponseListener<SampleResponse>) {
        post(SampleRequest(SampleRequestData(userID)), sampleEndpoint2, authentication, null, null, null, listener)
    }
}
```

## Endpoint Definition

Each endpoint you will target with your project must be defined for the service as an `Endpoint` instance. There are several types of endpoints based on the PowerAuth signature that is required.

### Signed endpoint `EndpointSigned`

For endpoints that are __signed__ by PowerAuth signature and can be end-to-end encrypted.

Example:

```kotlin
val mySignedEndpoint = EndpointSigned<MyRequest, MyResponse>("api/my/endpoint/path", "/endpoint/uriId")
// uriId is defined by the endpoint issuer - ask your server developer/provider
```

### Signed endpoint with Token `EndpointSignedWithToken`

For endpoints that are __signed by token__ by PowerAuth signature and can be end-to-end encrypted.

More info for token-based authentication [can be found here](https://github.com/wultra/powerauth-mobile-sdk/blob/develop/docs/PowerAuth-SDK-for-Android.md#token-based-authentication)

Example:

```kotlin
val myTokenEndpoint = EndpointSignedWithToken<MyRequest, MyResponse>("api/my/endpoint/path", "possession_universal")

// token name (`possession_universal` in this case) is the name of the token as stored in the PowerAuthSDK
// more info can be found in the PowerAuthSDK documentation
// https://github.com/wultra/powerauth-mobile-sdk/blob/develop/docs/PowerAuth-SDK-for-Android.md#token-based-authentication

```

### Basic endpoint (not signed) `EndpointBasic`

For endpoints that are __not signed__ by PowerAuth signature but can be end-to-end encrypted.

Example:

```kotlin
val myBasicEndpoint = EndpointBasic<MyRequest, MyResponse>("api/my/endpoint/path")
```

## Creating an HTTP request

To create an HTTP request to your endpoint, you need to call the `Api.post` method with the following parameters:

- `data` - with the payload of your request
- `endpoint` - an endpoint that will be called
- `auth` - `PowerAuthAuthentication` instance that will sign the request  
  - this parameter is missing for the basic and token endpoints 
- `headers` - custom HTTP headers, `null` by default
- `encryptor` - End to End encryptor in case that the encryption is required, `null` by default
- `okHttpInterceptor` - OkHttp interceptor to intercept requests eg. for logging purposes, `null` by default
- `listener` - result listener


Example:

```kotlin
// Sample Data that will be sent and received from the server
class SampleRequestData(@SerializedName("uid") val userID: String)
class SampleResponseData(@SerializedName("name") val username: String)

// Request objects
class SampleRequest(requestObject: SampleRequestData): ObjectRequest<SampleRequestData>(requestObject)
class SampleResponse(responseObject: SampleResponseData, status: Status): ObjectResponse<SampleResponseData>(responseObject, status)

// endpoint configuration
val myEndpoint = EndpointSigned<SampleRequest, SampleResponse>("api/my/endpoint/path", "/my/endoint/uriId")

// Authentication, for example purposes, expect user PIN 1111
val auth = PowerAuthAuthentication.possessionWithPassword("1111")
            
// Api.post call
post(
    // create request data
    SampleRequest(SampleResponseData("12345")),
    // specify endpoint
    myEndpoint,
    // Authenticated with
    auth,
    // custom HTTP headers
    hashMapOf(Pair("MyCustomHeader","Value"))
    // encrypt with the application scope. null if not encrypted (usual case)
    powerAuthSDK.eciesEncryptorForApplicationScope,
    // no HTTP interceptor
    null,
    // handle response or error
    object : IApiCallResponseListener<SampleResponse> {
        override fun onFailure(error: ApiError) {
            // handle error
        }

        override fun onSuccess(result: SampleResponse) {
            // handle success
        }
    }
)

```

## Error Handling

Every error produced by this library is of a `ApiError` type. This error contains the following information:

- `error` - A specific reason, why the error happened. For more information see [ApiErrorCode chapter](#apierrorcode).
- `e` - Original exception/error that caused this error. In case of PowerAuth-related errors, it will be by the type of `ApiHttpException` or `ErrorResponseApiException`

### ApiErrorCode

Each `ApiError ` has an optional `error` property for why the error was created. Such reason can be useful when you're creating for example a general error handling or reporting, or when you're debugging the code.

#### Known common API errors

| Option Name | Description |
|---|---|
| `ERROR_GENERIC` | When unexpected error happened |
| `POWERAUTH_AUTH_FAIL` | General authentication failure (wrong password, wrong activation state, etc...) |
| `INVALID_REQUEST` | Invalid request sent - missing request object in the request |
| `INVALID_ACTIVATION` | Activation is not valid (it is different from configured activation) |
| `INVALID_APPLICATION` | Invalid application identifier is attempted for operation manipulation. |
| `INVALID_OPERATION` | Invalid operation identifier is attempted for operation manipulation. |
| `ERR_ACTIVATION` | Error during activation |
| `ERR_AUTHENTICATION` | Error in case that PowerAuth authentication fails |
| `ERR_SECURE_VAULT` | Error during secure vault unlocking |
| `ERR_ENCRYPTION` | Returned in case encryption or decryption fails |
| `TOO_MANY_REQUESTS` | Too many same requests |
| `REMOTE_COMMUNICATION_ERROR` | Communication with remote system failed |

#### Known specific API errors

There are many Wultra-specific codes available, each starting with a service prefix:

- `OPERATION_` - like `OPERATION_EXPIRED`, when operation approval fails because it expired. 
- `PUSH_` - like `PUSH_REGISTRATION_FAILED` when push registering fails. 
- `ACTIVATION_CODE_` - like `ACTIVATION_CODE_FAILED` when failing to retrieve the activation code for the ActivationSpawn library. 
- `ONBOARDING_` - for onboarding-related errors. 
- `IDENTITY_` for identity-related errors. 

## Language Configuration

Before using any methods from this SDK that call the backend, a proper language should be set. A properly translated content is served based on this configuration. The property that stores language settings __does not persist__. You need to set `acceptLanguage` every time that the application boots.

<!-- begin box warning -->
Note: Content language capabilities are limited by the implementation of the server - it must support the provided language.
<!-- end -->

### Format

The default value is always `en`. With other languages, we use values compliant with standard RFC [Accept-Language](https://tools.ietf.org/html/rfc7231#section-5.3.5).

## Logging

For logging purposes `com.wultra.android.powerauth.networking.Logger` that prints to the console is used.

### Verbosity Level

You can limit the amount of logged information via `verboseLevel` property.

| Level | Description |
| --- | --- |
| `OFF` | Silences all messages. |
| `ERROR` | Only errors will be printed into the log. |
| `WARNING` _(default)_ | Errors and warnings will be printed into the log. |
| `DEBUG` | All messages will be printed into the log. |

<!-- begin remove -->
## Web Documentation

This documentation is also available at the [Wultra Developer Portal](https://developers.wultra.com/).

## License

All sources are licensed using the Apache 2.0 license. You can use them with no restrictions. If you are using this library, please let us know. We will be happy to share and promote your project.

## Contact

If you need any assistance, do not hesitate to drop us a line at [hello@wultra.com](mailto:hello@wultra.com) or our official [wultra.com/discord](https://wultra.com/discord) channel.

### Security Disclosure

If you believe you have identified a security vulnerability with this SDK, you should report it as soon as possible via email to [support@wultra.com](mailto:support@wultra.com). Please do not post it to a public issue tracker.
<!-- end -->
