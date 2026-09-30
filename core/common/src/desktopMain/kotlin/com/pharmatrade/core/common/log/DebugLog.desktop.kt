package com.pharmatrade.core.common.log

actual fun debugLog(tag: String, message: String) {
    println("D/$tag: $message")
}
