# Logging

The library writes diagnostic messages to Android Logcat via `WPNLogger`.

<!-- begin box info -->
`WPNLogger` calls internally the `android.util.Log` class.
<!-- end -->

## Verbosity Level

You can limit the amount of logged information via `verboseLevel` property.

| Level                 | Description                                       |
|-----------------------|---------------------------------------------------|
| `OFF`                 | Silences all messages.                            |
| `ERROR`               | Only errors will be printed into the log.         |
| `WARNING` _(default)_ | Errors and warnings will be printed into the log. |
| `INFO`                | Errors, warnings, and informational messages will be printed into the log. |
| `DEBUG`               | All messages will be printed into the log.        |

## HTTP Traffic

The built-in HTTP logger is disabled by default. To enable it when troubleshooting, set:

```kotlin
WPNLogger.logHttpTraffic = true
WPNLogger.verboseLevel = WPNLogger.VerboseLevel.INFO
```

The logger records the method, origin (scheme, host, and port), response status, and filtered headers at `INFO`. Network I/O failures are logged at `ERROR` with the exception class, not its message. It does not log full URLs or request and response bodies, including decrypted E2EE responses, even at `DEBUG`.

The values of `Authorization`, `Proxy-Authorization`, `Cookie`, `Set-Cookie`, and `X-PowerAuth-*` headers are always redacted, regardless of capitalization. Use `WPNLogger.httpHeadersToSkip` to omit additional headers, including any application-specific headers that contain sensitive values. Adding a redacted header to the skip list hides it entirely; removing it from the list does not expose its value.

## Log Listener

Set `WPNLogger.logListener` to route messages to your own logging system. A listener with `followVerboseLevel = true` receives only messages allowed by `WPNLogger.verboseLevel`. To disable Logcat while keeping your own logging active, set `WPNLogger.verboseLevel` to `OFF` and implement the listener with `followVerboseLevel = false`. The listener can then apply its own filtering.

HTTP diagnostics still require `WPNLogger.logHttpTraffic = true` and use the same redaction rules for both destinations.

<!-- begin box info -->
Log listener comes in handy when you want to log into a file or some online service.
<!-- end -->
