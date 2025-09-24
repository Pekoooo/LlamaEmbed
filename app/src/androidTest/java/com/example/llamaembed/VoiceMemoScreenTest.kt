package com.example.llamaembed

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.llamaembed.ui.screens.VoiceMemoScreen
import com.example.llamaembed.ui.theme.EmbeddedGemmaTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for VoiceMemoScreen
 * Tests the UI behavior and user interactions
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class VoiceMemoScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun voiceMemoScreen_displaysCorrectly() {
        composeTestRule.setContent {
            EmbeddedGemmaTheme {
                VoiceMemoScreen()
            }
        }

        // Check if the main title is displayed
        composeTestRule
            .onNodeWithText("Voice Memos")
            .assertIsDisplayed()

        // Check if search bar is present
        composeTestRule
            .onNodeWithText("Search memos with AI...")
            .assertIsDisplayed()
    }

    @Test
    fun searchBar_acceptsInput() {
        composeTestRule.setContent {
            EmbeddedGemmaTheme {
                VoiceMemoScreen()
            }
        }

        val searchQuery = "test search"

        // Type in search bar
        composeTestRule
            .onNodeWithText("Search memos with AI...")
            .performTextInput(searchQuery)

        // Verify the text was entered
        composeTestRule
            .onNodeWithText(searchQuery)
            .assertIsDisplayed()
    }
}