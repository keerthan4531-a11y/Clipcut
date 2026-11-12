package com.clipcut.editor.ui.editor

import com.clipcut.editor.ui.theme.ClipCutAccents
import com.clipcut.editor.ui.theme.LocalClipCutColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.clipcut.editor.engine.LutPresets
import com.clipcut.editor.model.EffectCategory
import com.clipcut.editor.model.EffectType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clipcut.editor.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EffectLibraryPanel(
    hasClipSelected: Boolean,
    hasCopiedEffects: Boolean,
    onExportEffects: () -> Unit,
    onEffectSelected: (EffectType) -> Unit = {},
    onLutPresetSelected: (String) -> Unit = {},
    onImportEffects: () -> Unit,
    onCopyEffects: () -> Unit,
    onPasteEffects: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semanticColors = LocalClipCutColors.current
    PremiumEditorPanel(
        title = stringResource(R.string.effect_library_title),
        subtitle = stringResource(R.string.panel_effect_library_subtitle),
        icon = Icons.Default.ContentCopy,
        accent = ClipCutAccents.Mauve,
        onClose = onClose,
        modifier = modifier.heightIn(max = 560.dp),
        scrollable = true,
        closeContentDescription = stringResource(R.string.effect_library_close_cd)
    ) {
        FilterBrowserCard(
            onEffectSelected = onEffectSelected,
            onLutPresetSelected = onLutPresetSelected
        )
        Spacer(modifier = Modifier.height(12.dp))
        PremiumPanelCard(accent = ClipCutAccents.Mauve) {
            Text(
                text = stringResource(R.string.panel_effect_library_workflow_title),
                color = semanticColors.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.panel_effect_library_description),
                color = semanticColors.subtext,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PremiumPanelPill(
                    text = if (hasClipSelected) {
                        stringResource(R.string.panel_effect_library_status_clip_ready)
                    } else {
                        stringResource(R.string.panel_effect_library_status_clip_needed)
                    },
                    accent = if (hasClipSelected) ClipCutAccents.Green else ClipCutAccents.Red
                )
                PremiumPanelPill(
                    text = if (hasCopiedEffects) {
                        stringResource(R.string.panel_effect_library_status_buffer_ready)
                    } else {
                        stringResource(R.string.panel_effect_library_status_buffer_empty)
                    },
                    accent = if (hasCopiedEffects) ClipCutAccents.Sapphire else semanticColors.subtext
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val singleColumn = maxWidth < 520.dp
            val cardWidth = if (singleColumn) maxWidth else (maxWidth - 10.dp) / 2

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EffectLibraryActionCard(
                    title = stringResource(R.string.panel_effect_library_copy),
                    subtitle = if (hasClipSelected) {
                        stringResource(R.string.panel_effect_library_copy_description)
                    } else {
                        stringResource(R.string.panel_effect_library_copy_disabled)
                    },
                    icon = Icons.Default.ContentCopy,
                    accent = ClipCutAccents.Mauve,
                    enabled = hasClipSelected,
                    buttonLabel = stringResource(R.string.panel_effect_library_copy),
                    buttonStyle = ActionButtonStyle.Outlined,
                    onClick = onCopyEffects,
                    modifier = Modifier.width(cardWidth)
                )
                EffectLibraryActionCard(
                    title = stringResource(R.string.panel_effect_library_paste),
                    subtitle = when {
                        !hasClipSelected -> stringResource(R.string.panel_effect_library_paste_needs_clip)
                        !hasCopiedEffects -> stringResource(R.string.panel_effect_library_paste_needs_buffer)
                        else -> stringResource(R.string.panel_effect_library_paste_description)
                    },
                    icon = Icons.Default.ContentPaste,
                    accent = ClipCutAccents.Green,
                    enabled = hasClipSelected && hasCopiedEffects,
                    buttonLabel = stringResource(R.string.panel_effect_library_paste),
                    buttonStyle = ActionButtonStyle.Outlined,
                    onClick = onPasteEffects,
                    modifier = Modifier.width(cardWidth)
                )
                EffectLibraryActionCard(
                    title = stringResource(R.string.panel_effect_library_export),
                    subtitle = if (hasClipSelected) {
                        stringResource(R.string.panel_effect_library_export_description)
                    } else {
                        stringResource(R.string.panel_effect_library_export_disabled)
                    },
                    icon = Icons.Default.Upload,
                    accent = ClipCutAccents.Peach,
                    enabled = hasClipSelected,
                    buttonLabel = stringResource(R.string.panel_effect_library_export),
                    buttonStyle = ActionButtonStyle.Filled,
                    onClick = onExportEffects,
                    modifier = Modifier.width(cardWidth)
                )
                EffectLibraryActionCard(
                    title = stringResource(R.string.panel_effect_library_import),
                    subtitle = stringResource(R.string.panel_effect_library_import_description),
                    icon = Icons.Default.Download,
                    accent = ClipCutAccents.Blue,
                    enabled = true,
                    buttonLabel = stringResource(R.string.panel_effect_library_import),
                    buttonStyle = ActionButtonStyle.Filled,
                    onClick = onImportEffects,
                    modifier = Modifier.width(cardWidth)
                )
            }
        }

        if (!hasClipSelected) {
            Spacer(modifier = Modifier.height(12.dp))
            PremiumPanelCard(accent = ClipCutAccents.Red) {
                Text(
                    text = stringResource(R.string.panel_effect_library_clip_required_title),
                    color = semanticColors.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.panel_effect_library_select_clip_hint),
                    color = semanticColors.subtext,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private enum class ActionButtonStyle {
    Filled,
    Outlined
}

@Composable
private fun EffectLibraryActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    buttonLabel: String,
    buttonStyle: ActionButtonStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semanticColors = LocalClipCutColors.current
    Surface(
        modifier = modifier,
        color = semanticColors.panelHighest,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (enabled) accent.copy(alpha = 0.24f) else semanticColors.cardStroke
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 208.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = accent.copy(alpha = if (enabled) 0.16f else 0.08f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    accent.copy(alpha = if (enabled) 0.24f else 0.14f)
                )
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) accent else semanticColors.subtext,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    color = if (enabled) semanticColors.text else semanticColors.subtext,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = semanticColors.subtext,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (buttonStyle) {
                ActionButtonStyle.Filled -> {
                    Button(
                        onClick = onClick,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = semanticColors.surfaceBase,
                            disabledContainerColor = semanticColors.surfaceLow,
                            disabledContentColor = semanticColors.overlay
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = icon,
                            contentDescription = buttonLabel,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(buttonLabel)
                    }
                }

                ActionButtonStyle.Outlined -> {
                    OutlinedButton(
                        onClick = onClick,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = accent,
                            disabledContentColor = semanticColors.overlay
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (enabled) accent.copy(alpha = 0.28f) else semanticColors.cardStroke
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = icon,
                            contentDescription = buttonLabel,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(buttonLabel)
                    }
                }
            }
        }
    }
}

/**
 * The actual filter browser: every effect ClipCut can render, grouped by category, plus the
 * built-in colour looks. Selecting a card applies it to the selected clip immediately.
 *
 * Cards are plain text buttons rather than live thumbnails: a 17³ LUT or a 12-branch shader
 * grid rendered per card on a mid-range GPU costs more than it is worth while the sheet is
 * open, and the previous panel had no way to reach these effects at all.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBrowserCard(
    onEffectSelected: (EffectType) -> Unit,
    onLutPresetSelected: (String) -> Unit
) {
    val semanticColors = LocalClipCutColors.current
    var query by remember { mutableStateOf("") }
    var showLooks by remember { mutableStateOf(false) }
    val needle = query.trim().lowercase()

    PremiumPanelCard(accent = ClipCutAccents.Mauve) {
        Text(
            text = stringResource(R.string.effect_browser_title),
            color = semanticColors.text,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.effect_browser_subtitle),
            color = semanticColors.subtext,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BrowserToggle(
                label = stringResource(R.string.effect_browser_effects),
                selected = !showLooks,
                onClick = { showLooks = false }
            )
            BrowserToggle(
                label = stringResource(R.string.effect_browser_looks),
                selected = showLooks,
                onClick = { showLooks = true }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.effect_browser_search)) }
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (showLooks) {
            LutPresets.categories.forEach { category ->
                val looks = LutPresets.byCategory(category).filter {
                    needle.isEmpty() ||
                        it.displayName.lowercase().contains(needle) ||
                        it.description.lowercase().contains(needle)
                }
                if (looks.isNotEmpty()) {
                    Text(
                        text = category,
                        color = semanticColors.subtext,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        looks.forEach { preset ->
                            BrowserChip(
                                label = preset.displayName,
                                onClick = { onLutPresetSelected(preset.id) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        } else {
            EffectCategory.entries.forEach { category ->
                val effects = EffectType.entries.filter {
                    it.category == category &&
                        (needle.isEmpty() || it.displayName.lowercase().contains(needle))
                }
                if (effects.isNotEmpty()) {
                    Text(
                        text = category.displayName,
                        color = semanticColors.subtext,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        effects.forEach { effect ->
                            BrowserChip(
                                label = effect.displayName,
                                onClick = { onEffectSelected(effect) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun BrowserToggle(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val semanticColors = LocalClipCutColors.current
    Surface(
        onClick = onClick,
        color = if (selected) ClipCutAccents.Mauve.copy(alpha = 0.22f) else semanticColors.panelHighest,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, semanticColors.cardStroke)
    ) {
        Text(
            text = label,
            color = if (selected) ClipCutAccents.Mauve else semanticColors.subtext,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun BrowserChip(
    label: String,
    onClick: () -> Unit
) {
    val semanticColors = LocalClipCutColors.current
    Surface(
        onClick = onClick,
        color = semanticColors.panelHighest,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, semanticColors.cardStroke),
        modifier = Modifier.width(124.dp)
    ) {
        Text(
            text = label,
            color = semanticColors.text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}
