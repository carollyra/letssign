package br.com.letssign.ui.documento

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.letssign.R
import br.com.letssign.ui.common.BotaoIconeTonal
import br.com.letssign.ui.common.BotaoPrincipal
import br.com.letssign.ui.common.Icones
import br.com.letssign.ui.theme.Espacos
import br.com.letssign.ui.theme.Movimento

enum class EtapaDocumento(@StringRes val titulo: Int) {
    ARQUIVO(R.string.etapa_arquivo),
    SIGNATARIOS(R.string.etapa_signatarios),
    CAMPOS(R.string.etapa_campos),
    REVISAO(R.string.etapa_revisao),
}

/** Um segmento por etapa; o preenchimento corre de uma para a outra quando a etapa muda. */
@Composable
fun IndicadorEtapas(etapa: EtapaDocumento, modifier: Modifier = Modifier) {
    val total = EtapaDocumento.entries.size
    val progresso by animateFloatAsState(
        targetValue = etapa.ordinal + 1f,
        animationSpec = Movimento.lento(),
        label = "progressoDasEtapas",
    )

    Column(modifier = modifier.padding(horizontal = Espacos.tela)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { indice ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((progresso - indice).coerceIn(0f, 1f))
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
            }
        }
        Spacer(Modifier.height(Espacos.m))
        Text(
            text = stringResource(R.string.etapa_posicao, etapa.ordinal + 1, total),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AnimatedContent(
            targetState = etapa,
            transitionSpec = {
                val sentido = if (targetState > initialState) 1 else -1
                (slideInHorizontally(Movimento.lento()) { sentido * it / 6 } + fadeIn(Movimento.medio()))
                    .togetherWith(
                        slideOutHorizontally(Movimento.lento()) { -sentido * it / 6 } + fadeOut(Movimento.rapido()),
                    )
            },
            label = "tituloDaEtapa",
        ) { atual ->
            Text(
                text = stringResource(atual.titulo),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
            )
        }
    }
}

/** Rodapé das etapas: a ação principal e, quando há etapa anterior, o caminho de volta. */
@Composable
fun AcoesDaEtapa(
    textoPrincipal: String,
    onPrincipal: () -> Unit,
    modifier: Modifier = Modifier,
    carregando: Boolean = false,
    habilitado: Boolean = true,
    iconePrincipal: ImageVector? = null,
    onVoltar: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Espacos.tela, end = Espacos.tela, top = Espacos.s, bottom = Espacos.m),
        horizontalArrangement = Arrangement.spacedBy(Espacos.s),
    ) {
        if (onVoltar != null) {
            BotaoIconeTonal(
                icone = Icones.Voltar,
                descricao = stringResource(R.string.acao_voltar),
                onClick = onVoltar,
                habilitado = !carregando,
            )
        }
        BotaoPrincipal(
            texto = textoPrincipal,
            onClick = onPrincipal,
            habilitado = habilitado,
            carregando = carregando,
            icone = iconePrincipal,
            modifier = Modifier.weight(1f),
        )
    }
}
