package com.example.sentine.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.sentine.MainActivity

class SentinelTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val sharedPref = getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE)
        val currentState = sharedPref.getBoolean("monitoring_enabled", true)
        
        val newState = !currentState
        sharedPref.edit().putBoolean("monitoring_enabled", newState).apply()
        
        if (newState) {
            val intent = Intent(this, SentinelMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            val intent = Intent(this, SentinelMonitorService::class.java)
            stopService(intent)
        }
        
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val sharedPref = getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE)
        val isEnabled = sharedPref.getBoolean("monitoring_enabled", true)

        if (isEnabled) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Active"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Inactive"
            }
        }
        
        tile.updateTile()
    }
}
