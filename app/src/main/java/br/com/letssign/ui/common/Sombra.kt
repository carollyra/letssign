package br.com.letssign.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

/**
 * Sombra desenhada só do lado de fora do contorno. A sombra de elevação comum aparece por baixo
 * de um preenchimento translúcido e escurece o que está sob ele; esta não.
 */
fun Modifier.sombraExterna(
    elevacao: () -> Dp,
    raioDoCanto: Dp,
    cor: Color = Color.Black.copy(alpha = 0.30f),
): Modifier = drawBehind {
    val altura = elevacao().toPx()
    if (altura <= 0f) return@drawBehind
    val raio = raioDoCanto.toPx()
    val contorno = Path().apply {
        addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(raio)))
    }
    clipPath(contorno, clipOp = ClipOp.Difference) {
        drawIntoCanvas { tela ->
            val pincel = Paint().asFrameworkPaint().apply {
                color = cor.toArgb()
                setShadowLayer(altura * 1.6f, 0f, altura * 0.6f, cor.toArgb())
            }
            tela.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, raio, raio, pincel)
        }
    }
}
