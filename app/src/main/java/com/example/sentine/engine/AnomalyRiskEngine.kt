package com.example.sentine.engine

import android.content.Context
import org.json.JSONObject

class AnomalyRiskEngine(private val context: Context) {

    private lateinit var mean:  FloatArray
    private lateinit var scale: FloatArray
    private var isReady = false

    // Normal behavior thresholds learned during training
    // These represent the 90th percentile of normal app behavior
    private val NORMAL_THRESHOLDS = mapOf(
        "network_upload_rate"  to 600f,    // KB/min — above this is suspicious
        "session_length"       to 15000f,  // seconds
        "screen_off_activity"  to 300f,    // KB while screen off
        "background_activity"  to 500f,    // activity units
        "packet_count"         to 500f,    // packets
        "upload_variance"      to 400f     // variance units
    )

    // How much each feature contributes to anomaly detection
    // Matches feature_importances from training
    private val FEATURE_WEIGHTS = mapOf(
        "network_upload_rate"  to 0.28f,
        "screen_off_activity"  to 0.24f,
        "background_activity"  to 0.20f,
        "upload_variance"      to 0.14f,
        "packet_count"         to 0.08f,
        "session_length"       to 0.06f
    )

    fun initialize() {
        try {
            val json = context.assets.open("anomaly_scaler.json")
                .bufferedReader().readText()
            val obj = JSONObject(json)
            val meanArr  = obj.getJSONArray("mean")
            val scaleArr = obj.getJSONArray("scale")
            mean  = FloatArray(meanArr.length())  { meanArr.getDouble(it).toFloat() }
            scale = FloatArray(scaleArr.length()) { scaleArr.getDouble(it).toFloat() }
            isReady = true
        } catch (e: Exception) {
            isReady = false
        }
    }

    fun score(signals: AppSignals): Int {
        val rawFeatures = floatArrayOf(
            signals.networkUploadKbPerMin,
            signals.sessionLengthSeconds,
            signals.screenOffUploadKb,
            signals.backgroundActivityScore * 100f,
            signals.packetCount.toFloat(),
            signals.uploadVariance
        )

        val featureNames = listOf(
            "network_upload_rate", "session_length", "screen_off_activity",
            "background_activity", "packet_count", "upload_variance"
        )

        var weightedAnomalyScore = 0f
        var totalWeight = 0f

        rawFeatures.forEachIndexed { i, value ->
            val name      = featureNames[i]
            val threshold = NORMAL_THRESHOLDS[name] ?: return@forEachIndexed
            val weight    = FEATURE_WEIGHTS[name] ?: return@forEachIndexed

            // How many times above the normal threshold is this value?
            val ratio = (value / threshold).coerceIn(0f, 5f)
            // Convert ratio to 0-100 score
            // ratio 0 = score 0, ratio 1 = score 20, ratio 5 = score 100
            val featureScore = (ratio / 5f * 100f)

            weightedAnomalyScore += featureScore * weight
            totalWeight += weight
        }

        val finalScore = if (totalWeight > 0f)
            (weightedAnomalyScore / totalWeight).toInt()
        else 0

        return finalScore.coerceIn(0, 100)
    }
}
