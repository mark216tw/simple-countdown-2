package com.mark.simplecountdown.model

data class TimerSettings(
    val alarmDurationSeconds: Long = 60,
    val tickSoundEnabled: Boolean = false,
    val keepScreenOn: Boolean = false,
    val darkMode: Boolean = false,
    val themeColor: AppThemeColor = AppThemeColor.CORAL,
) {
    companion object {
        val alarmDurationOptions = listOf(0L, 10L, 30L, 60L, 300L, -1L)
    }
}

enum class AppThemeColor(val seedColor: Long) {
    CORAL(0xFFE85D4A),
    TANGERINE(0xFFF59E0B),
    MINT(0xFF13A889),
    OCEAN(0xFF1687D9),
    VIOLET(0xFF8255D9),
    BERRY(0xFFD94B82),
}
