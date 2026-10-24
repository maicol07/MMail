package it.maicol07.mmail.ui.theme

import android.os.Build
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun platformSeedColor(): Color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    dynamicLightColorScheme(LocalContext.current).primary
} else Color(0xFF3F683B)
