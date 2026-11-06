package br.com.letssign.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.letssign.ui.theme.Movimento
import br.com.letssign.ui.theme.TemaLetsSign

/** Círculo que cresce e, em seguida, o check sendo traçado dentro dele. Roda uma vez ao entrar na tela. */
@Composable
fun CheckAnimado(
    modifier: Modifier = Modifier,
    tamanho: Dp = 96.dp,
    fundo: Color = TemaLetsSign.cores.concluido.fundo,
    cor: Color = TemaLetsSign.cores.concluido.conteudo,
) {
    val circulo = remember { Animatable(0f) }
    val traco = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        circulo.animateTo(1f, tween(Movimento.MEDIO, easing = Movimento.Suave))
        traco.animateTo(1f, tween(Movimento.LENTO, easing = Movimento.Suave))
    }

    val caminho = remember { Path() }
    val trecho = remember { Path() }
    val medida = remember { PathMeasure() }

    Canvas(modifier = modifier.size(tamanho)) {
        val lado = size.minDimension
        drawCircle(
            color = fundo,
            radius = lado / 2f * (0.6f + 0.4f * circulo.value),
            alpha = circulo.value,
        )
        if (traco.value > 0f) {
            caminho.rewind()
            caminho.moveTo(lado * 0.30f, lado * 0.52f)
            caminho.lineTo(lado * 0.44f, lado * 0.66f)
            caminho.lineTo(lado * 0.71f, lado * 0.37f)
            medida.setPath(caminho, false)
            trecho.rewind()
            medida.getSegment(0f, medida.length * traco.value, trecho, true)
            drawPath(
                path = trecho,
                color = cor,
                style = Stroke(width = lado * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
