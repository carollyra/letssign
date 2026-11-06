package br.com.letssign.ui.documento

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.ui.common.BotaoDeTexto
import br.com.letssign.ui.common.BotaoPrincipal
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.Cartao
import br.com.letssign.ui.common.ControleSegmentado
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.EstadoVazio
import br.com.letssign.ui.common.Icones
import br.com.letssign.ui.common.entradaSuave
import br.com.letssign.ui.common.progressoDoItem
import br.com.letssign.ui.common.rememberEntradaDeLista
import br.com.letssign.ui.common.rotulo
import br.com.letssign.ui.theme.Espacos
import br.com.letssign.ui.theme.Movimento

@Composable
fun SelecaoSignatariosScreen(
    onVoltar: () -> Unit,
    onCadastrarSignatario: () -> Unit,
    onProximaEtapa: (Long) -> Unit,
    viewModel: SelecaoSignatariosViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.proximaEtapa.collect(onProximaEtapa)
    }

    when {
        estado.carregando -> Carregando()
        estado.erroCarregar -> EstadoErro(
            mensagem = stringResource(R.string.documento_erro_nao_encontrado),
            onTentarNovamente = viewModel::carregar,
        )
        else -> Column(modifier = Modifier.fillMaxSize()) {
            if (estado.itens.isEmpty()) {
                EstadoVazio(
                    icone = Icones.Pessoas,
                    titulo = stringResource(R.string.signatarios_vazio_titulo),
                    descricao = stringResource(R.string.selecao_vazio_descricao),
                    modifier = Modifier.weight(1f),
                    acao = {
                        BotaoPrincipal(
                            texto = stringResource(R.string.selecao_cadastrar),
                            onClick = onCadastrarSignatario,
                            icone = Icones.Mais,
                        )
                    },
                )
            } else {
                val entrada = rememberEntradaDeLista()
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Espacos.tela, vertical = Espacos.xs),
                    verticalArrangement = Arrangement.spacedBy(Espacos.s),
                ) {
                    item(key = "instrucao") {
                        Text(
                            text = stringResource(R.string.selecao_instrucao),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    itemsIndexed(estado.itens, key = { _, item -> item.usuario.id }) { indice, item ->
                        ItemSignatario(
                            item = item,
                            onAlternar = { viewModel.alternar(item.usuario.id) },
                            onDefinirPapel = { viewModel.definirPapel(item.usuario.id, it) },
                            modifier = Modifier
                                .animateItem()
                                .entradaSuave(entrada.progressoDoItem(indice)),
                        )
                    }
                    item(key = "cadastrar") {
                        BotaoDeTexto(
                            texto = stringResource(R.string.selecao_cadastrar),
                            onClick = onCadastrarSignatario,
                            icone = Icones.Mais,
                        )
                    }
                }
            }

            val erro = when {
                estado.erroMinimo -> stringResource(R.string.selecao_erro_minimo)
                estado.erroGeral != null -> stringResource(estado.erroGeral!!)
                else -> null
            }
            if (estado.itens.isNotEmpty()) {
                Text(
                    text = erro ?: pluralStringResource(
                        R.plurals.selecao_total,
                        estado.totalSelecionados,
                        estado.totalSelecionados,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (erro != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = Espacos.tela, vertical = Espacos.xxs),
                )
            }
            AcoesDaEtapa(
                textoPrincipal = stringResource(
                    if (estado.salvando) R.string.acao_salvando else R.string.acao_continuar,
                ),
                onPrincipal = viewModel::continuar,
                carregando = estado.salvando,
                onVoltar = onVoltar,
            )
        }
    }
}

@Composable
private fun ItemSignatario(
    item: ItemSelecao,
    onAlternar: () -> Unit,
    onDefinirPapel: (PapelSignatario) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selecionado = item.papel != null
    val papeis = PapelSignatario.entries

    Cartao(
        onClick = onAlternar,
        margemInterna = PaddingValues(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Checkbox
                selected = selecionado
            },
    ) {
        Row(
            modifier = Modifier.padding(Espacos.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MarcaDeSelecao(selecionado = selecionado)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    text = item.usuario.nome,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.usuario.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        AnimatedVisibility(visible = selecionado) {
            ControleSegmentado(
                opcoes = papeis.map { stringResource(it.rotulo) },
                indiceSelecionado = papeis.indexOf(item.papel),
                onSelecionar = { onDefinirPapel(papeis[it]) },
                modifier = Modifier.padding(start = Espacos.m, end = Espacos.m, bottom = Espacos.m),
            )
        }
    }
}

@Composable
private fun MarcaDeSelecao(selecionado: Boolean) {
    val cores = MaterialTheme.colorScheme
    val fundo by animateColorAsState(
        targetValue = if (selecionado) cores.primary else Color.Transparent,
        animationSpec = Movimento.rapido(),
        label = "fundoDaMarca",
    )
    val borda by animateColorAsState(
        targetValue = if (selecionado) cores.primary else cores.outline,
        animationSpec = Movimento.rapido(),
        label = "bordaDaMarca",
    )
    val check by animateFloatAsState(
        targetValue = if (selecionado) 1f else 0f,
        animationSpec = Movimento.rapido(),
        label = "checkDaMarca",
    )
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(fundo, CircleShape)
            .border(1.5.dp, borda, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icones.Check,
            contentDescription = null,
            tint = cores.onPrimary,
            modifier = Modifier
                .size(15.dp)
                .graphicsLayer {
                    alpha = check
                    scaleX = 0.6f + 0.4f * check
                    scaleY = 0.6f + 0.4f * check
                },
        )
    }
}
