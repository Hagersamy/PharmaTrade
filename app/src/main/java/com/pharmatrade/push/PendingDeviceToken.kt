package com.pharmatrade.push

// Stopgap holder for the FCM registration token until the backend's device-token registration
// endpoint contract is known (see project memory: project-pharmatrade-fcm-scope). Nothing reads
// this yet — once that endpoint exists, whoever wires it up should read `latest` here (and ideally
// observe it, e.g. turn this into a StateFlow) after login succeeds, POST it to the backend, and
// flush it whenever onNewToken fires again.
object PendingDeviceToken {
    var latest: String? = null
}
