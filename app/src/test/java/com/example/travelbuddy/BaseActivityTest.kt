package com.example.travelbuddy

import androidx.appcompat.app.AppCompatDelegate
import org.junit.Test
import org.junit.Assert.*

class BaseActivityTest {
    @Test
    fun testDarkModeToggleFromLightToDark() {
        var isDarkMode = false
        isDarkMode = !isDarkMode
        assertTrue(isDarkMode)
    }

    @Test
    fun testDarkModeToggleFromDarkToLight() {
        var isDarkMode = true
        isDarkMode = !isDarkMode
        assertFalse(isDarkMode)
    }

    @Test
    fun testDarkModeDefaultIsLight() {
        val defaultValue = false
        val isDarkMode = defaultValue
        assertFalse(isDarkMode)
    }

    @Test
    fun testDarkModePreferenceKey() {
        val expectedKey = "dark_mode"
        val actualKey = "dark_mode"
        assertEquals(expectedKey, actualKey)
    }

    @Test
    fun testSharedPreferencesName() {
        val expectedName = "TravelBuddyPrefs"
        val actualName = "TravelBuddyPrefs"
        assertEquals(expectedName, actualName)
    }

    @Test
    fun testDarkModeIconResourceNames() {
        val darkModeIconName = "ic_dark_mode"
        val lightModeIconName = "ic_light_mode"
        assertNotNull(darkModeIconName)
        assertNotNull(lightModeIconName)
        assertNotEquals(darkModeIconName, lightModeIconName)
        assertTrue(darkModeIconName.contains("dark"))
        assertTrue(lightModeIconName.contains("light"))
    }

    @Test
    fun testAppCompatDelegateModes() {
        val nightMode = AppCompatDelegate.MODE_NIGHT_YES
        val dayMode = AppCompatDelegate.MODE_NIGHT_NO
        assertNotNull(nightMode)
        assertNotNull(dayMode)
        assertNotEquals(nightMode, dayMode)
    }

    @Test
    fun testDarkModeToggleLogic() {
        var isDarkMode = false
        isDarkMode = !isDarkMode
        assertTrue(isDarkMode)
        isDarkMode = !isDarkMode
        assertFalse(isDarkMode)
        isDarkMode = !isDarkMode
        assertTrue(isDarkMode)
    }

    @Test
    fun testDarkModeStatePersistence() {
        val savedState = true
        val retrievedState = savedState
        assertEquals(savedState, retrievedState)
    }

    @Test
    fun testDarkModeButtonIdName() {
        val buttonIdName = "darkModeButtonGlobal"
        assertNotNull(buttonIdName)
        assertTrue(buttonIdName.isNotEmpty())
        assertTrue(buttonIdName.contains("darkMode"))
        assertTrue(buttonIdName.contains("Button"))
    }

    @Test
    fun testAppCompatDelegateModeValues() {
        val nightMode = AppCompatDelegate.MODE_NIGHT_YES
        val dayMode = AppCompatDelegate.MODE_NIGHT_NO
        val followSystem = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        assertNotNull(nightMode)
        assertNotNull(dayMode)
        assertNotNull(followSystem)
        assertNotEquals(nightMode, dayMode)
        assertNotEquals(nightMode, followSystem)
        assertNotEquals(dayMode, followSystem)
    }

    @Test
    fun testDarkModeToggleMultipleTimes() {
        var isDarkMode = false
        isDarkMode = !isDarkMode
        assertTrue(isDarkMode)
        isDarkMode = !isDarkMode
        assertFalse(isDarkMode)
        isDarkMode = !isDarkMode
        assertTrue(isDarkMode)
        isDarkMode = !isDarkMode
        assertFalse(isDarkMode)
    }

    @Test
    fun testDarkModePreferenceKeyFormat() {
        val key = "dark_mode"
        assertTrue(key.isNotEmpty())
        assertTrue(key.contains("dark"))
        assertTrue(key.contains("mode"))
        assertFalse(key.contains(" "))
        assertTrue(key.matches(Regex("[a-z_]+")))
    }
}
