package com.ravidor.forksure

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecipeSharingHelperTest {

    @Test
    fun formatRecipeForSharing_contains_name_dividers_and_footer() {
        val content = "Title: Brownies\nIngredients: sugar, flour"
        val out = RecipeSharingHelper.formatRecipeForSharing(content, "Test Recipe")
        assertThat(out).contains("🧁 Test Recipe")
        assertThat(out).contains("━━━━━━━━━━━━━━━━━━━━")
        assertThat(out).contains("Shared from ForkSure")
        assertThat(out).contains(content)
    }

    @Test
    fun isAppAvailable_returns_true_for_ANY_and_false_for_known_missing() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        assertThat(RecipeSharingHelper.isAppAvailable(ctx, ShareTarget.ANY)).isTrue()
        // On Robolectric default environment, these packages are not present
        assertThat(RecipeSharingHelper.isAppAvailable(ctx, ShareTarget.NOTION)).isFalse()
        assertThat(RecipeSharingHelper.isAppAvailable(ctx, ShareTarget.GOOGLE_KEEP)).isFalse()
    }
}
