# Creating a Service API Class

Everything you need is packed inside the single `com.wultra.android.powerauth.networking.Api` abstract class that provides all the necessary APIs for your networking.

This class takes several parameters:

- `baseUrl` - Base URL for endpoints. For example `https://myservice.com/my-controller/`
- `okHttpClient` - okhttp3 client that will be used for the networking. You can leverage all utilities that are provided by this client like timeout configuration, listeners, etc...
- `powerAuthSDK` - `PowerAuthSDK` instance that will sign requests
- `gsonBuilder` - GsonBuilder for (de)serialization
- `appContext` - Application Context
- `userAgent` - Custom user-agent that will be added as an HTTP header to each request.

`Api` does not follow HTTP redirects by default. Configure `baseUrl` and the endpoint paths to point directly to the final service URL where possible.

To opt in, set `api.redirectPolicy` to a `RedirectPolicy` callback. It receives the source URL, resolved destination URL, and status code for a replayable 307 or 308. Return `true` to replay the POST once with its original body, or `false` to report the 3xx as an HTTP error. Only approve destinations where both sending the body and repeating the operation are safe.

The SDK does not replay HTTPS-to-HTTP redirects. PowerAuth-authenticated and encrypted requests, and requests carrying `X-PowerAuth-*`, `Authorization`, `Proxy-Authorization`, or explicit `Cookie` headers, can only be replayed within the same HTTPS origin. A cross-origin replay of a basic, unencrypted request does not copy the original request headers.

Redirects that are not followed reach `onFailure` as an `ApiError` wrapping an `ApiHttpException` with the HTTP status code. OkHttp's built-in redirect settings do not override this policy, and the supplied `OkHttpClient` is not modified.

<!-- begin box info -->
It is expected that you inherit this class and create your own APIs based on our needs.
<!-- end -->

Example MyServiceApi that will call 2 sample endpoints (one authenticated and one authenticated with a token):

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
