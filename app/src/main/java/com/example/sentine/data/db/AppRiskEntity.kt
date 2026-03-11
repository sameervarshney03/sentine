package com.example.sentine.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_risks")
data class AppRiskEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val riskScore: Int,
    val riskLevel: String, // "HIGH", "MEDIUM", "LOW", "SAFE"
    val mlScore: Int = 0,
    val anomalyScore: Int = 0,
    val networkScore: Int = 0,
    val backgroundScore: Int = 0,
    val serviceScore: Int = 0,
    val screenOffScore: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isTrusted: Boolean = false,
    val trustedUntil: Long = 0L,
    val isRecentlyUsed: Boolean = false,
    val skipReason: String? = null,
    val reasons: String = ""
)
