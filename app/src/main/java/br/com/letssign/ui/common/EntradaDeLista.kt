package br.com.letssign.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import br.com.letssign.ui.theme.Movimento
import kotlinx.coroutines.delay

/**
 * Controla a entrada dos itens quando uma lista aparece pela primeira vez. Depois disso os itens
 * que surgem ao rolar não animam de novo; inclusões e remoções ficam por conta do `animateItem`.
 */
class EntradaDeLista {
    internal var concluida = false
}

@Composable
fun rememberEntradaDeLista(): EntradaDeLista = remember { EntradaDeLista() }

/** Progresso de 0 a 1 da entrada do item, escalonado pela posição dele na lista. */
@Composable
fun EntradaDeLista.progressoDoItem(indice: Int): State<Float> {
    val progresso = remember { Animatable(if (concluida) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (progresso.value < 1f) {
            delay(indice.coerceAtMost(ITENS_ESCALONADOS) * ATRASO_POR_ITEM)
            concluida = true
            progresso.animateTo(1f, tween(Movimento.MEDIO, easing = Movimento.Suave))
        }
    }

    return progresso.asState()
}

/** Fade com um pequeno deslocamento para cima, conforme o [progresso] da entrada. */
fun Modifier.entradaSuave(progresso: State<Float>): Modifier = graphicsLayer {
    alpha = progresso.value
    translationY = (1f - progresso.value) * 14.dp.toPx()
}

private const val ITENS_ESCALONADOS = 8
private const val ATRASO_POR_ITEM = 35L
