package com.mark.simplecountdown.timer

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.mark.simplecountdown.model.TimerSnapshot
import kotlin.math.abs

data class TimerRecord(
    val active: Boolean,
    val paused: Boolean,
    val name: String,
    val originalDurationSeconds: Long,
    val endAtElapsedMillis: Long,
    val wallClockEndAtMillis: Long,
    val bootEpochMillis: Long,
    val bootCount: Int,
    val sameBoot: Boolean,
    val remainingWhenPaused: Long,
    val colorValue: Int,
    val soundEnabled: Boolean,
    val tickSoundEnabled: Boolean,
    val keepScreenOn: Boolean,
    val alarmRinging: Boolean,
    val alarmDurationSeconds: Long,
    val alarmStartedAtWallClockMillis: Long,
) {
    fun remainingSeconds(): Long {
        if (!active) return 0
        if (paused) return remainingWhenPaused.coerceAtLeast(0)
        val remainingMillis = if (sameBoot) {
            endAtElapsedMillis - SystemClock.elapsedRealtime()
        } else {
            wallClockEndAtMillis - System.currentTimeMillis()
        }
        return ((remainingMillis + 999) / 1000).coerceAtLeast(0)
    }

    fun toSnapshot() = TimerSnapshot(
        active = active,
        paused = paused,
        name = name,
        originalDurationSeconds = originalDurationSeconds,
        remainingSeconds = remainingSeconds(),
        colorValue = colorValue,
        soundEnabled = soundEnabled,
        tickSoundEnabled = tickSoundEnabled,
        keepScreenOn = keepScreenOn,
        alarmRinging = alarmRinging,
        alarmDurationSeconds = alarmDurationSeconds,
    )
}

object TimerStateStore {
    private const val PREFS = "native_timer_state_v1"
    private const val ACTIVE = "active"
    private const val PAUSED = "paused"
    private const val NAME = "name"
    private const val ORIGINAL_DURATION = "original_duration"
    private const val END_AT_ELAPSED = "end_at_elapsed"
    private const val WALL_CLOCK_END_AT = "wall_clock_end_at"
    private const val BOOT_EPOCH = "boot_epoch"
    private const val BOOT_COUNT = "boot_count"
    private const val PAUSED_REMAINING = "paused_remaining"
    private const val COLOR = "color"
    private const val SOUND = "sound"
    private const val TICK_SOUND = "tick_sound"
    private const val KEEP_SCREEN_ON = "keep_screen_on"
    private const val ALARM_RINGING = "alarm_ringing"
    private const val ALARM_DURATION = "alarm_duration"
    private const val ALARM_STARTED_AT = "alarm_started_at"

    @Synchronized
    fun read(context: Context): TimerRecord {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val storedBootCount = preferences.getInt(BOOT_COUNT, -1)
        val currentBootCount = currentBootCount(context)
        val storedBootEpoch = preferences.getLong(BOOT_EPOCH, 0)
        val sameBoot = if (currentBootCount >= 0 && storedBootCount >= 0) {
            currentBootCount == storedBootCount
        } else {
            abs((System.currentTimeMillis() - SystemClock.elapsedRealtime()) - storedBootEpoch) < 60_000
        }
        return TimerRecord(
            active = preferences.getBoolean(ACTIVE, false),
            paused = preferences.getBoolean(PAUSED, false),
            name = preferences.getString(NAME, "") ?: "",
            originalDurationSeconds = preferences.getLong(ORIGINAL_DURATION, 0),
            endAtElapsedMillis = preferences.getLong(END_AT_ELAPSED, 0),
            wallClockEndAtMillis = preferences.getLong(WALL_CLOCK_END_AT, 0),
            bootEpochMillis = storedBootEpoch,
            bootCount = storedBootCount,
            sameBoot = sameBoot,
            remainingWhenPaused = preferences.getLong(PAUSED_REMAINING, 0),
            colorValue = preferences.getInt(COLOR, 0xFFE45C4F.toInt()),
            soundEnabled = preferences.getBoolean(SOUND, true),
            tickSoundEnabled = preferences.getBoolean(TICK_SOUND, false),
            keepScreenOn = preferences.getBoolean(KEEP_SCREEN_ON, false),
            alarmRinging = preferences.getBoolean(ALARM_RINGING, false),
            alarmDurationSeconds = preferences.getLong(ALARM_DURATION, 60),
            alarmStartedAtWallClockMillis = preferences.getLong(ALARM_STARTED_AT, 0),
        )
    }

    @Synchronized
    fun start(
        context: Context,
        name: String,
        durationSeconds: Long,
        colorValue: Int,
        soundEnabled: Boolean,
        tickSoundEnabled: Boolean,
        keepScreenOn: Boolean,
        alarmDurationSeconds: Long,
    ): TimerRecord {
        val duration = durationSeconds.coerceIn(1, 359999)
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowWallClock = System.currentTimeMillis()
        return TimerRecord(
            active = true,
            paused = false,
            name = name,
            originalDurationSeconds = duration,
            endAtElapsedMillis = nowElapsed + duration * 1000,
            wallClockEndAtMillis = nowWallClock + duration * 1000,
            bootEpochMillis = nowWallClock - nowElapsed,
            bootCount = currentBootCount(context),
            sameBoot = true,
            remainingWhenPaused = duration,
            colorValue = colorValue,
            soundEnabled = soundEnabled,
            tickSoundEnabled = tickSoundEnabled,
            keepScreenOn = keepScreenOn,
            alarmRinging = false,
            alarmDurationSeconds = normalizeAlarmDuration(alarmDurationSeconds),
            alarmStartedAtWallClockMillis = 0,
        ).also { write(context, it) }
    }

    @Synchronized
    fun pause(context: Context): TimerRecord {
        val current = read(context)
        if (!current.active || current.paused) return current
        val remaining = current.remainingSeconds()
        if (remaining <= 0) return current
        return current.copy(paused = true, remainingWhenPaused = remaining)
            .also { write(context, it) }
    }

    @Synchronized
    fun resume(context: Context): TimerRecord {
        val current = read(context)
        if (!current.active || !current.paused) return current
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowWallClock = System.currentTimeMillis()
        return current.copy(
            paused = false,
            endAtElapsedMillis = nowElapsed + current.remainingWhenPaused * 1000,
            wallClockEndAtMillis = nowWallClock + current.remainingWhenPaused * 1000,
            bootEpochMillis = nowWallClock - nowElapsed,
            bootCount = currentBootCount(context),
            sameBoot = true,
        ).also { write(context, it) }
    }

    @Synchronized
    fun addTime(context: Context, seconds: Long): TimerRecord {
        val current = read(context)
        if (!current.active || seconds <= 0) return current
        val updated = if (current.paused) {
            current.copy(
                remainingWhenPaused = (current.remainingWhenPaused + seconds).coerceAtMost(359999),
            )
        } else {
            val allowedAddition = (359999 - current.remainingSeconds()).coerceAtLeast(0)
            val additionMillis = seconds.coerceAtMost(allowedAddition) * 1000
            current.copy(
                endAtElapsedMillis = current.endAtElapsedMillis + additionMillis,
                wallClockEndAtMillis = current.wallClockEndAtMillis + additionMillis,
            )
        }
        write(context, updated)
        return updated
    }

    @Synchronized
    fun reset(context: Context): TimerRecord {
        val current = read(context)
        if (!current.active) return current
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowWallClock = System.currentTimeMillis()
        return current.copy(
            paused = false,
            endAtElapsedMillis = nowElapsed + current.originalDurationSeconds * 1000,
            wallClockEndAtMillis = nowWallClock + current.originalDurationSeconds * 1000,
            bootEpochMillis = nowWallClock - nowElapsed,
            bootCount = currentBootCount(context),
            sameBoot = true,
            remainingWhenPaused = current.originalDurationSeconds,
        ).also { write(context, it) }
    }

    @Synchronized
    fun rebaseAfterBootIfNeeded(context: Context): TimerRecord {
        val current = read(context)
        if (!current.active || current.sameBoot) return current
        val remaining = current.remainingSeconds()
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowWallClock = System.currentTimeMillis()
        return current.copy(
            endAtElapsedMillis = nowElapsed + remaining * 1000,
            wallClockEndAtMillis = nowWallClock + remaining * 1000,
            bootEpochMillis = nowWallClock - nowElapsed,
            bootCount = currentBootCount(context),
            sameBoot = true,
        ).also { write(context, it) }
    }

    @Synchronized
    fun finish(context: Context): TimerRecord {
        val current = read(context)
        return current.copy(
            active = false,
            paused = false,
            endAtElapsedMillis = 0,
            wallClockEndAtMillis = 0,
            remainingWhenPaused = 0,
            alarmRinging = false,
            alarmStartedAtWallClockMillis = 0,
        ).also { write(context, it) }
    }

    @Synchronized
    fun complete(context: Context): TimerRecord {
        val current = read(context)
        val shouldRing = current.soundEnabled
        return current.copy(
            active = false,
            paused = false,
            endAtElapsedMillis = 0,
            wallClockEndAtMillis = 0,
            remainingWhenPaused = 0,
            alarmRinging = shouldRing,
            alarmStartedAtWallClockMillis = if (shouldRing) System.currentTimeMillis() else 0,
        ).also { write(context, it) }
    }

    @Synchronized
    fun dismissAlarm(context: Context): TimerRecord {
        val current = read(context)
        if (!current.alarmRinging) return current
        return current.copy(alarmRinging = false, alarmStartedAtWallClockMillis = 0)
            .also { write(context, it) }
    }

    @Synchronized
    fun updateSettings(
        context: Context,
        alarmDurationSeconds: Long,
        soundEnabled: Boolean,
        tickSoundEnabled: Boolean,
        keepScreenOn: Boolean,
    ): TimerRecord {
        val current = read(context)
        val keepRinging = current.alarmRinging && soundEnabled
        return current.copy(
            alarmDurationSeconds = normalizeAlarmDuration(alarmDurationSeconds),
            soundEnabled = soundEnabled,
            tickSoundEnabled = tickSoundEnabled,
            keepScreenOn = keepScreenOn,
            alarmRinging = keepRinging,
            alarmStartedAtWallClockMillis = if (keepRinging) current.alarmStartedAtWallClockMillis else 0,
        ).also { write(context, it) }
    }

    @SuppressLint("ApplySharedPref", "UseKtx")
    private fun write(context: Context, record: TimerRecord) {
        // Notification actions and receivers can exit immediately, so this state must be durable now.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ACTIVE, record.active)
            .putBoolean(PAUSED, record.paused)
            .putString(NAME, record.name)
            .putLong(ORIGINAL_DURATION, record.originalDurationSeconds)
            .putLong(END_AT_ELAPSED, record.endAtElapsedMillis)
            .putLong(WALL_CLOCK_END_AT, record.wallClockEndAtMillis)
            .putLong(BOOT_EPOCH, record.bootEpochMillis)
            .putInt(BOOT_COUNT, record.bootCount)
            .putLong(PAUSED_REMAINING, record.remainingWhenPaused)
            .putInt(COLOR, record.colorValue)
            .putBoolean(SOUND, record.soundEnabled)
            .putBoolean(TICK_SOUND, record.tickSoundEnabled)
            .putBoolean(KEEP_SCREEN_ON, record.keepScreenOn)
            .putBoolean(ALARM_RINGING, record.alarmRinging)
            .putLong(ALARM_DURATION, record.alarmDurationSeconds)
            .putLong(ALARM_STARTED_AT, record.alarmStartedAtWallClockMillis)
            .commit()
    }

    private fun currentBootCount(context: Context): Int {
        return Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    }

    private fun normalizeAlarmDuration(seconds: Long): Long = when (seconds) {
        -1L, 0L, 10L, 30L, 60L, 300L -> seconds
        else -> 60L
    }
}
