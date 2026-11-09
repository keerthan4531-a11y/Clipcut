package com.clipcut.editor.ui.theme

import com.clipcut.editor.engine.AppearanceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipCutAppearancePolicyTest {

    @Test
    fun theAppearanceMenuOffersOnlySchemesThatExist() {
        // "System" used to sit in this list and resolve to DARK in both branches --
        // a control that read the platform preference and ignored it.
        assertEquals(
            listOf(AppearanceMode.DARK, AppearanceMode.HIGH_CONTRAST_DARK),
            AppearanceMode.entries.toList(),
        )
        AppearanceMode.entries.forEach { mode ->
            assertEquals(mode, ClipCutThemeDefaults.resolveMode(mode))
        }
    }

    @Test
    fun highContrastSemanticTextMeetsWcagAa() {
        val colors = ClipCutThemeDefaults.colorsFor(AppearanceMode.HIGH_CONTRAST_DARK)

        listOf(
            colors.background,
            colors.backgroundMid,
            colors.panel,
            colors.panelRaised,
            colors.panelHighest,
            colors.surfaceBase,
            colors.surfaceLow,
            colors.surface,
            colors.surfaceHigh,
        ).forEach { surface ->
            assertTrue(ClipCutThemeDefaults.contrastRatio(colors.text, surface) >= 4.5)
            assertTrue(ClipCutThemeDefaults.contrastRatio(colors.subtext, surface) >= 4.5)
        }
        assertTrue(ClipCutThemeDefaults.contrastRatio(colors.disabledText, colors.disabledSurface) >= 4.5)
    }

    @Test
    fun highContrastSemanticStrokesMeetNonTextFloor() {
        val colors = ClipCutThemeDefaults.colorsFor(AppearanceMode.HIGH_CONTRAST_DARK)

        assertTrue(ClipCutThemeDefaults.contrastRatio(colors.cardStroke, colors.panel) >= 3.0)
        assertTrue(ClipCutThemeDefaults.contrastRatio(colors.cardStrokeStrong, colors.panel) >= 3.0)
        assertTrue(ClipCutThemeDefaults.contrastRatio(colors.cardStrokeStrong, colors.panelHighest) >= 3.0)
    }

    @Test
    fun lowEmphasisMochaTokensStayBelowSemanticIndicatorFloor() {
        val overlayOnPanel = ClipCutThemeDefaults.contrastRatio(Mocha.Overlay0, Mocha.PanelHighest)
        val strokeOnPanel = ClipCutThemeDefaults.contrastRatio(Mocha.CardStrokeStrong, Mocha.Panel)
        val highContrast = ClipCutThemeDefaults.colorsFor(AppearanceMode.HIGH_CONTRAST_DARK)

        assertTrue(overlayOnPanel < 3.0)
        assertTrue(strokeOnPanel < 3.0)
        assertTrue(ClipCutThemeDefaults.contrastRatio(highContrast.cardStrokeStrong, highContrast.panel) >= 3.0)
    }

    @Test
    fun selectedHighContrastChipHasReadableLabelForCommonAccents() {
        listOf(
            ClipCutAccents.Lavender,
            ClipCutAccents.Blue,
            ClipCutAccents.Sapphire,
            ClipCutAccents.Sky,
            ClipCutAccents.Teal,
            ClipCutAccents.Green,
            ClipCutAccents.Yellow,
            ClipCutAccents.Peach,
            ClipCutAccents.Maroon,
            ClipCutAccents.Red,
            ClipCutAccents.Mauve,
            ClipCutAccents.Pink,
            ClipCutAccents.Flamingo,
            ClipCutAccents.Rosewater,
        ).forEach { accent ->
            assertTrue(
                "Chip label contrast failed for $accent",
                ClipCutThemeDefaults.contrastRatio(
                    ClipCutThemeDefaults.colorsFor(AppearanceMode.HIGH_CONTRAST_DARK).onAccent,
                    accent,
                ) >= 4.5,
            )
        }
    }

    @Test
    fun highContrastIndicatorsMeetNonTextFloor() {
        val colors = ClipCutThemeDefaults.colorsFor(AppearanceMode.HIGH_CONTRAST_DARK)

        listOf(colors.overlay, colors.overlayStrong, colors.focusRing).forEach { indicator ->
            assertTrue(ClipCutThemeDefaults.contrastRatio(indicator, colors.panel) >= 3.0)
            assertTrue(ClipCutThemeDefaults.contrastRatio(indicator, colors.panelHighest) >= 3.0)
        }
    }

    @Test
    fun darkModeKeepsPrimaryTextReadable() {
        val colors = ClipCutThemeDefaults.colorsFor(AppearanceMode.DARK)

        assertTrue(ClipCutThemeDefaults.contrastRatio(colors.text, colors.panel) >= 4.5)
        assertTrue(ClipCutThemeDefaults.contrastRatio(colors.subtext, colors.panel) >= 4.5)
    }

    @Test
    fun radiusTokensStayWithinProfessionalGeometryCap() {
        listOf(Radius.xs, Radius.sm, Radius.md, Radius.lg, Radius.xl, Radius.xxl).forEach { radius ->
            assertTrue("Radius token exceeded 12dp: $radius", radius.value <= 12f)
        }
    }
}
