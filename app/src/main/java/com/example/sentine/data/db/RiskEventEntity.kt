package com.example.sentine.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "risk_events")
data class RiskEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val eventType: String,
    val eventDetail: String,
    val timestamp: Long
)
