package com.example.sentine.engine

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import android.os.PowerManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.util.Log
import com.example.sentine.data.TrafficStatsCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RiskScoringEngine(private val context: Context) {

    private val mlEngine      = MLRiskEngine(context)
    private val anomalyEngine = AnomalyRiskEngine(context)
    var isMLReady: Boolean = false
        private set

    data class RiskResult(
        val packageName:   String,
        val score:         Int,
        val level:         String,
        val mlScore:       Int,
        val ruleScore:     Int,
        val anomalyScore:  Int,
        val networkScore:  Int,
        val backgroundScore: Int,
        val serviceScore:  Int,
        val screenOffScore: Int,
        val reasons:        List<String>,
        val isRecentlyUsed: Boolean = false,
        val isFirstReading: Boolean = false,
        val skipReason:    String? = null
    )

    fun initialize() {
        isMLReady = mlEngine.initialize()
        anomalyEngine.initialize()
    }

    suspend fun calculateRisk(
        packageName: String, 
        gracePeriodMinutes: Int,
        isTrusted: Boolean,
        trustedUntil: Long = 0L
    ): RiskResult = withContext(Dispatchers.IO) {

        val uid = try {
            context.packageManager.getApplicationInfo(packageName, 0).uid
        } catch (e: Exception) { -1 }

        if (uid == -1) {
            return@withContext RiskResult(
                packageName = packageName,
                score = 0,
                level = "SAFE",
                mlScore = 0,
                ruleScore = 0,
                anomalyScore = 0,
                networkScore = 0,
                backgroundScore = 0,
                serviceScore = 0,
                screenOffScore = 0,
                reasons = emptyList(),
                skipReason = "App not found"
            )
        }

        // ── Context check: user recently used app ──
        if (userRecentlyOpenedApp(packageName, gracePeriodMinutes)) {
            return@withContext RiskResult(
                packageName  = packageName,
                score        = 0,
                level        = "SAFE",
                mlScore      = 0,
                ruleScore    = 0,
                anomalyScore = 0,
                networkScore = 0,
                backgroundScore = 0,
                serviceScore = 0,
                screenOffScore = 0,
                reasons      = listOf(
                    "App recently used by you — monitoring paused"
                ),
                isRecentlyUsed = true,
                skipReason = "Recently used"
            )
        }

        val sharedPref = context.getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE)

        // ── Collect REAL signals ──
        val signals = collectSignals(packageName, uid)

        // ── Layer 1: Rule-based score (break down for UI) ──
        val networkScore = calculateNetworkScore(signals)
        val backgroundScore = calculateBackgroundScore(signals)
        val serviceScore = calculateServiceScore(signals)
        val screenOffScore = calculateScreenOffScore(signals)
        val ruleScore = (networkScore + backgroundScore + serviceScore + screenOffScore).coerceIn(0, 100)

        // ── Layer 2: Anomaly score (real deviation) ──
        val anomalyScore = anomalyEngine.score(signals)

        // ── Layer 3: ML model score (real inference) ──
        val mlResult = if (isMLReady) {
            mlEngine.analyze(signals)
        } else null

        val mlScore = mlResult?.combinedScore
            ?.takeIf { it >= 0 } ?: 0

        // ── Weighted combination ──
        // Only use ML weight if model is loaded
        val finalScore = if (isMLReady && mlScore >= 0) {
            // Full 3-layer score
            (ruleScore    * 0.20f +
             anomalyScore * 0.35f +
             mlScore      * 0.45f).toInt()
        } else {
            // Fallback: rule + anomaly only
            (ruleScore    * 0.50f +
             anomalyScore * 0.50f).toInt()
        }.coerceIn(0, 100)

        val clampedScore = finalScore.coerceIn(0, 100)

        val level = when {
            clampedScore >= 76 -> "HIGH"
            clampedScore >= 51 -> "MEDIUM"
            clampedScore >= 26 -> "LOW"
            else               -> "SAFE"
        }

        Log.d("RiskEngine", """
            ═══ FINAL SCORE: $packageName ═══
            ruleScore    = $ruleScore
            anomalyScore = $anomalyScore
            mlScore      = $mlScore
            finalScore   = $clampedScore
            level        = $level
            mlReady      = $isMLReady
        """.trimIndent())

        var isTrustActive = isTrusted && System.currentTimeMillis() < trustedUntil
        var finalClampedScore = clampedScore
        var finalLevel = level
        var skipReasonVal: String? = null
        val finalReasons = mutableListOf<String>().apply { addAll(mlResult?.topReasons ?: buildRuleReasons(signals)) }

        if (isTrustActive) {
            val trustBaseline = sharedPref.getFloat("trust_baseline_$packageName", -1f)
            val isSpike = trustBaseline > 0f && signals.networkUploadKbPerMin > (3 * trustBaseline)

            if (isSpike) {
                isTrustActive = false
                skipReasonVal = "Trust overridden: 3x network spike detected"
                finalReasons.add("TRUST OVERRIDE: Network usage (${signals.networkUploadKbPerMin.toInt()} KB/min) is >3x historical average.")
            } else {
                finalClampedScore = (finalClampedScore * 0.3f).toInt()
                finalLevel = when {
                    finalClampedScore >= 76 -> "HIGH"
                    finalClampedScore >= 51 -> "MEDIUM"
                    finalClampedScore >= 26 -> "LOW"
                    else -> "SAFE"
                }
                skipReasonVal = "Score reduced by 70% (Trusted app)"
            }
        }

        RiskResult(
            packageName  = packageName,
            score        = finalClampedScore,
            level        = finalLevel,
            mlScore      = mlScore,
            ruleScore    = ruleScore,
            anomalyScore = anomalyScore,
            networkScore = networkScore,
            backgroundScore = backgroundScore,
            serviceScore = serviceScore,
            screenOffScore = screenOffScore,
            reasons      = finalReasons,
            isFirstReading = signals.isFirstReading,
            skipReason   = skipReasonVal
        )
    }

    // Rule-based reasons when ML is unavailable
    private fun buildRuleReasons(
        signals: AppSignals
    ): List<String> {
        val reasons = mutableListOf<String>()
        if (signals.networkUploadKbPerMin > 200)
            reasons.add(
                "Uploading ${signals.networkUploadKbPerMin
                    .toInt()} KB/min in background"
            )
        if (signals.backgroundWakeCount > 5)
            reasons.add(
                "Woke device ${signals.backgroundWakeCount} times without user interaction"
            )
        if (signals.hasForegroundService && 
            signals.serviceRunningMinutes > 30)
            reasons.add(
                "Background service running for ${signals.serviceRunningMinutes} minutes"
            )
        if (signals.screenOffUploadKb > 100)
            reasons.add(
                "Sent data while screen was off"
            )
        return reasons.ifEmpty {
            listOf("Behavior within normal parameters")
        }
    }

    // ── Collect all signals for an app ──
    private fun collectSignals(
        packageName: String, 
        uid: Int
    ): AppSignals {
    
        val activityManager = context.getSystemService(
            Context.ACTIVITY_SERVICE) as ActivityManager
        val powerManager = context.getSystemService(
            Context.POWER_SERVICE) as PowerManager
        val usageStatsManager = context.getSystemService(
            Context.USAGE_STATS_SERVICE) as UsageStatsManager
    
        val now          = System.currentTimeMillis()
        val thirtyMinAgo = now - (30 * 60 * 1000L)
        val oneHourAgo   = now - (60 * 60 * 1000L)
    
        // ── SIGNAL 1: REAL network upload delta ──
        val currentTxBytes = TrafficStats.getUidTxBytes(uid)
            .takeIf { it >= 0L } ?: 0L
        val currentRxBytes = TrafficStats.getUidRxBytes(uid)
            .takeIf { it >= 0L } ?: 0L
    
        val networkDelta = TrafficStatsCache.getNetworkDelta(
            uid            = uid,
            currentTxBytes = currentTxBytes,
            currentRxBytes = currentRxBytes
        )
    
        val uploadKbPerMin    = networkDelta.uploadKbPerMin
        val uploadVariance    = networkDelta.uploadVariance
        val packetCount       = (currentTxBytes / 1500L).toInt()
    
        Log.d("RiskEngine", 
            "$packageName → upload: ${uploadKbPerMin}KB/min")
    
        // ── SIGNAL 2: REAL background activity from UsageStats ──
        val usageEvents = usageStatsManager.queryEvents(
            thirtyMinAgo, now
        )
    
        var backgroundWakeCount    = 0
        var foregroundTimeMs       = 0L
        var lastEventTime          = 0L
        var hasRecentForeground    = false
        val event = UsageEvents.Event()
    
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.packageName != packageName) continue
    
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    hasRecentForeground = true
                    lastEventTime = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (lastEventTime > 0) {
                        foregroundTimeMs += event.timeStamp - lastEventTime
                    }
                    lastEventTime = 0L
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    backgroundWakeCount++
                }
            }
        }
    
        // Background activity score 0.0-1.0
        // Based on how much time in bg vs fg in last 30 min
        val backgroundActivityScore = when {
            foregroundTimeMs <= 0 && backgroundWakeCount > 3 -> 0.9f
            foregroundTimeMs <= 0 && backgroundWakeCount > 0 -> 0.6f
            backgroundWakeCount > 5 -> 0.8f
            backgroundWakeCount > 2 -> 0.5f
            else -> (backgroundWakeCount / 10f).coerceIn(0f, 1f)
        }
    
        Log.d("RiskEngine",
            "$packageName → bgWakes: $backgroundWakeCount " +
            "fgTime: ${foregroundTimeMs}ms " + 
            "bgScore: $backgroundActivityScore")
    
        // ── SIGNAL 3: REAL foreground service check ──
        val runningServices = try {
            @Suppress("DEPRECATION")
            activityManager.getRunningServices(300)
        } catch (e: Exception) { emptyList() }
    
        val appService = runningServices.firstOrNull { svc ->
            svc.service.packageName == packageName
        }
    
        val hasForegroundService  = appService != null
        val serviceRunningMinutes = appService?.let { svc ->
            ((now - svc.activeSince) / 60_000L).toInt()
                .coerceAtLeast(0)
        } ?: 0
    
        Log.d("RiskEngine",
            "$packageName → fgService: $hasForegroundService " +
            "runningMins: $serviceRunningMinutes")
    
        // ── SIGNAL 4: REAL screen off upload check ──
        val isScreenOff      = !powerManager.isInteractive
        val screenOffUploadKb = if (isScreenOff) uploadKbPerMin else 0f
    
        // ── SIGNAL 5: REAL session length from UsageStats ──
        val usageStats = usageStatsManager.queryAndAggregateUsageStats(
            oneHourAgo, now
        )
        val stats = usageStats[packageName]
        val totalSessionMs = stats?.totalTimeInForeground ?: 0L
    
        return AppSignals(
            packageName           = packageName,
            uid                   = uid,
            networkUploadKbPerMin = uploadKbPerMin,
            uploadVariance        = uploadVariance,
            packetCount           = packetCount,
            backgroundWakeCount   = backgroundWakeCount,
            backgroundActivityScore = backgroundActivityScore,
            hasForegroundService  = hasForegroundService,
            serviceRunningMinutes = serviceRunningMinutes,
            screenOffUploadKb     = screenOffUploadKb,
            sessionLengthSeconds  = (totalSessionMs / 1000f),
            isFirstReading        = networkDelta.isFirstReading
        )
    }

    private fun calculateNetworkScore(signals: AppSignals): Int {
        return when {
            signals.networkUploadKbPerMin > 1000 -> 100
            signals.networkUploadKbPerMin > 500  -> 70
            signals.networkUploadKbPerMin > 100  -> 40
            signals.networkUploadKbPerMin > 10   -> 10
            else -> 0
        }
    }

    private fun calculateBackgroundScore(signals: AppSignals): Int {
        return (signals.backgroundActivityScore * 100).toInt()
    }

    private fun calculateServiceScore(signals: AppSignals): Int {
        if (!signals.hasForegroundService) return 0
        return when {
            signals.serviceRunningMinutes > 120 -> 80
            signals.serviceRunningMinutes > 60  -> 50
            signals.serviceRunningMinutes > 30  -> 30
            else -> 10
        }
    }

    private fun calculateScreenOffScore(signals: AppSignals): Int {
        return when {
            signals.screenOffUploadKb > 500 -> 100
            signals.screenOffUploadKb > 100 -> 70
            signals.screenOffUploadKb > 10  -> 40
            else -> 0
        }
    }

    private fun userRecentlyOpenedApp(packageName: String, gracePeriodMin: Int): Boolean {
        if (gracePeriodMin <= 0) return false
        
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val startTime = now - (gracePeriodMin * 60 * 1000L)
        
        val events = usageStatsManager.queryEvents(startTime, now)
        val event = UsageEvents.Event()
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName == packageName && 
                (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || 
                 event.eventType == UsageEvents.Event.USER_INTERACTION)) {
                return true
            }
        }
        return false
    }
}
