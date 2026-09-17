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
    SampleRequest(SampleResponseData("12345")),
    // specify endpoint
    myEndpoint,
    // Authenticated with
    auth,
    // custom HTTP headers
    hashMapOf(Pair("MyCustomHeader","Value"))
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
