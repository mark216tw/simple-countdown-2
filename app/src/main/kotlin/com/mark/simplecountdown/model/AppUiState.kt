package com.mark.simplecountdown.model

data class AppUiState(
    val presets: List<TimerPreset> = emptyList(),
    val lastCustomTimer: TimerPreset? = null,
    val settings: TimerSettings = TimerSettings(),
    val timer: TimerSnapshot = TimerSnapshot(),
    val initialized: Boolean = false,
)
