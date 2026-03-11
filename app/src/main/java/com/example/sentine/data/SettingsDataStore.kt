package com.example.sentine.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> 
    by preferencesDataStore(name = "sentinel_settings")

object SettingsKeys {
    val MONITORING_ENABLED  = booleanPreferencesKey("monitoring_enabled")
    val SCAN_FREQUENCY_MINS = intPreferencesKey("scan_frequency_mins")
    val BATTERY_SAVER       = booleanPreferencesKey("battery_saver")
    val HIGH_RISK_ALERTS    = booleanPreferencesKey("high_risk_alerts")
    val MEDIUM_RISK_ALERTS  = booleanPreferencesKey("medium_risk_alerts")
    val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    val QUIET_HOURS_START   = intPreferencesKey("quiet_hours_start")
    val QUIET_HOURS_END     = intPreferencesKey("quiet_hours_end")
    val FIRST_SCAN_COMPLETED= booleanPreferencesKey("first_scan_completed")
    val ONBOARDING_COMPLETED= booleanPreferencesKey("onboarding_completed")
    val LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
}

data class AppSettings(
    val monitoringEnabled:  Boolean = true,
    val scanFrequencyMins:  Int     = 5,
    val batterySaver:       Boolean = true,
    val highRiskAlerts:     Boolean = true,
    val mediumRiskAlerts:   Boolean = false,
    val quietHoursEnabled:  Boolean = false,
    val quietHoursStart:    Int     = 22,
    val quietHoursEnd:      Int     = 7,
    val firstScanCompleted: Boolean = false,
    val onboardingCompleted:Boolean = false,
    val lastScanTimestamp:  Long    = 0L,
)

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = 
        context.settingsDataStore.data.map { prefs ->
            AppSettings(
                monitoringEnabled = prefs[SettingsKeys.MONITORING_ENABLED]  ?: true,
                scanFrequencyMins = prefs[SettingsKeys.SCAN_FREQUENCY_MINS] ?: 5,
                batterySaver      = prefs[SettingsKeys.BATTERY_SAVER]       ?: true,
                highRiskAlerts    = prefs[SettingsKeys.HIGH_RISK_ALERTS]    ?: true,
                mediumRiskAlerts  = prefs[SettingsKeys.MEDIUM_RISK_ALERTS]  ?: false,
                quietHoursEnabled = prefs[SettingsKeys.QUIET_HOURS_ENABLED] ?: false,
                quietHoursStart   = prefs[SettingsKeys.QUIET_HOURS_START]   ?: 22,
                quietHoursEnd     = prefs[SettingsKeys.QUIET_HOURS_END]     ?: 7,
                firstScanCompleted= prefs[SettingsKeys.FIRST_SCAN_COMPLETED]?: false,
                onboardingCompleted=prefs[SettingsKeys.ONBOARDING_COMPLETED]?: false,
                lastScanTimestamp = prefs[SettingsKeys.LAST_SCAN_TIMESTAMP] ?: 0L,
            )
        }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.MONITORING_ENABLED] = enabled
        }
    }

    suspend fun setScanFrequency(minutes: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.SCAN_FREQUENCY_MINS] = minutes
        }
    }

    suspend fun setBatterySaver(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.BATTERY_SAVER] = enabled
        }
    }

    suspend fun setHighRiskAlerts(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.HIGH_RISK_ALERTS] = enabled
        }
    }

    suspend fun setMediumRiskAlerts(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.MEDIUM_RISK_ALERTS] = enabled
        }
    }

    suspend fun setQuietHours(enabled: Boolean, start: Int, end: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.QUIET_HOURS_ENABLED] = enabled
            prefs[SettingsKeys.QUIET_HOURS_START]   = start
            prefs[SettingsKeys.QUIET_HOURS_END]     = end
        }
    }

    suspend fun clearAllData(context: Context) {
        context.settingsDataStore.edit { it.clear() }
    }

    suspend fun setFirstScanCompleted() {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.FIRST_SCAN_COMPLETED] = true
            prefs[SettingsKeys.LAST_SCAN_TIMESTAMP]  = System.currentTimeMillis()
        }
    }

    suspend fun setOnboardingCompleted() {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun updateLastScanTimestamp() {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.LAST_SCAN_TIMESTAMP] = System.currentTimeMillis()
        }
    }
}
