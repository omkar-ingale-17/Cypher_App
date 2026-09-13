package com.cypher.assistant.features.applications.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.cypher.assistant.core.command.CommandResult
import com.cypher.assistant.features.applications.data.launcher.AndroidApplicationLauncher
import com.cypher.assistant.features.applications.domain.matcher.ApplicationMatcher
import com.cypher.assistant.features.applications.domain.model.AppInfo
import com.cypher.assistant.features.applications.domain.model.AppMatchResult
import com.cypher.assistant.features.applications.domain.model.LaunchResult
import com.cypher.assistant.features.applications.domain.repository.ApplicationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe, cached implementation of [ApplicationRepository].
 */
@Singleton
class ApplicationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val matcher: ApplicationMatcher,
    private val launcher: AndroidApplicationLauncher
) : ApplicationRepository {

    companion object {
        private const val TAG = "CYPHER_APP_REPO"
        private const val CACHE_EXPIRY_MS = 60_000L // 1 minute cache TTL
    }

    private var cachedApps: List<AppInfo> = emptyList()
    private var lastCacheTime: Long = 0L

    override suspend fun getInstalledApplications(forceRefresh: Boolean): List<AppInfo> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedApps.isNotEmpty() && (now - lastCacheTime < CACHE_EXPIRY_MS)) {
            return@withContext cachedApps
        }

        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = try {
            pm.queryIntentActivities(mainIntent, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying intent activities", e)
            emptyList()
        }

        val apps = resolveInfos.mapNotNull { resolveInfo ->
            try {
                val pkgName = resolveInfo.activityInfo.packageName
                // Skip Cypher itself in general app query list
                if (pkgName == context.packageName) return@mapNotNull null

                val appLabel = resolveInfo.loadLabel(pm).toString().trim()
                if (appLabel.isBlank()) return@mapNotNull null

                val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val icon = resolveInfo.loadIcon(pm)

                AppInfo(
                    packageName = pkgName,
                    appName = appLabel,
                    launchActivityName = resolveInfo.activityInfo.name,
                    isSystemApp = isSystem,
                    icon = icon
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error resolving app info", e)
                null
            }
        }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }

        cachedApps = apps
        lastCacheTime = now
        Log.i(TAG, "Indexed ${apps.size} launchable applications")
        apps
    }

    override suspend fun searchApplications(query: String): List<AppInfo> = withContext(Dispatchers.Default) {
        val apps = getInstalledApplications()
        if (query.isBlank()) return@withContext apps

        val normalized = matcher.normalize(query)
        apps.filter { app ->
            val normName = matcher.normalize(app.appName)
            normName.contains(normalized) || app.packageName.contains(normalized)
        }
    }

    override suspend fun resolveApplication(query: String): AppMatchResult = withContext(Dispatchers.Default) {
        val apps = getInstalledApplications()
        matcher.match(query, apps)
    }

    override suspend fun launchApplication(packageName: String): LaunchResult = withContext(Dispatchers.Main) {
        val apps = getInstalledApplications()
        val targetApp = apps.firstOrNull { it.packageName == packageName }
            ?: return@withContext LaunchResult.NotFound(
                query = packageName,
                message = "I couldn't find that application on this phone."
            )

        launcher.launch(targetApp)
    }

    override suspend fun openApplicationByName(appNameQuery: String): LaunchResult = withContext(Dispatchers.Default) {
        val matchResult = resolveApplication(appNameQuery)

        when (matchResult) {
            is AppMatchResult.Match -> {
                withContext(Dispatchers.Main) {
                    launcher.launch(matchResult.appInfo)
                }
            }

            is AppMatchResult.Ambiguous -> {
                val candidateNames = matchResult.candidates.take(3).joinToString(" and ") { it.appName }
                LaunchResult.Ambiguous(
                    candidates = matchResult.candidates,
                    message = "I found multiple applications matching that name: $candidateNames. Which one should I open?"
                )
            }

            is AppMatchResult.NotFound -> {
                LaunchResult.NotFound(
                    query = appNameQuery,
                    message = "I couldn't find $appNameQuery on this phone."
                )
            }
        }
    }

    override suspend fun openSystemScreen(screenType: String): LaunchResult = withContext(Dispatchers.Main) {
        launcher.launchSystemScreen(screenType)
    }

    override suspend fun launchHome(): LaunchResult = withContext(Dispatchers.Main) {
        launcher.launchHome()
    }

    override suspend fun goBack(): CommandResult = withContext(Dispatchers.Main) {
        launcher.goBack()
    }

    override suspend fun lockScreen(): CommandResult = withContext(Dispatchers.Main) {
        launcher.lockScreen()
    }

    override suspend fun closeApp(appName: String): CommandResult = withContext(Dispatchers.Main) {
        launcher.closeApp(appName)
    }
}
