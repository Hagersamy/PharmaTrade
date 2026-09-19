package com.pharmatrade.core.common.i18n

import platform.Foundation.NSLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages

// NOTE: Kotlin/Native's iOS targets can only be compiled on macOS with Xcode installed, which
// this codebase can't do from Windows — this file has never been built. Double-check the
// NSLocale interop shape below once you're building on your Mac.
actual fun systemLanguageCode(): String? {
    val preferred = NSLocale.preferredLanguages.firstOrNull() as? String
    return preferred?.substringBefore("-") ?: NSLocale.currentLocale.languageCode
}
