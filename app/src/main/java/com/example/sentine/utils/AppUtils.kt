package com.example.sentine.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

object AppUtils {
    fun getInstalledApps(context: Context): List<ApplicationInfo> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA).filter {
            (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }
    }

    fun getAppName(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    fun getUidForPackage(context: Context, packageName: String): Int {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0).uid
        } catch (e: Exception) {
            -1
        }
    }
}
