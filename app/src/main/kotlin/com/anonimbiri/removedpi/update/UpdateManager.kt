package com.anonimbiri.removedpi.update

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val version: String,
    val versionCode: Int,
    val releaseNotes: String,
    val debugApkUrl: String,
    val releaseApkUrl: String,
    val publishedAt: String,
    val isPrerelease: Boolean
)

data class VersionInfo(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String?,
    val preReleaseNum: Int
)

object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val GITHUB_RELEASES_API = "https://api.github.com/repos/GameSketchers/RemoveDPI/releases"
    private const val GITHUB_TAG_API = "https://api.github.com/repos/GameSketchers/RemoveDPI/releases/tags/"
    
    suspend fun checkForUpdates(context: Context): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val currentVersionName = getCurrentVersionName(context)
            Log.d(TAG, "Current app version: $currentVersionName")
            
            val latestRelease = fetchLatestRelease()
            if (latestRelease == null) {
                Log.e(TAG, "Failed to fetch latest release")
                return@withContext null
            }
            
            val remoteVersion = latestRelease.getString("tag_name").removePrefix("v").removePrefix("V")
            Log.d(TAG, "Latest remote version: $remoteVersion")
            
            val comparison = compareVersions(remoteVersion, currentVersionName)
            Log.d(TAG, "Version comparison result: $comparison (positive = update available)")
            
            if (comparison > 0) {
                val releaseInfo = parseReleaseInfo(latestRelease)
                Log.d(TAG, "Update available: ${releaseInfo?.version}")
                return@withContext releaseInfo
            } else {
                Log.d(TAG, "Already on latest version or newer")
                return@withContext null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            return@withContext null
        }
    }
    
    suspend fun getReleaseByTag(tag: String): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val cleanTag = tag.removePrefix("v").removePrefix("V")
            Log.d(TAG, "Fetching release for tag: $cleanTag")
            
            var json = fetchReleaseByTag("v$cleanTag")
            
            if (json == null) {
                json = fetchReleaseByTag(cleanTag)
            }
            
            if (json != null) {
                return@withContext parseReleaseInfo(json)
            }
            
            Log.d(TAG, "Release not found for tag: $cleanTag")
            return@withContext null
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch release by tag", e)
            return@withContext null
        }
    }
    
    private fun fetchLatestRelease(): JSONObject? {
        return try {
            val url = "$GITHUB_RELEASES_API?per_page=1"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "RemoveDPI-App")
                connectTimeout = 15000
                readTimeout = 15000
            }
            
            Log.d(TAG, "Fetching: $url")
            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")
            
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                if (jsonArray.length() > 0) {
                    jsonArray.getJSONObject(0)
                } else null
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "API error: $responseCode - $error")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchLatestRelease failed", e)
            null
        }
    }
    
    private fun fetchReleaseByTag(tag: String): JSONObject? {
        return try {
            val url = "$GITHUB_TAG_API$tag"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "RemoveDPI-App")
                connectTimeout = 15000
                readTimeout = 15000
            }
            
            Log.d(TAG, "Fetching tag: $url")
            val responseCode = connection.responseCode
            
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(response)
            } else {
                Log.d(TAG, "Tag not found: $tag (code: $responseCode)")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchReleaseByTag failed for: $tag", e)
            null
        }
    }
    
    private fun parseReleaseInfo(json: JSONObject): ReleaseInfo? {
        return try {
            val tagName = json.optString("tag_name", "")
            if (tagName.isEmpty()) return null
            
            val version = tagName.removePrefix("v").removePrefix("V")
            
            val assets = json.optJSONArray("assets")
            var debugUrl = ""
            var releaseUrl = ""
            
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    val downloadUrl = asset.optString("browser_download_url", "")
                    
                    when {
                        name.equals("app-debug.apk", ignoreCase = true) -> debugUrl = downloadUrl
                        name.equals("app-release.apk", ignoreCase = true) -> releaseUrl = downloadUrl
                    }
                }
            }
            
            ReleaseInfo(
                version = version,
                versionCode = calculateVersionCode(version),
                releaseNotes = json.optString("body", ""),
                debugApkUrl = debugUrl,
                releaseApkUrl = releaseUrl,
                publishedAt = json.optString("published_at", ""),
                isPrerelease = json.optBoolean("prerelease", false)
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseReleaseInfo failed", e)
            null
        }
    }
    
    private fun parseVersion(version: String): VersionInfo {
        val clean = version.removePrefix("v").removePrefix("V").trim()
        
        val parts = clean.split("-", limit = 2)
        val versionPart = parts[0]
        val preReleasePart = parts.getOrNull(1)
        
        val versionNumbers = versionPart.split(".")
        val major = versionNumbers.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = versionNumbers.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = versionNumbers.getOrNull(2)?.toIntOrNull() ?: 0
        
        var preRelease: String? = null
        var preReleaseNum = 0
        
        if (preReleasePart != null) {
            val preParts = preReleasePart.split(".")
            preRelease = preParts.getOrNull(0)
            preReleaseNum = preParts.getOrNull(1)?.toIntOrNull() ?: 0
        }
        
        return VersionInfo(major, minor, patch, preRelease, preReleaseNum)
    }
    
    fun compareVersions(version1: String, version2: String): Int {
        val v1 = parseVersion(version1)
        val v2 = parseVersion(version2)
        
        Log.d(TAG, "Comparing: $v1 vs $v2")
        
        if (v1.major != v2.major) return v1.major - v2.major
        
        if (v1.minor != v2.minor) return v1.minor - v2.minor
        
        if (v1.patch != v2.patch) return v1.patch - v2.patch
        
        val preReleaseOrder = mapOf(
            null to 100,
            "stable" to 100,
            "release" to 100,
            "rc" to 30,
            "beta" to 20,
            "alpha" to 10
        )
        
        val v1PreOrder = preReleaseOrder[v1.preRelease?.lowercase()] ?: 0
        val v2PreOrder = preReleaseOrder[v2.preRelease?.lowercase()] ?: 0
        
        if (v1PreOrder != v2PreOrder) return v1PreOrder - v2PreOrder
        
        return v1.preReleaseNum - v2.preReleaseNum
    }
    
    private fun calculateVersionCode(version: String): Int {
        val v = parseVersion(version)
        return v.major * 100000 + v.minor * 1000 + v.patch * 10 + v.preReleaseNum
    }
    
    fun getCurrentVersionName(context: Context): String {
        return try {
            val versionName = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "0.0.0"
            versionName.removePrefix("v").removePrefix("V")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get version name", e)
            "0.0.0"
        }
    }
    
    fun isDebugBuild(context: Context): Boolean {
        return try {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }
}