package br.com.letssign.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import br.com.letssign.ui.theme.Movimento

private val FORMA_DO_TRILHO = RoundedCornerShape(12.dp)
private val FORMA_DO_SEGMENTO = RoundedCornerShape(9.dp)

/**
 * Escolha entre poucas opções de mesma largura, com o marcador deslizando até a selecionada.
 * [indiceSelecionado] negativo deixa o controle sem marcador.
 */
@Composable
fun ControleSegmentado(
    opcoes: List<String>,
    indiceSelecionado: Int,
    onSelecionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(cores.surfaceVariant, FORMA_DO_TRILHO)
            .padding(3.dp),
    ) {
        val larguraDoSegmento = maxWidth / opcoes.size
        val larguraEmPixels = with(LocalDensity.current) { larguraDoSegmento.roundToPx() }
        val deslocamento by animateIntAsState(
            targetValue = larguraEmPixels * indiceSelecionado.coerceAtLeast(0),
            animationSpec = Movimento.medio(),
            label = "marcadorDoSegmento",
        )
        if (indiceSelecionado >= 0) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(deslocamento, 0) }
                    .width(larguraDoSegmento)
                    .fillMaxHeight()
                    .shadow(2.dp, FORMA_DO_SEGMENTO)
                    .background(cores.surfaceBright, FORMA_DO_SEGMENTO),
            )
        }
        Row(modifier = Modifier.fillMaxSize()) {
            opcoes.forEachIndexed { indice, texto ->
                val selecionado = indice == indiceSelecionado
                val cor by animateColorAsState(
                    targetValue = if (selecionado) cores.onSurface else cores.onSurfaceVariant,
                    animationSpec = Movimento.rapido(),
                    label = "textoDoSegmento",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selecionado,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelecionar(indice) },
                        )
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = texto,
                        style = MaterialTheme.typography.labelMedium,
                        color = cor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
