package com.example.sentine.engine

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.flex.FlexDelegate

class MLRiskEngine(private val context: Context) {

    private val TAG = "MLRiskEngine"
    private var interpreter: Interpreter? = null

    // Scaler parameters loaded from JSON
    private lateinit var classifierMean:  FloatArray
    private lateinit var classifierScale: FloatArray

    // Anomaly scaler parameters
    private lateinit var anomalyMean:  FloatArray
    private lateinit var anomalyScale: FloatArray

    // Feature importances for explanation
    private var featureImportances: Map<String, Float> = emptyMap()

    private val FEATURE_NAMES = listOf(
        "network_upload_rate",
        "session_length",
        "screen_off_activity",
        "background_activity",
        "packet_count",
        "upload_variance"
    )

    data class MLRiskResult(
        val mlScore: Int,               // 0-100 from classifier
        val anomalyScore: Int,          // 0-100 from anomaly detection
        val combinedScore: Int,         // weighted final
        val riskProbability: Float,     // raw 0.0-1.0 from model
        val topReasons: List<String>,   // plain English explanations
        val isInitialized: Boolean      // false if model failed to load
    )

    // ─────────────────────────────────────────────
    // INITIALIZE — call once when app starts
    // ─────────────────────────────────────────────
    fun initialize(): Boolean {
        return try {
            // Load TFLite model
            val modelBytes = context.assets.open("sentinel_classifier.tflite")
                .readBytes()
            val modelBuffer = ByteBuffer
                .allocateDirect(modelBytes.size)
                .apply {
                    order(ByteOrder.nativeOrder())
                    put(modelBytes)
                    rewind()
                }
            val options = Interpreter.Options().apply {
    addDelegate(
        org.tensorflow.lite.flex.FlexDelegate()
    )
    setNumThreads(2)
}
interpreter = Interpreter(modelBuffer, options)

            // Load classifier scaler params
            val scalerJson = context.assets.open("scaler_params.json")
                .bufferedReader().readText()
            val scalerObj = JSONObject(scalerJson)
            val meanArr   = scalerObj.getJSONArray("mean")
            val scaleArr  = scalerObj.getJSONArray("scale")
            classifierMean  = FloatArray(meanArr.length())  { meanArr.getDouble(it).toFloat() }
            classifierScale = FloatArray(scaleArr.length()) { scaleArr.getDouble(it).toFloat() }

            // Load anomaly scaler params
            val anomalyJson = context.assets.open("anomaly_scaler.json")
                .bufferedReader().readText()
            val anomalyObj = JSONObject(anomalyJson)
            val aMeanArr   = anomalyObj.getJSONArray("mean")
            val aScaleArr  = anomalyObj.getJSONArray("scale")
            anomalyMean  = FloatArray(aMeanArr.length())  { aMeanArr.getDouble(it).toFloat() }
            anomalyScale = FloatArray(aScaleArr.length()) { aScaleArr.getDouble(it).toFloat() }

            // Load feature importances
            val fiJson = context.assets.open("feature_importances.json")
                .bufferedReader().readText()
            val fiObj = JSONObject(fiJson)
            featureImportances = FEATURE_NAMES.associateWith { name ->
                fiObj.optDouble(name, 0.0).toFloat()
            }

            Log.d(TAG, "MLRiskEngine initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MLRiskEngine: ${e.message}")
            false
        }
    }

    // ─────────────────────────────────────────────
    // ANALYZE — call for each app to get ML score
    // ─────────────────────────────────────────────
    fun analyze(signals: AppSignals): MLRiskResult {
        // Log the REAL input values going into the model
        Log.d("MLRiskEngine", """
            ═══ ML INFERENCE: ${signals.packageName} ═══
            networkUploadKbPerMin  = ${signals.networkUploadKbPerMin}
            sessionLengthSeconds   = ${signals.sessionLengthSeconds}
            screenOffUploadKb      = ${signals.screenOffUploadKb}
            backgroundActivity     = ${signals.backgroundActivityScore}
            packetCount            = ${signals.packetCount}
            uploadVariance         = ${signals.uploadVariance}
        """.trimIndent())

        if (interpreter == null) {
            Log.e("MLRiskEngine", "INTERPRETER IS NULL — model not loaded!")
            // Return rule-based fallback, NOT a fake score
            return MLRiskResult(
                mlScore         = -1,  // -1 means ML unavailable
                anomalyScore    = -1,
                combinedScore   = -1,
                riskProbability = -1f,
                topReasons      = listOf("ML model unavailable — using behavioral signals only"),
                isInitialized   = false
            )
        }

        // Build REAL feature vector from REAL signals
        val rawFeatures = floatArrayOf(
            signals.networkUploadKbPerMin,
            signals.sessionLengthSeconds,
            signals.screenOffUploadKb,
            signals.backgroundActivityScore * 100f,
            signals.packetCount.toFloat(),
            signals.uploadVariance
        )

        // Normalize using REAL scaler params from JSON
        val normalized = FloatArray(rawFeatures.size) { i ->
            if (classifierScale[i] != 0f)
                (rawFeatures[i] - classifierMean[i]) / classifierScale[i]
            else 0f
        }

        // Run REAL TFLite inference
        val inputBuffer  = Array(1) { normalized }
        val outputBuffer = Array(1) { FloatArray(2) }
        
        try {
            interpreter!!.run(inputBuffer, outputBuffer)
        } catch (e: Exception) {
            Log.e("MLRiskEngine", "Inference FAILED: ${e.message}")
            return MLRiskResult(
                mlScore = -1, anomalyScore = -1,
                combinedScore = -1, riskProbability = -1f,
                topReasons = listOf("ML inference error: ${e.message}"),
                isInitialized = false
            )
        }

        val safeProbability = outputBuffer[0][0]
        val riskProbability = outputBuffer[0][1]
        val mlScore = (riskProbability * 100).toInt().coerceIn(0, 100)

        // Log REAL output from model
        Log.d("MLRiskEngine", """
            ═══ ML OUTPUT: ${signals.packageName} ═══
            safeProbability = $safeProbability
            riskProbability = $riskProbability
            mlScore         = $mlScore
        """.trimIndent())

        val anomalyInput = FloatArray(rawFeatures.size) { i ->
            if (anomalyScale[i] != 0f)
                (rawFeatures[i] - anomalyMean[i]) / anomalyScale[i]
            else 0f
        }
        val deviationMagnitude = anomalyInput.map { it * it }.sum() / anomalyInput.size
        val anomalyScore = (deviationMagnitude / 5.0 * 100.0).toInt().coerceIn(0, 100)

        val combinedScore = ((mlScore * 0.65f) + 
                             (anomalyScore * 0.35f)).toInt()
                             .coerceIn(0, 100)

        return MLRiskResult(
            mlScore         = mlScore,
            anomalyScore    = anomalyScore,
            combinedScore   = combinedScore,
            riskProbability = riskProbability,
            topReasons      = buildExplanations(signals, riskProbability, rawFeatures),
            isInitialized   = true
        )
    }

    // ─────────────────────────────────────────────
    // EXPLANATION — translate scores to English
    // ─────────────────────────────────────────────
    private fun buildExplanations(
        signals: AppSignals,
        probability: Float,
        rawFeatures: FloatArray
    ): List<String> {
        val reasons = mutableListOf<String>()

        // Network upload
        when {
            signals.networkUploadKbPerMin > 1000 ->
                reasons.add("Uploading ${signals.networkUploadKbPerMin.toInt()} KB/min — extremely high for a background app")
            signals.networkUploadKbPerMin > 300 ->
                reasons.add("Unusual network upload in background (${signals.networkUploadKbPerMin.toInt()} KB/min)")
        }

        // Screen-off uploads
        if (signals.screenOffUploadKb > 200) {
            reasons.add("Sent ${signals.screenOffUploadKb.toInt()} KB while your screen was off and device was idle")
        }

        // Background activity
        if (signals.backgroundActivityScore > 0.7f) {
            reasons.add("Very active in background without user interaction")
        }

        // Background wake count
        if (signals.backgroundWakeCount > 8) {
            reasons.add("Woke itself up ${signals.backgroundWakeCount} times in background")
        }

        // Foreground service without user opening app
        if (signals.hasForegroundService && signals.serviceRunningMinutes > 30) {
            reasons.add("Running a persistent background service for ${signals.serviceRunningMinutes} minutes")
        }

        // High ML confidence
        if (probability > 0.85f) {
            reasons.add("Behavior pattern closely matches known malicious app profiles")
        } else if (probability > 0.70f) {
            reasons.add("Some behavioral similarities to known threat patterns")
        }

        return reasons.ifEmpty {
            listOf("App behavior is within normal parameters")
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
