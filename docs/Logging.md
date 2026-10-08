# Logging

The library is intensively logging into the console via `WPNLogger`.

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
| `DEBUG`               | All messages will be printed into the log.        |

## Log Listener

The `WPNLogger` class offers a static `logListener` property. If you provide a listener, all logs will also be passed to it (the library always logs into the Android default log).

<!-- begin box info -->
Log listener comes in handy when you want to log into a file or some online service.
<!-- end -->
