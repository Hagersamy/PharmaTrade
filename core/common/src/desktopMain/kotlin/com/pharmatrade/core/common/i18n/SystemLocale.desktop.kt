package com.pharmatrade.core.common.i18n

import java.util.Locale

actual fun systemLanguageCode(): String? = Locale.getDefault().language
