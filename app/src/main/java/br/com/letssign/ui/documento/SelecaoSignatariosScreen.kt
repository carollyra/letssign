package br.com.letssign.ui.documento

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.EstadoVazio
import br.com.letssign.ui.common.rotulo

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

    Scaffold(
        topBar = {
            BarraSuperior(titulo = stringResource(R.string.selecao_titulo), onVoltar = onVoltar)
        },
    ) { margens ->
        when {
            estado.carregando -> Carregando(Modifier.padding(margens))
            estado.erroCarregar -> EstadoErro(
                mensagem = stringResource(R.string.documento_erro_nao_encontrado),
                modifier = Modifier.padding(margens),
                onTentarNovamente = viewModel::carregar,
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(margens),
            ) {
                IndicadorEtapa(EtapaDocumento.SIGNATARIOS)
                if (estado.itens.isEmpty()) {
                    EstadoVazio(
                        titulo = stringResource(R.string.signatarios_vazio_titulo),
                        descricao = stringResource(R.string.selecao_vazio_descricao),
                        modifier = Modifier.weight(1f),
                        acao = {
                            Button(onClick = onCadastrarSignatario) {
                                Text(stringResource(R.string.selecao_cadastrar))
                            }
                        },
                    )
                } else {
                    Text(
                        text = stringResource(R.string.selecao_instrucao),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(estado.itens, key = { it.usuario.id }) { item ->
                            ItemSignatario(
                                item = item,
                                onAlternar = { viewModel.alternar(item.usuario.id) },
                                onDefinirPapel = { viewModel.definirPapel(item.usuario.id, it) },
                            )
                            HorizontalDivider()
                        }
                        item {
                            TextButton(
                                onClick = onCadastrarSignatario,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            ) {
                                Text(stringResource(R.string.selecao_cadastrar))
                            }
                        }
                    }
                }

                val erro = when {
                    estado.erroMinimo -> stringResource(R.string.selecao_erro_minimo)
                    estado.erroGeral != null -> stringResource(estado.erroGeral!!)
                    else -> null
                }
                if (erro != null) {
                    Text(
                        text = erro,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.selecao_total,
                            estado.totalSelecionados,
                            estado.totalSelecionados,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = onVoltar, modifier = Modifier.padding(end = 8.dp)) {
                        Text(stringResource(R.string.acao_voltar))
                    }
                    Button(onClick = viewModel::continuar, enabled = !estado.salvando) {
                        Text(
                            stringResource(
                                if (estado.salvando) R.string.acao_salvando else R.string.acao_continuar,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemSignatario(
    item: ItemSelecao,
    onAlternar: () -> Unit,
    onDefinirPapel: (PapelSignatario) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAlternar)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.papel != null, onCheckedChange = { onAlternar() })
            Column(modifier = Modifier.padding(end = 12.dp)) {
                Text(item.usuario.nome, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = item.usuario.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (item.papel != null) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 48.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PapelSignatario.entries.forEach { papel ->
                    FilterChip(
                        selected = item.papel == papel,
                        onClick = { onDefinirPapel(papel) },
                        label = { Text(stringResource(papel.rotulo)) },
                    )
                }
            }
        }
    }
}
