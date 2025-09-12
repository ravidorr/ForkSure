package com.ravidor.forksure.repository

import com.ravidor.forksure.data.model.AppTheme
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PreferencesCacheManagerBehaviorTest {
    @Test
    fun updateThemeAndClearCacheAtomically_callsBoth() = runTest {
        val upr = mockk<UserPreferencesRepository>(relaxed = true)
        val rr = mockk<RecipeRepository>(relaxed = true)
        val mgr = PreferencesCacheManager(upr, rr)

        mgr.updateThemeAndClearCacheAtomically(AppTheme.DARK)

        coVerify { upr.updateTheme(AppTheme.DARK) }
        coVerify { rr.clearCache() }
    }
}
