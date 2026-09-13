package com.aleksandrantonikov.stuffstats

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()
    private fun label(id: Int) = composeRule.activity.getString(id)
    @Test fun homeScreenIsDisplayed() {
        composeRule.onNodeWithText(label(R.string.home_title)).assertIsDisplayed()
    }
    @Test fun createEditArchiveRestoreAndRecreate() {
        val name = "Phase1-${System.currentTimeMillis()}"
        composeRule.onNodeWithText(label(R.string.add_item)).performClick()
        composeRule.onNodeWithTag("name").performTextInput(name)
        composeRule.onNodeWithTag("price").performTextInput("100.00")
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("name").assertTextContains(name)
        composeRule.onNodeWithTag("save").performScrollTo().performClick()
        composeRule.waitUntil(10000) { composeRule.onAllNodesWithText(label(R.string.home_title)).fetchSemanticsNodes().isNotEmpty() && composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(name).performClick()
        composeRule.onNodeWithText("100.00 USD", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(label(R.string.edit_item)).performClick()
        composeRule.onNodeWithTag("name").performTextReplacement("$name edited")
        composeRule.onNodeWithTag("save").performScrollTo().performClick()
        composeRule.waitUntil(10000) { composeRule.onAllNodesWithTag("save").fetchSemanticsNodes().isEmpty() && composeRule.onAllNodesWithText("$name edited").fetchSemanticsNodes().isNotEmpty() }
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("$name edited").assertIsDisplayed()
        composeRule.onNodeWithText(label(R.string.archive_item)).performClick()
        composeRule.onNodeWithText(label(R.string.confirm)).performClick()
        composeRule.waitUntil(10000) { composeRule.onAllNodesWithText(label(R.string.archived_items)).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(label(R.string.archived_items)).performClick()
        composeRule.waitUntil(10000) { composeRule.onAllNodesWithText("$name edited").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("$name edited").performClick()
        composeRule.onNodeWithText(label(R.string.restore_item)).performClick()
        composeRule.onNodeWithText(label(R.string.confirm)).performClick()
        composeRule.waitUntil(10000) { composeRule.onAllNodesWithText(label(R.string.active_items)).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(label(R.string.active_items)).performClick()
        composeRule.waitUntil(10000) { composeRule.onAllNodesWithText("$name edited").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("$name edited").assertIsDisplayed()
    }
}
