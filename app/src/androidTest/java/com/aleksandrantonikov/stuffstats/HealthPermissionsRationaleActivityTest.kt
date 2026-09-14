package com.aleksandrantonikov.stuffstats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthPermissionsRationaleActivityTest {
    @get:Rule val composeRule = createAndroidComposeRule<HealthPermissionsRationaleActivity>()

    @Test fun explainsLocalDistanceAccess() {
        val activity = composeRule.activity

        composeRule.onNodeWithText(activity.getString(R.string.health_permissions_title)).assertIsDisplayed()
        composeRule.onNodeWithText(activity.getString(R.string.health_permissions_body)).assertIsDisplayed()
        composeRule.onNodeWithText(activity.getString(R.string.health_permissions_storage)).assertIsDisplayed()
    }
}
