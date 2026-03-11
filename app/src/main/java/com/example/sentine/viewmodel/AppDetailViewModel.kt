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
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import kotlinx.coroutines.launch

data class AppPermission(
    val name: String,
    val humanReadableName: String,
    val description: String,
    val category: String // "DANGEROUS", "NETWORK", "NORMAL"
)

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
                val isTrustingNow = !it.isTrusted
                val trustedUntilDate = if (isTrustingNow) System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000L else 0L
                val newLevel = if (isTrustingNow) "SAFE" else it.riskLevel
                
                val updated = it.copy(isTrusted = isTrustingNow, trustedUntil = trustedUntilDate, riskLevel = newLevel)
                repository.insertAppRisk(updated)
                _appRisk.value = updated
                
                if (isTrustingNow) {
                    try {
                        val context = getApplication<Application>()
                        val sharedPref = context.getSharedPreferences("sentinel_prefs", android.content.Context.MODE_PRIVATE)
                        val uid = context.packageManager.getApplicationInfo(packageName, 0).uid
                        val uploadBytes = android.net.TrafficStats.getUidTxBytes(uid)
                        val uploadKb = (uploadBytes.takeIf { bytes -> bytes != android.net.TrafficStats.UNSUPPORTED.toLong() } ?: 0L) / 1024f
                        val baselineKb = sharedPref.getFloat("baseline_$packageName", 0f)
                        var currentRate = 0f
                        if (uploadKb >= baselineKb) {
                            currentRate = (uploadKb - baselineKb) / 5f
                        }
                        // Default to 1.0f if very low to prevent simple noise triggering 3x spike
                        sharedPref.edit().putFloat("trust_baseline_$packageName", maxOf(currentRate, 1.0f)).apply()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
    }

    fun getAppPermissions(packageName: String): List<AppPermission> {
        val pm = getApplication<Application>().packageManager
        val permissions = mutableListOf<AppPermission>()
        try {
            val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            packageInfo.requestedPermissions?.forEach { permName ->
                try {
                    val permInfo = pm.getPermissionInfo(permName, 0)
                    val isDangerous = (permInfo.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS
                    val isNetwork = permName.contains("INTERNET") || permName.contains("NETWORK") || permName.contains("WIFI") || permName.contains("BLUETOOTH")
                    
                    val category = when {
                        isDangerous -> "DANGEROUS"
                        isNetwork -> "NETWORK"
                        else -> "NORMAL"
                    }

                    val humanReadable = permInfo.loadLabel(pm).toString().replaceFirstChar { it.uppercase() }
                    val desc = permInfo.loadDescription(pm)?.toString() ?: "Provides access to ${humanReadable.lowercase()}"
                    
                    permissions.add(AppPermission(permName, humanReadable, desc, category))
                } catch (e: Exception) {
                    val isNetwork = permName.contains("INTERNET") || permName.contains("NETWORK") || permName.contains("WIFI") || permName.contains("BLUETOOTH")
                    val category = if (isNetwork) "NETWORK" else "NORMAL"
                    val simpleName = permName.substringAfterLast(".").replace("_", " ")
                    permissions.add(AppPermission(permName, simpleName, "Unknown permission", category))
                }
            }
        } catch (e: Exception) {
            // App not found
        }
        return permissions.sortedBy { 
            when(it.category) { "DANGEROUS" -> 0; "NETWORK" -> 1; else -> 2 } 
        }
    }
}
