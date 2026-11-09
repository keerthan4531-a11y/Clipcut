package com.clipcut.editor.ui.projects

import android.os.Build
import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.clipcut.editor.R
import com.clipcut.editor.engine.CrashRecordSummary
import com.clipcut.editor.engine.ProcessExitRecorder
import com.clipcut.editor.engine.ProcessExitSnapshot
import com.clipcut.editor.ui.ClipCutTestTags
import com.clipcut.editor.ui.theme.ClipCutAccents
import com.clipcut.editor.ui.theme.ClipCutChromeIconButton
import com.clipcut.editor.ui.theme.ClipCutSecondaryButton
import com.clipcut.editor.ui.theme.LocalClipCutColors
import com.clipcut.editor.ui.theme.Radius
import com.clipcut.editor.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class CrashReportKind { CRASH, NOT_RESPONDING, LOW_MEMORY, MEMORY_LIMIT, RESOURCE_LIMIT }

/** The newest crash or abnormal exit the user hasn't seen yet. */
data class CrashReportNotice(
    val kind: CrashReportKind,
    val occurredAtEpochMs: Long,
    /** Saving, copying or dismissing sets aside every event up to this time. */
    val coversThroughEpochMs: Long,
    /** The exception class for a crash ClipCut recorded, otherwise Android's exit reason. */
    val detail: String,
    /** The version that crashed, when the crash record says. */
    val appVersion: String? = null,
)

data class CrashReportDevice(
    val appVersion: String,
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val sdkInt: Int,
    val abi: String,
) {
    companion object {
        fun current(appVersion: String) = CrashReportDevice(
            appVersion = appVersion,
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            androidRelease = Build.VERSION.RELEASE.orEmpty(),
            sdkInt = Build.VERSION.SDK_INT,
            abi = processAbi(),
        )
    }
}

/** The ABI this process runs as: a 32-bit APK on a 64-bit phone runs armeabi-v7a, not the phone's first ABI. */
private fun processAbi(): String {
    val abis = if (android.os.Process.is64Bit()) Build.SUPPORTED_64_BIT_ABIS else Build.SUPPORTED_32_BIT_ABIS
    return (abis.firstOrNull() ?: Build.SUPPORTED_ABIS.firstOrNull()).orEmpty()
}

object CrashReportNoticePolicy {
    /** Older events are history rather than news. The diagnostic ZIP still has them. */
    const val LOOKBACK_MS = 14L * 24 * 60 * 60 * 1000

    /** A crash record is written just before the process dies, and Android logs the exit a moment later. */
    internal const val CRASH_EXIT_MATCH_MS = 10_000L

    fun latest(
        crashes: List<CrashRecordSummary>,
        exits: List<ProcessExitSnapshot>,
        acknowledgedThroughEpochMs: Long,
        nowEpochMs: Long,
    ): CrashReportNotice? {
        val since = nowEpochMs - LOOKBACK_MS
        fun isNew(at: Long) = at > acknowledgedThroughEpochMs && at >= since

        val notices = crashes.filter { isNew(it.recordedAtEpochMs) }.map { crash ->
            CrashReportNotice(
                kind = CrashReportKind.CRASH,
                occurredAtEpochMs = crash.recordedAtEpochMs,
                coversThroughEpochMs = crash.recordedAtEpochMs,
                detail = crash.rootClassName,
                appVersion = crash.appVersion.ifBlank { null },
            )
        }.toMutableList()
        var coversThrough = notices.maxOfOrNull { it.occurredAtEpochMs } ?: Long.MIN_VALUE
        for (exit in exits) {
            if (!isNew(exit.timestampEpochMs)) continue
            val kind = kindOf(exit) ?: continue
            coversThrough = maxOf(coversThrough, exit.timestampEpochMs)
            // The crash record already tells this one, and names the exception.
            val recorded = exit.reasonCode == ProcessExitRecorder.REASON_CRASH && crashes.any {
                exit.timestampEpochMs - it.recordedAtEpochMs in 0..CRASH_EXIT_MATCH_MS
            }
            if (recorded) continue
            notices += CrashReportNotice(
                kind = kind,
                occurredAtEpochMs = exit.timestampEpochMs,
                coversThroughEpochMs = exit.timestampEpochMs,
                detail = if (kind == CrashReportKind.MEMORY_LIMIT) "MEMORY_LIMITER" else ProcessExitRecorder.reasonName(exit.reasonCode),
            )
        }
        return notices.maxByOrNull { it.occurredAtEpochMs }?.copy(coversThroughEpochMs = coversThrough)
    }

    /**
     * Exits worth a word with the user. Android reclaims memory from apps in the
     * background all the time, so a low-memory kill only counts while ClipCut was
     * on screen or running an export.
     */
    internal fun kindOf(exit: ProcessExitSnapshot): CrashReportKind? {
        val wasVisible = exit.importance in 1..ProcessExitRecorder.IMPORTANCE_PERCEPTIBLE
        return when (exit.reasonCode) {
            ProcessExitRecorder.REASON_CRASH,
            ProcessExitRecorder.REASON_CRASH_NATIVE,
            ProcessExitRecorder.REASON_INITIALIZATION_FAILURE -> CrashReportKind.CRASH
            ProcessExitRecorder.REASON_ANR -> CrashReportKind.NOT_RESPONDING
            ProcessExitRecorder.REASON_EXCESSIVE_RESOURCE_USAGE -> CrashReportKind.RESOURCE_LIMIT
            ProcessExitRecorder.REASON_LOW_MEMORY -> CrashReportKind.LOW_MEMORY.takeIf { wasVisible }
            ProcessExitRecorder.REASON_OTHER -> CrashReportKind.MEMORY_LIMIT.takeIf {
                wasVisible && ProcessExitRecorder.isMemoryLimiterKill(exit.description)
            }
            else -> null
        }
    }

    /** A GitHub issue body, in English for the tracker, with only what triage needs. */
    fun issueBody(notice: CrashReportNotice, device: CrashReportDevice): String {
        val at = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(notice.occurredAtEpochMs))
        val what = when (notice.kind) {
            CrashReportKind.CRASH -> "ClipCut closed unexpectedly"
            CrashReportKind.NOT_RESPONDING -> "ClipCut stopped responding and Android closed it"
            CrashReportKind.LOW_MEMORY -> "Android closed ClipCut while the phone was low on memory"
            CrashReportKind.MEMORY_LIMIT -> "Android closed ClipCut for going over its memory limit"
            CrashReportKind.RESOURCE_LIMIT -> "Android closed ClipCut for using too many system resources"
        }
        return buildString {
            appendLine("### What happened")
            appendLine("$what (${notice.detail}) at $at UTC.")
            appendLine()
            appendLine("### What I was doing")
            appendLine("(Describe what you were doing just before it happened.)")
            appendLine()
            appendLine("### Device")
            appendLine("- ClipCut: ${device.appVersion}")
            if (notice.appVersion != null && notice.appVersion != device.appVersion) {
                appendLine("- Version it happened in: ${notice.appVersion}")
            }
            appendLine("- Device: ${device.manufacturer} ${device.model}".trimEnd())
            appendLine("- Android: ${device.androidRelease} (API ${device.sdkInt})")
            appendLine("- ABI: ${device.abi}")
        }
    }

    fun reportFileName(notice: CrashReportNotice): String =
        "ClipCut-report-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date(notice.occurredAtEpochMs)) + ".zip"
}

@Composable
internal fun CrashReportBanner(
    notice: CrashReportNotice,
    saving: Boolean,
    onSaveReport: () -> Unit,
    onCopySummary: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalClipCutColors.current
    val context = LocalContext.current
    val at = DateUtils.formatDateTime(
        context,
        notice.occurredAtEpochMs,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
    )
    val title = stringResource(
        when (notice.kind) {
            CrashReportKind.CRASH -> R.string.crash_notice_crash
            CrashReportKind.NOT_RESPONDING -> R.string.crash_notice_not_responding
            CrashReportKind.LOW_MEMORY -> R.string.crash_notice_low_memory
            CrashReportKind.MEMORY_LIMIT -> R.string.crash_notice_memory_limit
            CrashReportKind.RESOURCE_LIMIT -> R.string.crash_notice_resource_limit
        },
        at
    )
    Surface(
        modifier = modifier
            .testTag(ClipCutTestTags.CRASH_REPORT_BANNER)
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = colors.panelHighest,
        shape = RoundedCornerShape(Radius.lg),
        border = BorderStroke(1.dp, ClipCutAccents.Peach.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = ClipCutAccents.Peach,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = colors.text,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(R.string.crash_notice_body),
                        color = colors.subtext,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                ClipCutChromeIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = stringResource(R.string.crash_notice_dismiss),
                    onClick = onDismiss,
                    modifier = Modifier.testTag(ClipCutTestTags.CRASH_REPORT_DISMISS)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                ClipCutSecondaryButton(
                    text = stringResource(R.string.crash_notice_save_report),
                    onClick = onSaveReport,
                    icon = Icons.Default.Download,
                    enabled = !saving,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(ClipCutTestTags.CRASH_REPORT_SAVE)
                )
                ClipCutSecondaryButton(
                    text = stringResource(R.string.crash_notice_copy_summary),
                    onClick = onCopySummary,
                    icon = Icons.Default.ContentCopy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(ClipCutTestTags.CRASH_REPORT_COPY)
                )
            }
        }
    }
}
