package br.com.letssign.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.letssign.domain.model.StatusAssinatura
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.ui.theme.Movimento
import br.com.letssign.ui.theme.TemaLetsSign
import br.com.letssign.ui.theme.TomDeStatus

val StatusDocumento.tom: TomDeStatus
    @Composable get() = when (this) {
        StatusDocumento.RASCUNHO -> TemaLetsSign.cores.rascunho
        StatusDocumento.ENVIADO -> TemaLetsSign.cores.enviado
        StatusDocumento.PARCIALMENTE_ASSINADO -> TemaLetsSign.cores.parcial
        StatusDocumento.CONCLUIDO -> TemaLetsSign.cores.concluido
    }

val StatusAssinatura.tom: TomDeStatus
    @Composable get() = when (this) {
        StatusAssinatura.PENDENTE -> TemaLetsSign.cores.pendente
        StatusAssinatura.ASSINADO -> TemaLetsSign.cores.concluido
    }

@Composable
fun ChipStatus(status: StatusDocumento, modifier: Modifier = Modifier) {
    Etiqueta(texto = stringResource(status.rotulo), tom = status.tom, modifier = modifier)
}

@Composable
fun ChipAssinatura(status: StatusAssinatura, modifier: Modifier = Modifier) {
    Etiqueta(texto = stringResource(status.rotulo), tom = status.tom, modifier = modifier)
}

@Composable
private fun Etiqueta(texto: String, tom: TomDeStatus, modifier: Modifier = Modifier) {
    val fundo by animateColorAsState(tom.fundo, Movimento.medio(), label = "fundoDaEtiqueta")
    val conteudo by animateColorAsState(tom.conteudo, Movimento.medio(), label = "conteudoDaEtiqueta")
    Row(
        modifier = modifier
            .background(fundo, CircleShape)
            .padding(start = 9.dp, end = 11.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(conteudo, CircleShape),
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = conteudo,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * Chip de escolha para filtros e seleções. [detalhe] aparece ao lado do texto com menos destaque
 * (um total, por exemplo), [legenda] numa segunda linha e [marcador] como um ponto de cor.
 */
@Composable
fun ChipDeFiltro(
    texto: String,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detalhe: String? = null,
    legenda: String? = null,
    marcador: Color? = null,
) {
    val cores = MaterialTheme.colorScheme
    val interacoes = remember { MutableInteractionSource() }
    val escala by escalaAoPressionar(interacoes, pressionado = 0.95f)
    val fundo by animateColorAsState(
        if (selecionado) cores.onSurface else cores.surface,
        Movimento.rapido(),
        label = "fundoDoChip",
    )
    val conteudo by animateColorAsState(
        if (selecionado) cores.surface else cores.onSurface,
        Movimento.rapido(),
        label = "conteudoDoChip",
    )
    val borda by animateColorAsState(
        if (selecionado) Color.Transparent else cores.outlineVariant,
        Movimento.rapido(),
        label = "bordaDoChip",
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .defaultMinSize(minHeight = 40.dp)
            .clip(CircleShape)
            .background(fundo)
            .border(1.dp, borda, CircleShape)
            .clickable(
                interactionSource = interacoes,
                indication = ripple(),
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { selected = selecionado }
            .padding(horizontal = 16.dp, vertical = if (legenda == null) 0.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (marcador != null) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(marcador, CircleShape),
            )
        }
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = texto,
                    style = MaterialTheme.typography.titleSmall,
                    color = conteudo,
                    maxLines = 1,
                    softWrap = false,
                )
                if (detalhe != null) {
                    Text(
                        text = detalhe,
                        style = MaterialTheme.typography.titleSmall,
                        color = conteudo.copy(alpha = 0.55f),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            if (legenda != null) {
                Text(
                    text = legenda,
                    style = MaterialTheme.typography.labelSmall,
                    color = conteudo.copy(alpha = 0.65f),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
