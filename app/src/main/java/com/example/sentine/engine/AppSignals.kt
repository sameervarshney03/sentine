package com.example.sentine.engine

data class AppSignals(
    val packageName: String,
    val uid: Int,

    // Signal 1 — Network
    val networkUploadKbPerMin: Float,    // KB uploaded per minute
    val sessionLengthSeconds: Float,     // how long network session lasted

    // Signal 2 — Screen off behavior
    val screenOffUploadKb: Float,        // KB uploaded while screen was off

    // Signal 3 — Background behavior
    val backgroundActivityScore: Float,  // 0.0 to 1.0, how active in background
    val backgroundWakeCount: Int,        // how many times app woke up itself

    // Signal 4 — Service
    val hasForegroundService: Boolean,
    val serviceRunningMinutes: Int,

    // Derived signals for ML
    val packetCount: Int,                // total outbound packets
    val uploadVariance: Float,           // how irregular the upload pattern is
    val isFirstReading: Boolean = false
)
