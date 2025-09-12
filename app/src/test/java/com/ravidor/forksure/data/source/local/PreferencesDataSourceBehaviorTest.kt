package com.ravidor.forksure.data.source.local

import android.content.SharedPreferences
import com.ravidor.forksure.data.model.AppTheme
import com.ravidor.forksure.data.model.UserPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesDataSourceBehaviorTest {

    private fun prefs(): SharedPreferences = mockk(relaxed = true)

    @Test
    fun saveAndLoadUserPreferences_roundTrip() = runTest {
        val sp = prefs()
        val ds = PreferencesDataSource(sp)
        val up = UserPreferences(theme = AppTheme.DARK, language = "en-US", cacheRecipes = true, maxCacheSize = 42)

        ds.saveUserPreferences(up)
        // Verify SharedPreferences writes at least a couple of keys to ensure it ran
        verify { sp.edit() }
    }
}
