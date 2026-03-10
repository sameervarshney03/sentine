package com.example.sentine.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sentine.data.db.AppDatabase
import com.example.sentine.data.db.AppRiskEntity
import com.example.sentine.data.db.RiskEventEntity
import com.example.sentine.data.repository.RiskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RiskRepository
    private val _appRisk = MutableStateFlow<AppRiskEntity?>(null)
    val appRisk: StateFlow<AppRiskEntity?> = _appRisk

    init {
        val db = AppDatabase.getDatabase(application)
        repository = RiskRepository(db.appRiskDao())
    }

    fun loadAppDetails(packageName: String) {
        viewModelScope.launch {
            _appRisk.value = repository.getAppRisk(packageName)
        }
    }

    fun getEvents(packageName: String): Flow<List<RiskEventEntity>> {
        return repository.getEventsForApp(packageName)
    }

    fun toggleTrusted(packageName: String) {
        viewModelScope.launch {
            val current = repository.getAppRisk(packageName)
            current?.let {
                val updated = it.copy(isTrusted = !it.isTrusted, riskLevel = if (!it.isTrusted) "SAFE" else it.riskLevel)
                repository.insertAppRisk(updated)
                _appRisk.value = updated
            }
        }
    }
}
