# Creating an HTTP request

To create an HTTP request to your endpoint, you need to call the `Api.post` method with the following parameters:

- `data` - with the payload of your request
- `endpoint` - an endpoint that will be called
- `auth` - `PowerAuthAuthentication` instance that will authenticate the request  
  - this parameter is missing for the basic and token endpoints 
- `headers` - custom HTTP headers, `null` by default
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
val myEndpoint = EndpointAuthenticated<SampleRequest, SampleResponse>("api/my/endpoint/path", "/my/endoint/uriId", SampleResponse::class.java, E2EEConfiguration.NOT_ENCRYPTED)

// Authentication, for example purposes, expect user PIN 1111
val auth = PowerAuthAuthentication.possessionWithPassword("1111")
            
// Api.post call
post(
    // create request data
    SampleRequest(SampleRequestData("12345")),
    // specify endpoint
    myEndpoint,
    // Authenticated with
    auth,
    // custom HTTP headers
    hashMapOf(Pair("MyCustomHeader","Value")),
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

## Request concurrency strategy

Requests to `EndpointAuthenticated` are signed with a PowerAuth authentication code, which uses a
counter as a representation of logical time. The order in which signed requests are validated on
the server matters: if more than one is issued at the same time, that order is not guaranteed,
and one of the requests may fail.

To prevent this, `Api.concurrencyStrategy` (of type `RequestConcurrencyStrategy`) controls how
`EndpointAuthenticated` requests are dispatched:

- `SERIAL_AUTHENTICATED` (default) - these requests are serialized via `PowerAuthSDK.getSerialExecutor()`, so only one is signed and in flight at a time, preserving counter order.
- `CONCURRENT_ALL` - all requests, including `EndpointAuthenticated`, are dispatched concurrently.

`EndpointBasic` and `EndpointAuthenticatedWithToken` requests are always dispatched concurrently
and are not affected by this setting, since they either aren't signed at all or use a
counter-independent token header.

If your app relies on firing multiple `EndpointAuthenticated` requests at once and handles
ordering itself, opt back into the previous behavior:

```kotlin
api.concurrencyStrategy = RequestConcurrencyStrategy.CONCURRENT_ALL
```
