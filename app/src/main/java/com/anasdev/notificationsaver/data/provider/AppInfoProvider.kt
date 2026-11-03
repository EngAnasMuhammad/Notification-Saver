package com.anasdev.notificationsaver.data.provider

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.anasdev.notificationsaver.data.local.NotificationDao
import com.anasdev.notificationsaver.domain.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System package provider querying installed applications and cross-referencing
 * saved database records and user blacklist configurations.
 */
@Singleton
class AppInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: NotificationDao,
) {
    @Volatile
    private var cachedBaseApps: List<AppInfo>? = null

    /**
     * Clears cached base apps when an application list needs to be re-evaluated.
     */
    fun refresh() {
        cachedBaseApps = null
    }

    /**
     * Retrieves the list of user-facing and active notification applications.
     * Caches the base system PackageManager scan so blacklist toggling is instantaneous.
     */
    @SuppressLint("QueryPermissionsNeeded")
    suspend fun getInstalledApps(blacklistedPackages: Set<String> = emptySet()): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager

        var baseList = cachedBaseApps
        if (baseList == null) {
            val installedApps = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                }
            } catch (_: Exception) {
                emptyList()
            }

            // Retrieve packages currently stored in the Room database so system apps can be managed
            val dbPackages = try {
                dao.getDistinctPackages().toSet()
            } catch (_: Exception) {
                emptySet()
            }

            baseList = installedApps.mapNotNull { appInfo ->
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val hasLaunchIntent = pm.getLaunchIntentForPackage(appInfo.packageName) != null
                val isInDb = appInfo.packageName in dbPackages

                if (hasLaunchIntent || !isSystemApp || isInDb) {
                    val appName = try {
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        appInfo.packageName
                    }
                    AppInfo(packageName = appInfo.packageName, appName = appName)
                } else {
                    null
                }
            }
            cachedBaseApps = baseList
        }

        val appList = baseList.toMutableList()
        val existingPackages = appList.map { it.packageName }.toSet()
        val missingBlacklisted = blacklistedPackages - existingPackages

        for (pkg in missingBlacklisted) {
            val appName = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                pkg.substringAfterLast('.')
            }
            appList.add(AppInfo(packageName = pkg, appName = appName))
        }

        appList.distinctBy { it.packageName }
    }
}
