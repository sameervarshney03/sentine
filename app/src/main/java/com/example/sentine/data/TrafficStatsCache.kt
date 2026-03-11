package com.example.sentine.data

import android.content.Context
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// Stores the PREVIOUS TrafficStats reading 
// so we can calculate the DELTA each scan

object TrafficStatsCache {

    // In-memory cache: uid → (bytes, timestamp)
    // Persists across scans within same session
    private val previousReadings = 
        mutableMapOf<Int, Pair<Long, Long>>()

    data class NetworkDelta(
        val uploadKbPerMin: Float,
        val downloadKbPerMin: Float,
        val uploadVariance:   Float,
        val intervalSeconds: Long,
        val isFirstReading: Boolean
    )

    // Keep a rolling window of last 5 readings 
    // to calculate variance:
    private val uploadHistory = 
        mutableMapOf<Int, ArrayDeque<Float>>()

    fun getNetworkDelta(
        uid: Int,
        currentTxBytes: Long,
        currentRxBytes: Long
    ): NetworkDelta {
        val now = System.currentTimeMillis()
        val previous = previousReadings[uid]

        // Store current reading for next scan
        previousReadings[uid] = Pair(
            currentTxBytes, now
        )

        // First ever reading for this app —
        // cannot calculate delta yet
        if (previous == null) {
            return NetworkDelta(
                uploadKbPerMin   = 0f,
                downloadKbPerMin = 0f,
                uploadVariance   = 0f,
                intervalSeconds  = 0L,
                isFirstReading   = true
            )
        }

        val (prevBytes, prevTime) = previous
        val intervalMs = now - prevTime
        val intervalSeconds = intervalMs / 1000L

        // Avoid division by zero or tiny intervals
        if (intervalSeconds < 10L) {
            return NetworkDelta(
                uploadKbPerMin   = 0f,
                downloadKbPerMin = 0f,
                uploadVariance   = 0f,
                intervalSeconds  = intervalSeconds,
                isFirstReading   = false
            )
        }

        // Calculate bytes uploaded IN THIS INTERVAL ONLY
        val deltaTxBytes = (currentTxBytes - prevBytes)
            .coerceAtLeast(0L)

        // Convert to KB per minute
        val uploadKbPerMin = (deltaTxBytes / 1024f) / 
                             (intervalSeconds / 60f)

        // In getNetworkDelta(), after calculating 
        // uploadKbPerMin, add:
        val history = uploadHistory.getOrPut(uid) { 
            ArrayDeque(5) 
        }
        if (history.size >= 5) history.removeFirst()
        history.addLast(uploadKbPerMin)

        // Calculate variance from history
        val variance = if (history.size > 1) {
            val mean = history.average().toFloat()
            history.map { (it - mean) * (it - mean) }
                   .average().toFloat()
        } else 0f

        return NetworkDelta(
            uploadKbPerMin   = uploadKbPerMin,
            downloadKbPerMin = 0f,
            uploadVariance   = variance,
            intervalSeconds  = intervalSeconds,
            isFirstReading   = false
        )
    }

    // Call this when app first installs to 
    // capture baseline before any scoring
    fun captureBaseline(uid: Int) {
        val txBytes = android.net.TrafficStats
            .getUidTxBytes(uid)
        if (txBytes != android.net.TrafficStats
                .UNSUPPORTED.toLong()) {
            previousReadings[uid] = Pair(
                txBytes, 
                System.currentTimeMillis()
            )
        }
    }

    fun clearCache() {
        previousReadings.clear()
        uploadHistory.clear()
    }
}
