package com.mark.simplecountdown.timer

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mark.simplecountdown.model.TimerPreset
import com.mark.simplecountdown.model.TimerSettings
import com.mark.simplecountdown.model.TimerSnapshot

class TimerRepository(context: Context) {
    private val appContext = context.applicationContext

    fun state(): TimerSnapshot = TimerStateStore.read(appContext).toSnapshot()

    fun start(preset: TimerPreset, settings: TimerSettings) {
        TimerStateStore.start(
            context = appContext,
            name = preset.name,
            durationSeconds = preset.durationSeconds,
            colorValue = preset.colorValue,
            soundEnabled = settings.alarmDurationSeconds != 0L,
            tickSoundEnabled = settings.tickSoundEnabled,
            keepScreenOn = settings.keepScreenOn,
            alarmDurationSeconds = settings.alarmDurationSeconds,
        )
        try {
            ContextCompat.startForegroundService(
                appContext,
                serviceIntent(TimerForegroundService.ACTION_SYNC),
            )
        } catch (error: RuntimeException) {
            TimerStateStore.finish(appContext)
            throw error
        }
    }

    fun pause() {
        TimerStateStore.pause(appContext)
        sendCommand(TimerForegroundService.ACTION_SYNC)
    }

    fun resume() {
        TimerStateStore.resume(appContext)
        sendCommand(TimerForegroundService.ACTION_SYNC)
    }

    fun addTime(seconds: Long) {
        TimerStateStore.addTime(appContext, seconds)
        sendCommand(TimerForegroundService.ACTION_SYNC)
    }

    fun reset() {
        TimerStateStore.reset(appContext)
        sendCommand(TimerForegroundService.ACTION_SYNC)
    }

    fun stop() {
        TimerStateStore.finish(appContext)
        sendCommand(TimerForegroundService.ACTION_STOP)
    }

    fun dismissAlarm() {
        TimerStateStore.dismissAlarm(appContext)
        sendCommand(TimerForegroundService.ACTION_DISMISS_ALARM)
    }

    fun updateSettings(settings: TimerSettings) {
        val previous = TimerStateStore.read(appContext)
        TimerStateStore.updateSettings(
            context = appContext,
            alarmDurationSeconds = settings.alarmDurationSeconds,
            soundEnabled = settings.alarmDurationSeconds != 0L,
            tickSoundEnabled = settings.tickSoundEnabled,
            keepScreenOn = settings.keepScreenOn,
        )
        if (previous.active || previous.alarmRinging) {
            sendCommand(TimerForegroundService.ACTION_SYNC)
        }
    }

    fun ensureService() {
        val state = TimerStateStore.read(appContext)
        if (!state.active && !state.alarmRinging) return
        ContextCompat.startForegroundService(
            appContext,
            serviceIntent(TimerForegroundService.ACTION_SYNC),
        )
    }

    private fun sendCommand(action: String, configure: Intent.() -> Unit = {}) {
        appContext.startService(serviceIntent(action).apply(configure))
    }

    private fun serviceIntent(action: String) =
        Intent(appContext, TimerForegroundService::class.java).apply { this.action = action }
}
