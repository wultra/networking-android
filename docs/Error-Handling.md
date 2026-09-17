# Error Handling

Every error produced by this library is of a `ApiError` type. This error contains the following information:

- `error` - A specific reason, why the error happened. For more information see [ApiErrorCode chapter](#apierrorcode).
- `e` - Original exception/error that caused this error. In case of PowerAuth-related errors, it will be by the type of `ApiHttpException` or `ErrorResponseApiException`

## ApiErrorCode

Each `ApiError ` has an optional `error` property for why the error was created. Such reason can be useful when you're creating, for example, a general error handling or reporting, or when you're debugging the code.

### Known common API errors

| Option Name                  | Description                                                                               |
|------------------------------|-------------------------------------------------------------------------------------------|
| `ERROR_GENERIC`              | Network error that indicates a generic network issue (for example server internal error). |
| `POWERAUTH_AUTH_FAIL`        | General authentication failure (wrong password, wrong activation state, etc...)           |
| `INVALID_REQUEST`            | Invalid request sent - missing request object in the request                              |
| `INVALID_ACTIVATION`         | Activation is not valid (it is different from configured activation)                      |
| `INVALID_APPLICATION`        | Invalid application identifier is attempted for operation manipulation.                   |
| `INVALID_OPERATION`          | Invalid operation identifier is attempted for operation manipulation.                     |
| `ERR_ACTIVATION`             | Error during activation                                                                   |
| `ERR_AUTHENTICATION`         | Error in case that PowerAuth authentication fails                                         |
| `ERR_SECURE_VAULT`           | Error during secure vault unlocking                                                       |
| `ERR_ENCRYPTION`             | Returned in case encryption or decryption fails                                           |
| `TOO_MANY_REQUESTS`          | Too many same requests                                                                    |
| `REMOTE_COMMUNICATION_ERROR` | Communication with remote system failed                                                   |

### Known specific API errors

There are many Wultra-specific codes available, each starting with a service prefix:

- `OPERATION_` - like `OPERATION_EXPIRED`, when operation approval fails because it expired. 
- `PUSH_` - like `PUSH_REGISTRATION_FAILED` when push registering fails. 
- `ACTIVATION_CODE_` - like `ACTIVATION_CODE_FAILED` when failing to retrieve the activation code for the ActivationSpawn library. 
- `ONBOARDING_` - for onboarding-related errors. 
- `IDENTITY_` for identity-related errors. 
