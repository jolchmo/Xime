package com.kingzcheung.kime.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CandidateBarTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `CandidateBar should display candidates`() {
        val candidates = listOf("你好", "世界", "测试")
        
        composeTestRule.setContent {
            CandidateBar(
                candidates = candidates,
                selectedIndex = 0,
                onCandidateClick = {},
                onCandidateLongClick = {}
            )
        }
        
        composeTestRule.onNodeWithText("你好").assertIsDisplayed()
        composeTestRule.onNodeWithText("世界").assertIsDisplayed()
        composeTestRule.onNodeWithText("测试").assertIsDisplayed()
    }
    
    @Test
    fun `CandidateBar should handle empty candidates`() {
        composeTestRule.setContent {
            CandidateBar(
                candidates = emptyList(),
                selectedIndex = -1,
                onCandidateClick = {},
                onCandidateLongClick = {}
            )
        }
    }
}