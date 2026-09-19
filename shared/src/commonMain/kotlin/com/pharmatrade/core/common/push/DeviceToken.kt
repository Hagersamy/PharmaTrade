package com.pharmatrade.core.common.push

// The current push registration token (FCM on Android) for this device, sent as `device_token`
// on register/pharmacy and register/supplier so the backend can target this device with pushes.
// Returns null if the platform doesn't support push yet (iOS/desktop) or the token isn't
// available yet (e.g. no network on first launch) — registration proceeds without one either way.
expect suspend fun currentDeviceToken(): String?
