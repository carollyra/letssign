package br.com.letssign.ui.pdf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import br.com.letssign.domain.regra.AreaNormalizada
import br.com.letssign.domain.regra.GeometriaCampo
import kotlin.math.roundToInt

data class CampoVisual(val area: AreaNormalizada, val cor: Color, val rotulo: String)

val FORMA_DO_CAMPO = RoundedCornerShape(6.dp)

/** Desenha os campos, sem interação, por cima de uma página de [tamanho] pixels. */
@Composable
fun CamposSobreAPagina(campos: List<CampoVisual>, tamanho: IntSize) {
    val densidade = LocalDensity.current
    campos.forEach { campo ->
        val pixels = GeometriaCampo.paraPixels(campo.area, tamanho.width.toFloat(), tamanho.height.toFloat())
        Box(
            modifier = Modifier
                .offset { IntOffset(pixels.x.roundToInt(), pixels.y.roundToInt()) }
                .size(
                    width = with(densidade) { pixels.largura.toDp() },
                    height = with(densidade) { pixels.altura.toDp() },
                )
                .background(campo.cor.copy(alpha = 0.14f), FORMA_DO_CAMPO)
                .border(1.5.dp, campo.cor, FORMA_DO_CAMPO),
            contentAlignment = Alignment.Center,
        ) {
            RotuloDoCampo(texto = campo.rotulo, cor = campo.cor)
        }
    }
}

@Composable
fun RotuloDoCampo(texto: String, cor: Color) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = cor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}
