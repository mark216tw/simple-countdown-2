package com.mark.simplecountdown.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mark.simplecountdown.model.AppThemeColor
import com.mark.simplecountdown.model.TimerSettings

@Composable
fun SettingsDialog(
    settings: TimerSettings,
    onDismiss: () -> Unit,
    onSettingsChange: (TimerSettings) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("設定") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("主題色", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                AppThemeColor.entries.chunked(3).forEach { themes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        themes.forEach { theme ->
                            ThemeColorButton(
                                theme = theme,
                                selected = settings.themeColor == theme,
                                onClick = {
                                    onSettingsChange(settings.copy(themeColor = theme))
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("倒數完成鈴響時間", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(2.dp))
                TimerSettings.alarmDurationOptions.chunked(2).forEach { rowOptions ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowOptions.forEach { seconds ->
                            val selected = settings.alarmDurationSeconds == seconds
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onSettingsChange(settings.copy(alarmDurationSeconds = seconds))
                                },
                                label = {
                                    Text(
                                        alarmDurationLabel(seconds),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = "已選擇",
                                        )
                                    }
                                } else {
                                    null
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                SettingSwitch("深色模式", settings.darkMode) {
                    onSettingsChange(settings.copy(darkMode = it))
                }
                SettingSwitch("倒數畫面保持常亮", settings.keepScreenOn) {
                    onSettingsChange(settings.copy(keepScreenOn = it))
                }
                SettingSwitch("倒數進行時播放答答聲", settings.tickSoundEnabled) {
                    onSettingsChange(settings.copy(tickSoundEnabled = it))
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
private fun ThemeColorButton(
    theme: AppThemeColor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = Color(theme.seedColor)
    val label = themeLabel(theme)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = label
                    stateDescription = if (selected) "已選擇" else "未選擇"
                }
                .clickable(role = Role.RadioButton, onClick = onClick),
            shape = CircleShape,
            color = color,
            border = if (selected) {
                BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
            } else {
                null
            },
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = if (color.luminance() < 0.5f) Color.White else Color.Black,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun alarmDurationLabel(seconds: Long): String = when (seconds) {
    0L -> "無聲"
    10L -> "10 秒"
    30L -> "30 秒"
    60L -> "1 分鐘"
    300L -> "5 分鐘"
    -1L -> "不自動停止"
    else -> "${seconds} 秒"
}

private fun themeLabel(theme: AppThemeColor): String = when (theme) {
    AppThemeColor.CORAL -> "珊瑚紅"
    AppThemeColor.TANGERINE -> "活力橙"
    AppThemeColor.MINT -> "薄荷綠"
    AppThemeColor.OCEAN -> "晴空藍"
    AppThemeColor.VIOLET -> "亮紫色"
    AppThemeColor.BERRY -> "莓果紅"
}
