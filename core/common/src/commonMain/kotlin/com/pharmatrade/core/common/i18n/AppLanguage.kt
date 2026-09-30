package com.pharmatrade.core.common.i18n

enum class AppLanguage(val code: String, val isRtl: Boolean) {
    ENGLISH("en", false),
    ARABIC("ar", true);

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}
