package br.com.letssign.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object Espacos {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 20.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Margem lateral padrão do conteúdo das telas. */
    val tela = 20.dp
}

internal val FormasLetsSign = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object Movimento {
    const val RAPIDO = 200
    const val MEDIO = 280
    const val LENTO = 350

    /** Sai rápido e desacelera até parar, sem ultrapassar o destino. */
    val Suave = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> rapido(): FiniteAnimationSpec<T> = tween(RAPIDO, easing = Suave)

    fun <T> medio(): FiniteAnimationSpec<T> = tween(MEDIO, easing = Suave)

    fun <T> lento(): FiniteAnimationSpec<T> = tween(LENTO, easing = Suave)
}
