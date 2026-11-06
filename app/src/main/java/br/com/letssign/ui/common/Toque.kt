package br.com.letssign.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import br.com.letssign.ui.theme.Movimento

/** Escala que encolhe de leve enquanto o elemento está pressionado, para dar resposta ao toque. */
@Composable
fun escalaAoPressionar(interacoes: InteractionSource, pressionado: Float = 0.97f): State<Float> {
    val estaPressionado by interacoes.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (estaPressionado) pressionado else 1f,
        animationSpec = Movimento.rapido(),
        label = "escalaAoPressionar",
    )
}
