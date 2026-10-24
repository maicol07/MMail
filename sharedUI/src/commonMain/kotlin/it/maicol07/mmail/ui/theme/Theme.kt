package it.maicol07.mmail.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicMaterialThemeState

@Composable
expect fun platformSeedColor(): Color

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    val state = rememberDynamicMaterialThemeState(
        isDark = dark,
        seedColor = platformSeedColor(),
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )
    DynamicMaterialExpressiveTheme(
        state = state,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
