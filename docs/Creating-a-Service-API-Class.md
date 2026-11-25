# Creating a Service API Class

Everything you need is packed inside the single `com.wultra.android.powerauth.networking.Api` abstract class that provides all the necessary APIs for your networking.

This class takes several parameters:

- `baseUrl` - Base URL for endpoints. For example `https://myservice.com/my-controller/`
- `okHttpClient` - okhttp3 client that will be used for the networking. You can leverage all utilities that are provided by this client like timeout configuration, listeners, etc...
- `powerAuthSDK` - `PowerAuthSDK` instance that will sign requests
- `gsonBuilder` - GsonBuilder for (de)serialization
- `appContext` - Application Context
- `userAgent` - Custom user-agent that will be added as an HTTP header to each request.
- `followRedirects` - Whether to follow HTTP redirects. Defaults to `false` and cannot be changed after construction.

`Api.followRedirects` defaults to `false`. Redirects that are not followed reach `onFailure` as an `ApiError` wrapping an `ApiHttpException` with the HTTP status code. Configure `baseUrl` and endpoint paths to point directly to the final service URL where possible.

Pass `followRedirects = true` to the `Api` constructor to opt in to standard OkHttp redirect handling, including redirects to another host and between HTTP and HTTPS. OkHttp determines whether a redirect changes a POST to a GET. Custom headers, including `X-PowerAuth-*`, may be sent to a different host or over plain HTTP, so only enable redirects when you trust the destinations.

When `followRedirects` is `true`, use the `okHttpInterceptor` parameter of `Api.post` to fine-tune redirects per request. For example, call `builder.followSslRedirects(false)` to disallow redirects between HTTP and HTTPS. When `followRedirects` is `false`, the hook cannot re-enable OkHttp's automatic redirects. The supplied `OkHttpClient` is not modified.

<!-- begin box info -->
It is expected that you inherit this class and create your own APIs based on our needs.
<!-- end -->

Example MyServiceApi that will call 2 sample endpoints (one authenticated and one authenticated with a token):

```kotlin
class MyServiceApi(
    okHttpClient: OkHttpClient,
    baseUrl: String,
    powerAuthSDK: PowerAuthSDK,
    appContext: Context,
    followRedirects: Boolean = false
) : Api(baseUrl, okHttpClient, powerAuthSDK, GsonBuilder(), appContext, followRedirects = followRedirects) {

    class SampleRequestData(@SerializedName("uid") val userID: String)
    class SampleResponseData(@SerializedName("name") val username: String)

    class SampleRequest(requestObject: SampleRequestData): ObjectRequest<SampleRequestData>(requestObject)
    class SampleResponse(responseObject: SampleResponseData, status: Status): ObjectResponse<SampleResponseData>(responseObject, status)

    companion object {
        // This endpoint points to https://my.serviceurl.com/api/auth/token/app/user/sample
        private val sampleEndpoint1 = EndpointAuthenticated("api/my/endpoint/user/sample", "/user/get", SampleRequest::class.java, SampleResponse::class.java)
        // This endpoint points to https://my.serviceurl.com/api/auth/token/app/user/sample2
        private val sampleEndpoint2 = EndpointAuthenticatedWithToken("api/my/endpoint/user/sample2", "possession_universal", SampleRequest::class.java, SampleResponse::class.java)
    }
    
    /** Get the username with a token-authenticated request. */
    fun sample1(userID: String, listener: IApiCallResponseListener<SampleResponse>) {
        post(SampleRequest(SampleRequestData(userID)), sampleEndpoint2, listener = listener)
    }
    
    /** Get the username with a user-authenticated request. */
    fun sample2(userID: String, authentication: PowerAuthAuthentication, listener: IApiCallResponseListener<SampleResponse>) {
        post(SampleRequest(SampleRequestData(userID)), sampleEndpoint1, authentication, listener = listener)
    }
}
```
