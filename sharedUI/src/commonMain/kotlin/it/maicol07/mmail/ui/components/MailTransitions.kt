package it.maicol07.mmail.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.mailSharedBounds(key: Any, visibilityScope: AnimatedVisibilityScope?): Modifier {
    if (visibilityScope == null) return Modifier
    val boundsSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    return Modifier.sharedBounds(
        sharedContentState = rememberSharedContentState(key),
        animatedVisibilityScope = visibilityScope,
        boundsTransform = { _, _ -> boundsSpec },
        enter = fadeIn(fadeSpec), exit = fadeOut(fadeSpec),
    )
}
