package com.aleksandrantonikov.stuffstats

import androidx.compose.runtime.CompositionLocalProvider
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aleksandrantonikov.stuffstats.ui.item.ItemList
import com.aleksandrantonikov.stuffstats.ui.item.ItemsState
import com.aleksandrantonikov.stuffstats.ui.item.PhotoEditor
import com.aleksandrantonikov.stuffstats.ui.theme.StuffStatsTheme
import com.aleksandrantonikov.stuffstats.domain.Category
import com.aleksandrantonikov.stuffstats.domain.Item
import com.aleksandrantonikov.stuffstats.domain.MetricType
import com.aleksandrantonikov.stuffstats.domain.UsageMetric
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LargeFontLayoutTest {
    @get:Rule val composeRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun catalogControlsRemainReachableAtTwoHundredPercentFontScale() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                StuffStatsTheme(dynamicColor = false) {
                    ItemList(
                        state = ItemsState(loading = false),
                        add = {},
                        open = {},
                        dashboard = {},
                        photoFile = { null },
                    )
                }
            }
        }

        val sortLabel = context.getString(R.string.sort_by)
        composeRule.onNodeWithTag("item_list").performScrollToNode(hasText(sortLabel, substring = true))
        composeRule.onNodeWithText(sortLabel, substring = true).assertIsDisplayed()
        val emptyLabel = context.getString(R.string.home_empty_title)
        composeRule.onNodeWithTag("item_list").performScrollToNode(hasText(emptyLabel))
        composeRule.onNodeWithText(emptyLabel).assertIsDisplayed()
    }

    @Test
    fun systemBackFromPhotoEditorUsesCaptureCleanupPath() {
        val discarded = AtomicBoolean(false)
        val returned = AtomicBoolean(false)
        lateinit var backDispatcher: OnBackPressedDispatcher
        composeRule.setContent {
            backDispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
            StuffStatsTheme(dynamicColor = false) {
                PhotoEditor(
                    item = Item(
                        id = 1,
                        name = "Item",
                        category = Category.OTHER,
                        priceMinor = null,
                        currency = "USD",
                        purchaseDate = null,
                        metric = UsageMetric(MetricType.USE_COUNT, "uses"),
                        notes = "",
                    ),
                    busy = false,
                    error = false,
                    back = { returned.set(true) },
                    newCapture = { null },
                    discardCapture = { discarded.set(true) },
                    save = { _, _, _, _ -> },
                )
            }
        }

        composeRule.runOnUiThread { backDispatcher.onBackPressed() }
        composeRule.waitUntil { returned.get() }

        assertTrue(discarded.get())
    }
}
