package com.clipcut.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.clipcut.editor.ui.ClipCutTestTags
import com.clipcut.editor.engine.AppearanceMode
import com.clipcut.editor.engine.DesktopOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ClipCutSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>().apply {
        enableAccessibilityChecks()
    }

    @Before
    fun waitForInitialComposeHierarchy() {
        compose.waitForComposeHierarchy()
    }

    @Test
    fun projectEditorExportAndSettingsSurfacesOpen() {
        compose.onNodeWithTag(ClipCutTestTags.PROJECTS_SCREEN).assertIsDisplayed()
        compose.assertAccessibilityChecksPass()

        compose.onNodeWithTag(ClipCutTestTags.PROJECTS_CREATE_PROJECT).performClick()
        compose.onNodeWithTag(ClipCutTestTags.TEMPLATE_SHEET).assertIsDisplayed()
        compose.onNodeWithTag(ClipCutTestTags.TEMPLATE_BLANK)
            .performScrollTo()
            .performClick()

        compose.waitUntilAtLeastOneExists(ClipCutTestTags.EDITOR_SCREEN)
        compose.waitForIdle()
        assertTrue(
            compose.onAllNodesWithTag(ClipCutTestTags.TUTORIAL_SCREEN)
                .fetchSemanticsNodes()
                .isEmpty()
        )
        dismissTutorialIfPresent()
        compose.assertAccessibilityChecksPass()

        compose.onNodeWithTag(ClipCutTestTags.EDITOR_EMPTY_ADD_MEDIA).assertIsDisplayed().performClick()
        compose.onNodeWithTag(ClipCutTestTags.MEDIA_PICKER_SHEET).assertIsDisplayed()
        compose.assertAccessibilityChecksPass()
        compose.onNodeWithTag(ClipCutTestTags.MEDIA_PICKER_CLOSE).performClick()

        compose.onNodeWithTag(ClipCutTestTags.EDITOR_EXPORT).assertIsDisplayed().performClick()
        compose.onNodeWithTag(ClipCutTestTags.EXPORT_SHEET).assertIsDisplayed()
        compose.assertAccessibilityChecksPass()
        compose.onNodeWithTag(ClipCutTestTags.EXPORT_CLOSE).performClick()

        compose.onNodeWithTag(ClipCutTestTags.EDITOR_BACK).performClick()
        compose.waitUntilAtLeastOneExists(ClipCutTestTags.PROJECTS_SCREEN)
        compose.waitUntilNoNodesExist(ClipCutTestTags.EDITOR_SCREEN)

        compose.onNodeWithTag(ClipCutTestTags.PROJECTS_SETTINGS).performClick()
        compose.onNodeWithTag(ClipCutTestTags.SETTINGS_SCREEN).assertIsDisplayed()
        compose.assertAccessibilityChecksPass()
        compose.onNodeWithTag(ClipCutTestTags.SETTINGS_PRIVACY_OPEN)
            .performScrollTo()
            .performClick()
        compose.onNodeWithTag(ClipCutTestTags.SETTINGS_PRIVACY_DASHBOARD).assertIsDisplayed()
        compose.assertAccessibilityChecksPass()
        compose.onNodeWithTag(ClipCutTestTags.SETTINGS_PRIVACY_CLOSE).performClick()
        compose.onNodeWithTag(ClipCutTestTags.SETTINGS_LICENSES_OPEN)
            .performScrollTo()
            .performClick()
        compose.onNodeWithTag(ClipCutTestTags.SETTINGS_LICENSES_DIALOG).assertIsDisplayed()
        compose.assertAccessibilityChecksPass()
        compose.onNodeWithTag(ClipCutTestTags.SETTINGS_LICENSES_CLOSE).performClick()

        compose.onNodeWithTag(ClipCutTestTags.SETTINGS_REPLAY_TUTORIAL)
            .performScrollTo()
            .performClick()
        compose.waitUntilAtLeastOneExists(ClipCutTestTags.EDITOR_SCREEN)
        compose.onNodeWithTag(ClipCutTestTags.TUTORIAL_SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(ClipCutTestTags.TUTORIAL_SKIP).performClick()
        compose.onNodeWithTag(ClipCutTestTags.EDITOR_SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(ClipCutTestTags.EDITOR_BACK).performClick()
        compose.waitUntilAtLeastOneExists(ClipCutTestTags.SETTINGS_SCREEN)
        compose.waitUntilNoNodesExist(ClipCutTestTags.EDITOR_SCREEN)

        compose.onNodeWithTag(ClipCutTestTags.SETTINGS_BACK)
            .performScrollTo()
            .performClick()

        compose.waitUntilAtLeastOneExists(ClipCutTestTags.PROJECTS_SCREEN)
        compose.waitUntilNoNodesExist(ClipCutTestTags.SETTINGS_SCREEN)
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun pseudoLocalesRenderExpandedAndRtlExportSurfaces() {
        try {
            setApplicationLocale("en-XA")
            compose.waitUntilAtLeastOneExists(ClipCutTestTags.PROJECTS_SCREEN)
            compose.onNodeWithTag(ClipCutTestTags.PROJECTS_SCREEN).assertIsDisplayed()
            compose.onNodeWithTag(ClipCutTestTags.PROJECTS_CREATE_PROJECT).performClick()
            compose.onNodeWithTag(ClipCutTestTags.TEMPLATE_BLANK)
                .performScrollTo()
                .performClick()
            compose.waitUntilAtLeastOneExists(ClipCutTestTags.EDITOR_SCREEN)
            dismissTutorialIfPresent()
            compose.onNodeWithTag(ClipCutTestTags.EDITOR_EXPORT).performClick()
            compose.onNodeWithTag(ClipCutTestTags.EXPORT_SHEET).assertIsDisplayed()
            compose.assertAccessibilityChecksPass()
            assertTrue(compose.onRoot().captureToImage().width > 0)

            setApplicationLocale("ar-XB")
            compose.onNodeWithTag(ClipCutTestTags.EXPORT_SHEET).assertIsDisplayed()
            compose.assertAccessibilityChecksPass()
            assertEquals(
                android.view.View.LAYOUT_DIRECTION_RTL,
                compose.activity.resources.configuration.layoutDirection,
            )
            assertTrue(compose.onRoot().captureToImage().width > 0)
        } finally {
            compose.activity
                .getSystemService(android.app.LocaleManager::class.java)
                .applicationLocales = android.os.LocaleList.getEmptyLocaleList()
        }
    }

    @Test
    fun highContrastPhoneAndDesktopEditorSurfacesRender() {
        try {
            updateAppearance(AppearanceMode.HIGH_CONTRAST_DARK, DesktopOverride.FORCE_OFF)
            compose.waitUntilAtLeastOneExists(ClipCutTestTags.PROJECTS_SCREEN)
            compose.onNodeWithTag(ClipCutTestTags.PROJECTS_CREATE_PROJECT).performClick()
            compose.onNodeWithTag(ClipCutTestTags.TEMPLATE_BLANK)
                .performScrollTo()
                .performClick()
            compose.waitUntilAtLeastOneExists(ClipCutTestTags.EDITOR_SCREEN)
            dismissTutorialIfPresent()
            compose.assertAccessibilityChecksPass()
            assertTrue(compose.onRoot().captureToImage().width > 0)

            compose.onNodeWithTag(ClipCutTestTags.EDITOR_EMPTY_ADD_MEDIA).performClick()
            compose.onNodeWithTag(ClipCutTestTags.MEDIA_PICKER_SHEET).assertIsDisplayed()
            compose.assertAccessibilityChecksPass()
            assertTrue(compose.onRoot().captureToImage().width > 0)
            compose.onNodeWithTag(ClipCutTestTags.MEDIA_PICKER_CLOSE).performClick()

            compose.onNodeWithTag(ClipCutTestTags.EDITOR_EXPORT).performClick()
            compose.onNodeWithTag(ClipCutTestTags.EXPORT_SHEET).assertIsDisplayed()
            compose.assertAccessibilityChecksPass()
            assertTrue(compose.onRoot().captureToImage().width > 0)
            compose.onNodeWithTag(ClipCutTestTags.EXPORT_CLOSE).performClick()

            updateAppearance(AppearanceMode.HIGH_CONTRAST_DARK, DesktopOverride.FORCE_ON)
            compose.waitUntilAtLeastOneExists(ClipCutTestTags.EDITOR_DESKTOP_SIDEBAR)
            compose.onNodeWithTag(ClipCutTestTags.EDITOR_DESKTOP_SIDEBAR).assertIsDisplayed()
            compose.assertAccessibilityChecksPass()
            assertTrue(compose.onRoot().captureToImage().width > 0)
            compose.onNodeWithTag(ClipCutTestTags.EDITOR_EXPORT).performClick()
            compose.onNodeWithTag(ClipCutTestTags.EXPORT_SHEET).assertIsDisplayed()
            compose.assertAccessibilityChecksPass()
            assertTrue(compose.onRoot().captureToImage().width > 0)
        } finally {
            updateAppearance(AppearanceMode.DARK, DesktopOverride.AUTO)
        }
    }

    private fun updateAppearance(appearanceMode: AppearanceMode, desktopOverride: DesktopOverride) {
        runBlocking {
            compose.activity.settingsRepository.updateAppearanceMode(appearanceMode)
            compose.activity.settingsRepository.updateDesktopOverride(desktopOverride)
            compose.activity.settingsRepository.settings.first { settings ->
                settings.appearanceMode == appearanceMode &&
                    settings.desktopModeOverride == desktopOverride
            }
        }
        compose.waitForIdle()
        compose.waitForComposeHierarchy()
    }

    private fun setApplicationLocale(languageTag: String) {
        compose.activity
            .getSystemService(android.app.LocaleManager::class.java)
            .applicationLocales = android.os.LocaleList.forLanguageTags(languageTag)
        compose.waitUntil(timeoutMillis = 20_000L) {
            compose.activity.resources.configuration.locales[0].toLanguageTag() == languageTag
        }
        compose.waitForComposeHierarchy()
    }

    private fun dismissTutorialIfPresent() {
        runCatching {
            compose.waitUntil(timeoutMillis = 1_200L) {
                compose.onAllNodesWithTag(ClipCutTestTags.TUTORIAL_SKIP)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
        }
        val tutorialNodes = runCatching {
            compose.onAllNodesWithTag(ClipCutTestTags.TUTORIAL_SKIP).fetchSemanticsNodes()
        }.getOrDefault(emptyList())
        if (tutorialNodes.isNotEmpty()) {
            compose.onNodeWithTag(ClipCutTestTags.TUTORIAL_SKIP).performClick()
        }
    }

}
