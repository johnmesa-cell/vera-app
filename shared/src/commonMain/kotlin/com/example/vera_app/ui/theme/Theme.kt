package com.example.vera_app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Tema principal de la aplicación Vera.
 * Proporciona el tema visual unificado de Compose Material 3 para Android y Desktop.
 */
@Composable
fun VeraTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        content = content,
    )
}
