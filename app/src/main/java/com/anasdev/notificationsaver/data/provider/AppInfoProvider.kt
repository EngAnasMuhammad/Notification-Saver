package com.anasdev.notificationsaver.data.provider

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.anasdev.notificationsaver.data.local.NotificationDao
import com.anasdev.notificationsaver.domain.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: NotificationDao
) {
    suspend fun getInstalledApps(blacklistedPackages: Set<String> = emptySet()): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedApps = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }
        } catch (_: Exception) {
            emptyList()
        }

        // Get packages currently present in the Room database so system apps that sent notifications can still be managed
        val dbPackages = try {
            dao.getDistinctPackages().toSet()
        } catch (_: Exception) {
            emptySet()
        }

        val appList = installedApps.mapNotNull { appInfo ->
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val hasLaunchIntent = pm.getLaunchIntentForPackage(appInfo.packageName) != null
            val isInDb = appInfo.packageName in dbPackages
            val isBlacklisted = appInfo.packageName in blacklistedPackages

            // Always keep app if it has launch intent OR is not system OR is in DB OR is blacklisted
            if (hasLaunchIntent || !isSystemApp || isInDb || isBlacklisted) {
                val appName = try {
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    appInfo.packageName
                }
                AppInfo(packageName = appInfo.packageName, appName = appName)
            } else {
                null
            }
        }.toMutableList()

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
