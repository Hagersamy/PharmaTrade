package com.pharmatrade.core.common.i18n

// The device's current system language code (e.g. "ar", "en"). Consulted only as the default
// on first launch, before the user has ever picked a language on the Profile screen — once
// they do, LanguageManager persists that explicit choice and this is never read again.
expect fun systemLanguageCode(): String?
