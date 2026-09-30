package com.pharmatrade.core.common.log

// Plain debug logging usable from shared (commonMain) code — Android routes it to logcat
// (filter with `adb logcat -s <tag>`), other platforms print to stdout.
expect fun debugLog(tag: String, message: String)
