package com.pharmatrade.core.common.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine

actual suspend fun currentDeviceToken(): String? = suspendCancellableCoroutine { continuation ->
    FirebaseMessaging.getInstance().token
        .addOnSuccessListener { token -> continuation.resume(token) {} }
        .addOnFailureListener { e ->
            Log.w("PharmaFCM", "currentDeviceToken: failed to fetch token for registration", e)
            continuation.resume(null) {}
        }
}
