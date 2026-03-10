package com.example.sentine.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_risks")
data class AppRiskEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val riskScore: Int,
    val riskLevel: String, // "HIGH", "MEDIUM", "LOW", "SAFE"
    val networkScore: Int,
    val backgroundScore: Int,
    val serviceScore: Int,
    val screenOffScore: Int,
    val lastUpdated: Long,
    val isTrusted: Boolean = false,
    val isRecentlyUsed: Boolean = false,
    val skipReason: String? = null
)
