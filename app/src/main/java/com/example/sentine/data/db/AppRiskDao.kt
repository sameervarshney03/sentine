package com.example.sentine.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRiskDao {
    @Query("SELECT * FROM app_risks ORDER BY riskScore DESC")
    fun getAllAppRisks(): Flow<List<AppRiskEntity>>

    @Query("SELECT * FROM app_risks WHERE packageName = :packageName")
    suspend fun getAppRisk(packageName: String): AppRiskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppRisk(appRisk: AppRiskEntity)

    @Update
    suspend fun updateAppRisk(appRisk: AppRiskEntity)

    @Query("DELETE FROM app_risks")
    suspend fun deleteAll()

    @Query("SELECT * FROM risk_events WHERE packageName = :packageName ORDER BY timestamp DESC LIMIT 20")
    fun getEventsForApp(packageName: String): Flow<List<RiskEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: RiskEventEntity)

    @Query("SELECT * FROM risk_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<RiskEventEntity>>

    @Query("DELETE FROM risk_events")
    suspend fun deleteAllEvents()
}
