package com.cypher.assistant.features.applications

import com.cypher.assistant.features.applications.domain.matcher.ApplicationMatcher
import com.cypher.assistant.features.applications.domain.model.AppInfo
import com.cypher.assistant.features.applications.domain.model.AppMatchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApplicationMatcherTest {

    private lateinit var matcher: ApplicationMatcher
    private lateinit var sampleApps: List<AppInfo>

    @Before
    fun setup() {
        matcher = ApplicationMatcher()
        sampleApps = listOf(
            AppInfo(packageName = "com.whatsapp", appName = "WhatsApp"),
            AppInfo(packageName = "com.instagram.android", appName = "Instagram"),
            AppInfo(packageName = "com.android.chrome", appName = "Google Chrome"),
            AppInfo(packageName = "com.google.android.youtube", appName = "YouTube"),
            AppInfo(packageName = "com.spotify.music", appName = "Spotify"),
            AppInfo(packageName = "com.google.android.apps.maps", appName = "Google Maps"),
            AppInfo(packageName = "com.google.android.gm", appName = "Gmail"),
            AppInfo(packageName = "com.android.settings", appName = "Settings"),
            AppInfo(packageName = "com.android.camera2", appName = "Camera")
        )
    }

    @Test
    fun testExactMatch() {
        val result = matcher.match("WhatsApp", sampleApps)
        assertTrue(result is AppMatchResult.Match)
        assertEquals("com.whatsapp", (result as AppMatchResult.Match).appInfo.packageName)
    }

    @Test
    fun testCaseInsensitiveMatch() {
        val result = matcher.match("whatsapp", sampleApps)
        assertTrue(result is AppMatchResult.Match)
        assertEquals("com.whatsapp", (result as AppMatchResult.Match).appInfo.packageName)
    }

    @Test
    fun testSpeechVariationApostrophe() {
        val result = matcher.match("what's app", sampleApps)
        assertTrue(result is AppMatchResult.Match)
        assertEquals("com.whatsapp", (result as AppMatchResult.Match).appInfo.packageName)
    }

    @Test
    fun testAliasInstagramInsta() {
        val result = matcher.match("insta", sampleApps)
        assertTrue(result is AppMatchResult.Match)
        assertEquals("com.instagram.android", (result as AppMatchResult.Match).appInfo.packageName)
    }

    @Test
    fun testAliasGoogleChrome() {
        val result = matcher.match("Chrome", sampleApps)
        assertTrue(result is AppMatchResult.Match)
        assertEquals("com.android.chrome", (result as AppMatchResult.Match).appInfo.packageName)
    }

    @Test
    fun testAliasYouTube() {
        val result = matcher.match("you tube", sampleApps)
        assertTrue(result is AppMatchResult.Match)
        assertEquals("com.google.android.youtube", (result as AppMatchResult.Match).appInfo.packageName)
    }

    @Test
    fun testNotFound() {
        val result = matcher.match("NonExistentApp12345", sampleApps)
        assertTrue(result is AppMatchResult.NotFound)
    }
}
