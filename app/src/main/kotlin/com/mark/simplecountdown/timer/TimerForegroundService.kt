package com.mark.simplecountdown.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.mark.simplecountdown.MainActivity
import com.mark.simplecountdown.R
import com.mark.simplecountdown.util.formatDuration
import kotlin.math.max

class TimerForegroundService : Service() {
    companion object {
        const val ACTION_START = "com.example.simplecountdown.START"
        const val ACTION_PAUSE = "com.example.simplecountdown.PAUSE"
        const val ACTION_RESUME = "com.example.simplecountdown.RESUME"
        const val ACTION_ADD_TIME = "com.example.simplecountdown.ADD_TIME"
        const val ACTION_RESET = "com.example.simplecountdown.RESET"
        const val ACTION_STOP = "com.example.simplecountdown.STOP"
        const val ACTION_SYNC = "com.example.simplecountdown.SYNC"
        const val ACTION_DISMISS_ALARM = "com.example.simplecountdown.DISMISS_ALARM"

        private const val ACTIVE_CHANNEL = "active_timer_v1"
        private const val ALARM_CHANNEL = "timer_alarm_v2"
        private const val ACTIVE_NOTIFICATION_ID = 1001
        private const val COMPLETION_NOTIFICATION_ID = 1002
        private const val WAKE_LOCK_TIMEOUT_MILLIS = 10 * 60 * 1000L
        private const val WAKE_LOCK_RENEW_MILLIS = 9 * 60 * 1000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var lastDisplayedSecond = -1L
    private val alarmTimeout = Runnable { stopAlarm(removeNotification = false) }
    private val wakeLockRenewer = Runnable {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        acquireWakeLock()
    }
    private val ticker = object : Runnable {
        override fun run() {
            val state = TimerStateStore.read(this@TimerForegroundService)
            if (!state.active) {
                stopServiceSilently()
                return
            }
            if (!state.paused && state.remainingSeconds() <= 0) {
                completeTimer()
                return
            }
            val remaining = state.remainingSeconds()
            if (remaining != lastDisplayedSecond) {
                lastDisplayedSecond = remaining
                playTick(state)
                notificationManager().notify(
                    ACTIVE_NOTIFICATION_ID,
                    buildActiveNotification(state),
                )
            }
            handler.postDelayed(this, 250)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = when (intent?.action) {
            ACTION_START -> TimerStateStore.start(
                context = this,
                name = intent.getStringExtra("name") ?: "倒數計時",
                durationSeconds = intent.getLongExtra("durationSeconds", 1),
                colorValue = intent.getIntExtra("colorValue", 0xFFE45C4F.toInt()),
                soundEnabled = intent.getBooleanExtra("soundEnabled", true),
                tickSoundEnabled = intent.getBooleanExtra("tickSoundEnabled", false),
                keepScreenOn = intent.getBooleanExtra("keepScreenOn", false),
                alarmDurationSeconds = intent.getLongExtra("alarmDurationSeconds", 60),
            )
            ACTION_PAUSE -> TimerStateStore.pause(this)
            ACTION_RESUME -> TimerStateStore.resume(this)
            ACTION_ADD_TIME -> TimerStateStore.addTime(
                this,
                intent.getLongExtra("seconds", 0),
            )
            ACTION_RESET -> TimerStateStore.reset(this)
            ACTION_STOP -> {
                TimerStateStore.finish(this)
                stopServiceSilently(removeCompletionNotification = true)
                return START_NOT_STICKY
            }
            ACTION_DISMISS_ALARM -> {
                val current = TimerStateStore.read(this)
                if (current.alarmRinging) {
                    TimerStateStore.dismissAlarm(this)
                    stopAlarm(removeNotification = true)
                    return START_NOT_STICKY
                }
                if (!current.active) {
                    stopServiceSilently(removeCompletionNotification = true)
                    return START_NOT_STICKY
                }
                current
            }
            else -> TimerStateStore.read(this)
        }

        if (state.alarmRinging) {
            stopTickSound()
            startForeground(COMPLETION_NOTIFICATION_ID, buildCompletionNotification(state))
            acquireWakeLock()
            stopAlarmOutputs()
            startAlarmOutputs(state)
            scheduleAlarmTimeout(state)
            return START_STICKY
        }

        if (!state.active) {
            stopServiceSilently()
            return START_NOT_STICKY
        }

        stopAlarmOutputs()
        syncTickSound(state)
        handler.removeCallbacks(alarmTimeout)
        notificationManager().cancel(COMPLETION_NOTIFICATION_ID)
        startForeground(ACTIVE_NOTIFICATION_ID, buildActiveNotification(state))
        if (state.paused) releaseWakeLock() else acquireWakeLock()
        restartTicker()
        return START_STICKY
    }

    private fun restartTicker() {
        handler.removeCallbacks(ticker)
        lastDisplayedSecond = -1
        handler.post(ticker)
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:countdown",
        ).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MILLIS)
        }
        handler.removeCallbacks(wakeLockRenewer)
        handler.postDelayed(wakeLockRenewer, WAKE_LOCK_RENEW_MILLIS)
    }

    private fun releaseWakeLock() {
        handler.removeCallbacks(wakeLockRenewer)
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun completeTimer() {
        val completed = TimerStateStore.complete(this)
        stopTickSound()
        releaseWakeLock()
        handler.removeCallbacks(ticker)
        notificationManager().cancel(ACTIVE_NOTIFICATION_ID)
        if (completed.alarmRinging) {
            startForeground(
                COMPLETION_NOTIFICATION_ID,
                buildCompletionNotification(completed),
            )
            acquireWakeLock()
            startAlarmOutputs(completed)
            scheduleAlarmTimeout(completed)
        } else {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            notificationManager().notify(
                COMPLETION_NOTIFICATION_ID,
                buildCompletionNotification(completed),
            )
            stopSelf()
        }
    }

    private fun stopServiceSilently(removeCompletionNotification: Boolean = false) {
        stopTickSound()
        stopAlarmOutputs()
        releaseWakeLock()
        handler.removeCallbacks(ticker)
        handler.removeCallbacks(alarmTimeout)
        handler.removeCallbacks(wakeLockRenewer)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        notificationManager().cancel(ACTIVE_NOTIFICATION_ID)
        if (removeCompletionNotification) {
            notificationManager().cancel(COMPLETION_NOTIFICATION_ID)
        }
        stopSelf()
    }

    private fun stopAlarm(removeNotification: Boolean) {
        stopTickSound()
        val state = TimerStateStore.dismissAlarm(this)
        stopAlarmOutputs()
        releaseWakeLock()
        handler.removeCallbacks(alarmTimeout)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (removeNotification) {
            notificationManager().cancel(COMPLETION_NOTIFICATION_ID)
        } else {
            notificationManager().notify(
                COMPLETION_NOTIFICATION_ID,
                buildCompletionNotification(state),
            )
        }
        stopSelf()
    }

    private fun buildActiveNotification(state: TimerRecord): Notification {
        val remaining = state.remainingSeconds().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val total = max(state.originalDurationSeconds.toInt(), remaining)
        val pauseOrResume = if (state.paused) ACTION_RESUME else ACTION_PAUSE
        val pauseOrResumeLabel = getString(if (state.paused) R.string.resume else R.string.pause)
        val pauseOrResumeIcon = if (state.paused) {
            android.R.drawable.ic_media_play
        } else {
            android.R.drawable.ic_media_pause
        }

        return NotificationCompat.Builder(this, ACTIVE_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(state.name)
            .setContentText(
                if (state.paused) {
                    "${getString(R.string.paused)} · ${formatDuration(remaining.toLong())}"
                } else {
                    "剩餘 ${formatDuration(remaining.toLong())}"
                },
            )
            .setContentIntent(openAppPendingIntent(10))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setColor(state.colorValue)
            .setColorized(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setProgress(total, (total - remaining).coerceAtLeast(0), state.paused)
            .addAction(
                pauseOrResumeIcon,
                pauseOrResumeLabel,
                servicePendingIntent(pauseOrResume, 20),
            )
            .addAction(
                android.R.drawable.ic_input_add,
                getString(R.string.add_one_minute),
                servicePendingIntent(ACTION_ADD_TIME, 21, 60),
            )
            .addAction(
                android.R.drawable.ic_delete,
                getString(R.string.stop),
                servicePendingIntent(ACTION_STOP, 22),
            )
            .build()
    }

    private fun buildCompletionNotification(state: TimerRecord): Notification {
        val builder = NotificationCompat.Builder(this, ALARM_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(getString(R.string.time_up))
            .setContentText(
                if (state.alarmRinging) "${state.name} · 點擊停止鬧鈴" else state.name,
            )
            .setContentIntent(openAppPendingIntent(30))
            .setAutoCancel(!state.alarmRinging)
            .setOngoing(state.alarmRinging)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setColor(state.colorValue)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (state.alarmRinging) {
            builder.addAction(
                R.drawable.ic_notification_timer,
                getString(R.string.dismiss_alarm),
                servicePendingIntent(ACTION_DISMISS_ALARM, 31),
            )
        }
        return builder.build()
    }

    private fun openAppPendingIntent(requestCode: Int): PendingIntent = PendingIntent.getActivity(
        this,
        requestCode,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun servicePendingIntent(
        action: String,
        requestCode: Int,
        seconds: Long = 0,
    ): PendingIntent {
        val intent = Intent(this, TimerForegroundService::class.java).apply {
            this.action = action
            if (seconds != 0L) putExtra("seconds", seconds)
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = notificationManager()
        manager.createNotificationChannel(
            NotificationChannel(
                ACTIVE_CHANNEL,
                getString(R.string.active_timer_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.active_timer_channel_description)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ALARM_CHANNEL,
                getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.alarm_channel_description)
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    private fun scheduleAlarmTimeout(state: TimerRecord) {
        handler.removeCallbacks(alarmTimeout)
        if (state.alarmDurationSeconds <= 0) return
        val elapsed = System.currentTimeMillis() - state.alarmStartedAtWallClockMillis
        val remainingMillis = state.alarmDurationSeconds * 1000 - elapsed
        handler.postDelayed(alarmTimeout, remainingMillis.coerceAtLeast(0))
    }

    private fun startAlarmOutputs(state: TimerRecord) {
        if (state.soundEnabled && mediaPlayer == null) startAlarmSound()
    }

    private fun startAlarmSound() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        requestAudioFocus(audioAttributes)
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(this@TimerForegroundService, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            mediaPlayer?.release()
            mediaPlayer = null
            abandonAudioFocus()
        }
    }

    private fun syncTickSound(state: TimerRecord) {
        if (!state.tickSoundEnabled || state.paused) {
            stopTickSound()
            return
        }
        if (toneGenerator == null) {
            toneGenerator = try {
                ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            } catch (_: RuntimeException) {
                null
            }
        }
    }

    private fun playTick(state: TimerRecord) {
        if (!state.active || state.paused || !state.tickSoundEnabled) return
        syncTickSound(state)
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
    }

    private fun stopTickSound() {
        toneGenerator?.release()
        toneGenerator = null
    }

    private fun requestAudioFocus(attributes: AudioAttributes) {
        val manager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { }
                .build()
            manager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            )
        }
    }

    private fun stopAlarmOutputs() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) player.stop()
            } catch (_: IllegalStateException) {
                // The system may have already invalidated the player.
            }
            player.release()
        }
        mediaPlayer = null
        abandonAudioFocus()
    }

    @Suppress("DEPRECATION")
    private fun abandonAudioFocus() {
        val manager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let(manager::abandonAudioFocusRequest)
            audioFocusRequest = null
        } else {
            manager.abandonAudioFocus(null)
        }
    }

    private fun notificationManager(): NotificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        handler.removeCallbacks(alarmTimeout)
        stopTickSound()
        stopAlarmOutputs()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
