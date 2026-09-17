# Creating a Service API Class

Everything you need is packed inside the single `com.wultra.android.powerauth.networking.Api` abstract class that provides all the necessary APIs for your networking.

This class takes several parameters:

- `baseUrl` - Base URL for endpoints. For example `https://myservice.com/my-controller/`
- `okHttpClient` - okhttp3 client that will be used for the networking. You can leverage all utilities that are provided by this client like timeout configuration, listeners, etc...
- `powerAuthSDK` - `PowerAuthSDK` instance that will sign requests
- `gsonBuilder` - GsonBuilder for (de)serialization
- `appContext` - Application Context
- `tokenProvider` - Deprecated and ignored. Token-authenticated requests use the `PowerAuthSDK.tokenStore`, which is shared automatically.
- `userAgent` - Custom user-agent that will be added as an HTTP header to each request.

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
        private val sampleEndpoint1 = EndpointAuthenticated<SampleRequest, SampleResponse>("api/my/endpoint/user/sample", "/user/get", SampleResponse::class.java)
        // This endpoint points to https://my.serviceurl.com/api/auth/token/app/user/sample2
        private val sampleEndpoint2 = EndpointAuthenticatedWithToken<SampleRequest, SampleResponse>("api/my/endpoint/user/sample2", "possession_universal", SampleResponse::class.java)
    }
    
    /** Get the username with a token-authenticated request. */
    fun sample1(userID: String, listener: IApiCallResponseListener<SampleResponse>) {
        post(SampleRequest(SampleRequestData(userID)), sampleEndpoint1, null, null, null, listener)
    }
    
    /** Get the username with a user-authenticated request. */
    fun sample2(userID: String, authentication: PowerAuthAuthentication, listener: IApiCallResponseListener<SampleResponse>) {
        post(SampleRequest(SampleRequestData(userID)), sampleEndpoint2, authentication, null, null, null, listener)
    }
}
```
