package br.com.letssign.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun LetsSignTheme(escuro: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCoresLetsSign provides if (escuro) CoresEscuras else CoresClaras) {
        MaterialTheme(
            colorScheme = if (escuro) EsquemaEscuro else EsquemaClaro,
            typography = TipografiaLetsSign,
            shapes = FormasLetsSign,
            content = content,
        )
    }
}

object TemaLetsSign {
    val cores: CoresLetsSign
        @Composable
        @ReadOnlyComposable
        get() = LocalCoresLetsSign.current
}
