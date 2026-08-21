package com.mark.simplecountdown.model

data class TimerSettings(
    val alarmDurationSeconds: Long = 60,
    val soundEnabled: Boolean = true,
    val tickSoundEnabled: Boolean = false,
    val keepScreenOn: Boolean = false,
    val darkMode: Boolean = false,
) {
    companion object {
        val alarmDurationOptions = listOf(60L, 300L, 600L, 900L, 1200L, 1500L, -1L)
    }
}
