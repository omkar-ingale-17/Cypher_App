package com.cypher.assistant.features.applications.domain.matcher

import com.cypher.assistant.features.applications.domain.model.AppInfo
import com.cypher.assistant.features.applications.domain.model.AppMatchResult
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Natural language resolver that matches raw spoken text to installed applications.
 *
 * Employs a multi-tier matching strategy:
 * 1. Exact Name Matching (case-insensitive)
 * 2. Exact Package Name Matching
 * 3. Canonical Alias / Synonym Lookup (e.g., "insta" -> "Instagram", "you tube" -> "YouTube")
 * 4. Word-Boundary / Prefix / Substring Matching
 * 5. Safe Fuzzy Levenshtein Matching (confidence threshold >= 0.75)
 */
@Singleton
class ApplicationMatcher @Inject constructor() {

    private val aliasMap: Map<String, List<String>> = mapOf(
        "youtube" to listOf("youtube", "yt", "you tube"),
        "yt" to listOf("youtube"),
        "you tube" to listOf("youtube"),
        "whatsapp" to listOf("whatsapp", "whats app", "what s app", "wa"),
        "wa" to listOf("whatsapp"),
        "whats app" to listOf("whatsapp"),
        "what s app" to listOf("whatsapp"),
        "instagram" to listOf("instagram", "insta", "ig", "insta gram"),
        "insta" to listOf("instagram"),
        "ig" to listOf("instagram"),
        "insta gram" to listOf("instagram"),
        "chrome" to listOf("chrome", "google chrome", "browser"),
        "google chrome" to listOf("chrome"),
        "chrome browser" to listOf("chrome"),
        "spotify" to listOf("spotify", "spotify music"),
        "maps" to listOf("maps", "google maps"),
        "google maps" to listOf("maps", "google maps"),
        "gmail" to listOf("gmail", "google mail", "email", "g mail"),
        "g mail" to listOf("gmail"),
        "google mail" to listOf("gmail"),
        "email" to listOf("gmail", "email", "mail"),
        "telegram" to listOf("telegram"),
        "discord" to listOf("discord"),
        "snapchat" to listOf("snapchat", "snap"),
        "snap" to listOf("snapchat"),
        "twitter" to listOf("x", "twitter"),
        "x" to listOf("x", "twitter"),
        "facebook" to listOf("facebook", "fb", "face book"),
        "fb" to listOf("facebook"),
        "face book" to listOf("facebook"),
        "settings" to listOf("settings", "system settings"),
        "system settings" to listOf("settings"),
        "camera" to listOf("camera"),
        "gallery" to listOf("gallery", "photos", "google photos"),
        "photos" to listOf("photos", "google photos", "gallery"),
        "google photos" to listOf("photos", "google photos"),
        "messages" to listOf("messages", "sms", "messaging"),
        "sms" to listOf("messages", "sms"),
        "phone" to listOf("phone", "dialer", "call"),
        "dialer" to listOf("phone", "dialer"),
        "play store" to listOf("google play store", "play store", "google play", "playstore"),
        "playstore" to listOf("google play store", "play store"),
        "google play" to listOf("google play store", "play store"),
        "files" to listOf("files", "file manager", "my files"),
        "file manager" to listOf("files", "file manager"),
        "calculator" to listOf("calculator"),
        "calendar" to listOf("calendar", "google calendar"),
        "clock" to listOf("clock", "alarm", "timer"),
        "alarm" to listOf("clock", "alarm"),
        "netflix" to listOf("netflix"),
        "linkedin" to listOf("linkedin"),
        "amazon" to listOf("amazon", "amazon shopping"),
        "uber" to listOf("uber"),
        "swiggy" to listOf("swiggy"),
        "zomato" to listOf("zomato")
    )

    /**
     * Finds the best matching application among [installedApps] for the given [query].
     */
    fun match(query: String, installedApps: List<AppInfo>): AppMatchResult {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank() || installedApps.isEmpty()) {
            return AppMatchResult.NotFound(query)
        }

        // 1. Exact Match on App Name (Normalized)
        val exactMatch = installedApps.firstOrNull { normalize(it.appName) == normalizedQuery }
        if (exactMatch != null) {
            return AppMatchResult.Match(exactMatch, confidence = 1.0f)
        }

        // 2. Exact Match on Package Name
        val packageMatch = installedApps.firstOrNull { it.packageName.lowercase(Locale.ROOT) == normalizedQuery }
        if (packageMatch != null) {
            return AppMatchResult.Match(packageMatch, confidence = 1.0f)
        }

        // 3. Check Alias Map
        val targetAliases = aliasMap[normalizedQuery] ?: emptyList()
        if (targetAliases.isNotEmpty()) {
            val aliasMatches = installedApps.filter { app ->
                val normName = normalize(app.appName)
                val normPkg = app.packageName.lowercase(Locale.ROOT)
                targetAliases.any { alias ->
                    normName == alias ||
                    normName.contains(alias) ||
                    normPkg.contains(alias.replace(" ", ""))
                }
            }

            if (aliasMatches.size == 1) {
                return AppMatchResult.Match(aliasMatches.first(), confidence = 0.98f, matchedAlias = normalizedQuery)
            } else if (aliasMatches.size > 1) {
                val primary = aliasMatches.firstOrNull { normalize(it.appName) in targetAliases }
                if (primary != null) {
                    return AppMatchResult.Match(primary, confidence = 0.95f, matchedAlias = normalizedQuery)
                }
                return AppMatchResult.Ambiguous(query, aliasMatches)
            }
        }

        // 4. Word-Boundary / Prefix / Contains Match
        val wordMatches = installedApps.filter { app ->
            val normName = normalize(app.appName)
            normName.startsWith(normalizedQuery) ||
            normName.split(" ").any { it == normalizedQuery } ||
            (normalizedQuery.length >= 4 && normName.contains(normalizedQuery))
        }

        if (wordMatches.size == 1) {
            return AppMatchResult.Match(wordMatches.first(), confidence = 0.90f)
        } else if (wordMatches.size > 1) {
            val singleExact = wordMatches.firstOrNull { normalize(it.appName) == normalizedQuery }
            if (singleExact != null) {
                return AppMatchResult.Match(singleExact, confidence = 0.95f)
            }
            return AppMatchResult.Ambiguous(query, wordMatches)
        }

        // 5. Safe Fuzzy Match (Levenshtein Distance)
        var bestScore = 0.0f
        val topCandidates = mutableListOf<Pair<AppInfo, Float>>()

        for (app in installedApps) {
            val normName = normalize(app.appName)
            val score = calculateSimilarity(normalizedQuery, normName)
            if (score >= 0.75f) { // Strict confidence threshold to avoid false positives
                topCandidates.add(Pair(app, score))
                if (score > bestScore) {
                    bestScore = score
                }
            }
        }

        val strongMatches = topCandidates.filter { it.second >= bestScore - 0.05f }
        return when {
            strongMatches.isEmpty() -> AppMatchResult.NotFound(query)
            strongMatches.size == 1 -> AppMatchResult.Match(strongMatches.first().first, confidence = strongMatches.first().second)
            else -> AppMatchResult.Ambiguous(query, strongMatches.map { it.first })
        }
    }

    /**
     * Normalizes query string: strips conversational fillers, punctuation, and converts to lowercase.
     */
    fun normalize(text: String): String {
        var clean = text.trim().lowercase(Locale.ROOT)

        // Pre-normalize common compound app names
        clean = clean.replace("what's app", "whatsapp")
            .replace("whats app", "whatsapp")
            .replace("what s app", "whatsapp")
            .replace("you tube", "youtube")
            .replace("face book", "facebook")
            .replace("insta gram", "instagram")
            .replace("g mail", "gmail")
            .replace("play store", "play store")
            .replace("playstore", "play store")

        // Remove filler prefixes
        val prefixes = listOf("my", "the", "app", "application", "please", "can you", "could you", "open", "launch", "start", "run", "go to", "find", "search for")
        for (prefix in prefixes) {
            val regex = Regex("""^(?:$prefix)\b\s*""", RegexOption.IGNORE_CASE)
            clean = regex.replace(clean, "")
        }

        // Remove trailing fillers
        val suffixes = listOf("app", "application", "for me", "please")
        for (suffix in suffixes) {
            val regex = Regex("""\s*\b(?:$suffix)$""", RegexOption.IGNORE_CASE)
            clean = regex.replace(clean, "")
        }

        // Clean punctuation & extra whitespace
        return clean.replace(Regex("""[^\w\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * Calculates normalized string similarity (0.0 to 1.0) using Levenshtein distance.
     */
    fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0 || len2 == 0) return 0.0f

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        val distance = dp[len1][len2]
        val maxLen = max(len1, len2)
        return 1.0f - (distance.toFloat() / maxLen.toFloat())
    }
}
