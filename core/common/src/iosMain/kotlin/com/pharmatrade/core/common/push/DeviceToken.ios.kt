package com.pharmatrade.core.common.push

// Push (APNs via Firebase) is explicitly deferred on iOS for now — see project memory
// project-pharmatrade-fcm-scope. NOTE: this file has never been built (no Mac/Xcode here).
actual suspend fun currentDeviceToken(): String? = null
