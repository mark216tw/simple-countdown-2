package com.mark.simplecountdown.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mark.simplecountdown.AppViewModel
import com.mark.simplecountdown.model.TimerPreset
import com.mark.simplecountdown.ui.home.HomeScreen
import com.mark.simplecountdown.ui.theme.SimpleCountdownTheme
import com.mark.simplecountdown.ui.timer.TimerScreen

private const val HOME_ROUTE = "home"
private const val TIMER_ROUTE = "timer"

@Composable
fun CountdownApp(viewModel: AppViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var pendingPreset by remember { mutableStateOf<TimerPreset?>(null) }

    fun openTimer(preset: TimerPreset) {
        viewModel.startTimer(preset)
        navController.navigate(TIMER_ROUTE) { launchSingleTop = true }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { allowed ->
        pendingPreset?.let(::openTimer)
        pendingPreset = null
        if (!allowed) {
            viewModel.showMessage("通知未開啟，離開 App 後可能無法看到完成提醒。")
        }
    }

    val startTimer: (TimerPreset) -> Unit = { preset ->
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingPreset = preset
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openTimer(preset)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onAppResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SimpleCountdownTheme(
        darkTheme = uiState.settings.darkMode,
        themeColor = uiState.settings.themeColor,
    ) {
        if (!uiState.initialized) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@SimpleCountdownTheme
        }

        NavHost(navController = navController, startDestination = HOME_ROUTE) {
            composable(HOME_ROUTE) {
                HomeScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onAddPreset = viewModel::addPreset,
                    onUpdatePreset = viewModel::updatePreset,
                    onDuplicatePreset = viewModel::duplicatePreset,
                    onDeletePreset = viewModel::deletePreset,
                    onSavePresetOrder = viewModel::savePresetOrder,
                    onSaveCustomTimer = viewModel::saveLastCustomTimer,
                    onSaveSettings = viewModel::saveSettings,
                    onStartTimer = startTimer,
                    onOpenTimer = {
                        navController.navigate(TIMER_ROUTE) { launchSingleTop = true }
                    },
                    onDismissAlarm = viewModel::dismissAlarm,
                )
            }
            composable(TIMER_ROUTE) {
                TimerScreen(
                    timer = uiState.timer,
                    snackbarHostState = snackbarHostState,
                    onBack = navController::popBackStack,
                    onPause = viewModel::pauseTimer,
                    onResume = viewModel::resumeTimer,
                    onAddTime = viewModel::addTime,
                    onReset = viewModel::resetTimer,
                    onStop = viewModel::stopTimer,
                    onDismissAlarm = viewModel::dismissAlarm,
                    onRestart = viewModel::restartTimer,
                )
            }
        }
    }
}
