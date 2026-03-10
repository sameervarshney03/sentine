package com.example.sentine.data.repository

import com.example.sentine.data.db.AppRiskDao
import com.example.sentine.data.db.AppRiskEntity
import com.example.sentine.data.db.RiskEventEntity
import kotlinx.coroutines.flow.Flow

class RiskRepository(private val appRiskDao: AppRiskDao) {
    val allAppRisks: Flow<List<AppRiskEntity>> = appRiskDao.getAllAppRisks()
    val allEvents: Flow<List<RiskEventEntity>> = appRiskDao.getAllEvents()

    suspend fun getAppRisk(packageName: String): AppRiskEntity? = appRiskDao.getAppRisk(packageName)
    
    fun getEventsForApp(packageName: String): Flow<List<RiskEventEntity>> = appRiskDao.getEventsForApp(packageName)

    suspend fun insertAppRisk(appRisk: AppRiskEntity) = appRiskDao.insertAppRisk(appRisk)

    suspend fun insertEvent(event: RiskEventEntity) = appRiskDao.insertEvent(event)

    suspend fun deleteAll() {
        appRiskDao.deleteAll()
        appRiskDao.deleteAllEvents()
    }
}
