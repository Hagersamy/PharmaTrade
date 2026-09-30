package com.pharmatrade.core.common.log

// Lets shared (commonMain) code write to the crash reporter without depending on Firebase.
// :app installs the Crashlytics-backed sink at startup (PharmaTradeApp); until then — and on
// platforms that never install one — every call is a no-op.
interface CrashReportSink {
    // Breadcrumb: kept in a rolling buffer and attached to the next crash / non-fatal report.
    fun log(message: String)
    fun setKey(key: String, value: String)
    // Reported to the console as a non-fatal issue, grouped by [error]'s type + message.
    fun recordNonFatal(error: Throwable, keys: Map<String, String>)
}

object CrashReporter {
    var sink: CrashReportSink? = null

    fun log(message: String) {
        sink?.log(message)
    }

    fun setKey(key: String, value: String) {
        sink?.setKey(key, value)
    }

    fun recordNonFatal(error: Throwable, keys: Map<String, String> = emptyMap()) {
        sink?.recordNonFatal(error, keys)
    }
}
