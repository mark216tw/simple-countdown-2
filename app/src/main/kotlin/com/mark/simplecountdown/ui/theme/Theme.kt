package com.mark.simplecountdown.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mark.simplecountdown.model.AppThemeColor

@Composable
fun SimpleCountdownTheme(
    darkTheme: Boolean,
    themeColor: AppThemeColor,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) darkColors(themeColor) else lightColors(themeColor)
    val context = LocalContext.current
    val view = LocalView.current

    SideEffect {
        context.findActivity()?.window?.let { window ->
            @Suppress("DEPRECATION")
            window.navigationBarColor = colors.surface.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightNavigationBars = !darkTheme
                isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(colorScheme = colors, content = content)
}

private fun lightColors(theme: AppThemeColor): ColorScheme = when (theme) {
    AppThemeColor.CORAL -> livelyLightScheme(
        primary = 0xFFC7352A,
        primaryContainer = 0xFFFFDAD5,
        onPrimaryContainer = 0xFF410002,
        secondary = 0xFF8C4A42,
        tertiary = 0xFF775A00,
        background = 0xFFFFF8F6,
    )
    AppThemeColor.TANGERINE -> livelyLightScheme(
        primary = 0xFF9C6400,
        primaryContainer = 0xFFFFDCB3,
        onPrimaryContainer = 0xFF321F00,
        secondary = 0xFF755A2F,
        tertiary = 0xFF4C6542,
        background = 0xFFFFF8F2,
    )
    AppThemeColor.MINT -> livelyLightScheme(
        primary = 0xFF006B57,
        primaryContainer = 0xFF7BF8D5,
        onPrimaryContainer = 0xFF002019,
        secondary = 0xFF4B635B,
        tertiary = 0xFF3F6375,
        background = 0xFFF4FBF7,
    )
    AppThemeColor.OCEAN -> livelyLightScheme(
        primary = 0xFF00639A,
        primaryContainer = 0xFFCEE5FF,
        onPrimaryContainer = 0xFF001D32,
        secondary = 0xFF4F616E,
        tertiary = 0xFF65587B,
        background = 0xFFF7F9FF,
    )
    AppThemeColor.VIOLET -> livelyLightScheme(
        primary = 0xFF6D45B8,
        primaryContainer = 0xFFEBDDFF,
        onPrimaryContainer = 0xFF25005A,
        secondary = 0xFF625B70,
        tertiary = 0xFF7E5260,
        background = 0xFFFCF8FF,
    )
    AppThemeColor.BERRY -> livelyLightScheme(
        primary = 0xFFA93665,
        primaryContainer = 0xFFFFD9E4,
        onPrimaryContainer = 0xFF3F001F,
        secondary = 0xFF74565F,
        tertiary = 0xFF7B5734,
        background = 0xFFFFF8F8,
    )
}

private fun darkColors(theme: AppThemeColor): ColorScheme = when (theme) {
    AppThemeColor.CORAL -> livelyDarkScheme(
        primary = 0xFFFFB4A9,
        onPrimary = 0xFF690005,
        primaryContainer = 0xFF93000A,
        onPrimaryContainer = 0xFFFFDAD5,
        secondary = 0xFFFFB4A9,
        tertiary = 0xFFE9C349,
        background = 0xFF1A1110,
    )
    AppThemeColor.TANGERINE -> livelyDarkScheme(
        primary = 0xFFFFBA52,
        onPrimary = 0xFF522E00,
        primaryContainer = 0xFF754600,
        onPrimaryContainer = 0xFFFFDCB3,
        secondary = 0xFFE7C18D,
        tertiary = 0xFFB1D18A,
        background = 0xFF1B140B,
    )
    AppThemeColor.MINT -> livelyDarkScheme(
        primary = 0xFF5DDBB9,
        onPrimary = 0xFF00382D,
        primaryContainer = 0xFF005141,
        onPrimaryContainer = 0xFF7BF8D5,
        secondary = 0xFFB2CCC2,
        tertiary = 0xFFA7CDDF,
        background = 0xFF0D1512,
    )
    AppThemeColor.OCEAN -> livelyDarkScheme(
        primary = 0xFF94CCFF,
        onPrimary = 0xFF003352,
        primaryContainer = 0xFF004B75,
        onPrimaryContainer = 0xFFCEE5FF,
        secondary = 0xFFB7C9D9,
        tertiary = 0xFFD0BFE8,
        background = 0xFF0D1419,
    )
    AppThemeColor.VIOLET -> livelyDarkScheme(
        primary = 0xFFD3BBFF,
        onPrimary = 0xFF3D1B86,
        primaryContainer = 0xFF542D9E,
        onPrimaryContainer = 0xFFEBDDFF,
        secondary = 0xFFCCC2DB,
        tertiary = 0xFFEFB8C8,
        background = 0xFF15111B,
    )
    AppThemeColor.BERRY -> livelyDarkScheme(
        primary = 0xFFFFB0C8,
        onPrimary = 0xFF650037,
        primaryContainer = 0xFF89164D,
        onPrimaryContainer = 0xFFFFD9E4,
        secondary = 0xFFE3BDC7,
        tertiary = 0xFFE9C18D,
        background = 0xFF1B1114,
    )
}

private fun livelyLightScheme(
    primary: Long,
    primaryContainer: Long,
    onPrimaryContainer: Long,
    secondary: Long,
    tertiary: Long,
    background: Long,
) = lightColorScheme(
    primary = Color(primary),
    onPrimary = Color.White,
    primaryContainer = Color(primaryContainer),
    onPrimaryContainer = Color(onPrimaryContainer),
    secondary = Color(secondary),
    tertiary = Color(tertiary),
    background = Color(background),
    surface = Color(background),
)

private fun livelyDarkScheme(
    primary: Long,
    onPrimary: Long,
    primaryContainer: Long,
    onPrimaryContainer: Long,
    secondary: Long,
    tertiary: Long,
    background: Long,
) = darkColorScheme(
    primary = Color(primary),
    onPrimary = Color(onPrimary),
    primaryContainer = Color(primaryContainer),
    onPrimaryContainer = Color(onPrimaryContainer),
    secondary = Color(secondary),
    tertiary = Color(tertiary),
    background = Color(background),
    surface = Color(background),
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
