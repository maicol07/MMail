package it.maicol07.mmail.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlainTextMessageBody(text: String, modifier: Modifier)
