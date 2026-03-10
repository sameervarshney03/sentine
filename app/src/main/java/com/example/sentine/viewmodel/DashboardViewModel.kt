package com.example.sentine.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sentine.data.db.AppDatabase
import com.example.sentine.data.db.AppRiskEntity
import com.example.sentine.data.db.RiskEventEntity
import com.example.sentine.data.repository.RiskRepository
import com.example.sentine.service.SentinelMonitorService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    val repository: RiskRepository
    val allAppRisks: StateFlow<List<AppRiskEntity>>
    val allEvents: Flow<List<RiskEventEntity>>
    
    val filter = MutableStateFlow("ALL")
    val sortBy = MutableStateFlow("RISK")
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _scanProgressText = MutableStateFlow("")
    val scanProgressText: StateFlow<String> = _scanProgressText

    init {
        val db = AppDatabase.getDatabase(application)
        repository = RiskRepository(db.appRiskDao())
        allEvents = repository.allEvents
        
        allAppRisks = combine(repository.allAppRisks, filter, sortBy) { risks, f, s ->
            val filtered = if (f == "ALL") risks else risks.filter { it.riskLevel == f }
            
            when (s) {
                "RISK" -> filtered.sortedByDescending { it.riskScore }
                "NAME" -> filtered.sortedBy { it.appName }
                "ACTIVITY" -> filtered.sortedByDescending { it.lastUpdated }
                else -> filtered
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun startScan() {
        viewModelScope.launch {
            _isScanning.value = true
            val contexts = listOf(
                "Checking network activity...",
                "Analyzing background patterns...",
                "Calculating risk scores..."
            )
            
            // Start service
            val context = getApplication<Application>()
            val intent = Intent(context, SentinelMonitorService::class.java).apply {
                putExtra("EXTRA_MANUAL_SCAN", true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            // Simulate progress text updates for UI
            var index = 0
            repeat(10) {
                _scanProgressText.value = contexts[index % contexts.size]
                index++
                delay(1500)
            }
            _isScanning.value = false
            _scanProgressText.value = "Scan complete!"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }
}
