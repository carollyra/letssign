package br.com.letssign.ui.acompanhamento

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.model.Documento
import br.com.letssign.domain.model.EventoDetalhado
import br.com.letssign.domain.model.SignatarioDoDocumento
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.domain.model.TipoEvento
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.EtiquetaAssinatura
import br.com.letssign.ui.common.EtiquetaStatus
import br.com.letssign.ui.common.corDoSignatario
import br.com.letssign.ui.common.formatarDataHora
import br.com.letssign.ui.common.rotulo

@Composable
fun AcompanhamentoScreen(
    onVoltar: () -> Unit,
    onAbrirPdf: (Long) -> Unit,
    onEditarRascunho: (Long) -> Unit,
    viewModel: AcompanhamentoViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val novaVersao by viewModel.novaVersao.collectAsStateWithLifecycle()
    var confirmandoVersao by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.rascunhoCriado.collect(onEditarRascunho)
    }

    Scaffold(
        topBar = {
            BarraSuperior(titulo = stringResource(R.string.acompanhamento_titulo), onVoltar = onVoltar)
        },
    ) { margens ->
        when (val atual = estado) {
            EstadoAcompanhamento.Carregando -> Carregando(Modifier.padding(margens))
            EstadoAcompanhamento.NaoEncontrado -> EstadoErro(
                mensagem = stringResource(R.string.documento_erro_nao_encontrado),
                modifier = Modifier.padding(margens),
            )
            EstadoAcompanhamento.Erro -> EstadoErro(
                mensagem = stringResource(R.string.documentos_erro_carregar),
                modifier = Modifier.padding(margens),
                onTentarNovamente = viewModel::tentarNovamente,
            )
            is EstadoAcompanhamento.Dados -> {
                val documento = atual.detalhe.documento
                LazyColumn(
                    modifier = Modifier.padding(margens),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "resumo") { Resumo(documento) }
                    item(key = "acoes") {
                        Acoes(
                            documento = documento,
                            novaVersao = novaVersao,
                            onAbrirPdf = { onAbrirPdf(documento.id) },
                            onEditarRascunho = { onEditarRascunho(documento.id) },
                            onNovaVersao = { confirmandoVersao = true },
                        )
                    }
                    item(key = "signatarios") { Signatarios(atual.detalhe.signatarios) }
                    item(key = "historico") { Historico(atual.eventos) }
                }
            }
        }
    }

    if (confirmandoVersao) {
        AlertDialog(
            onDismissRequest = { confirmandoVersao = false },
            title = { Text(stringResource(R.string.acompanhamento_nova_versao)) },
            text = { Text(stringResource(R.string.acompanhamento_nova_versao_explicacao)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmandoVersao = false
                        viewModel.criarVersao()
                    },
                ) {
                    Text(stringResource(R.string.acompanhamento_nova_versao_confirmar))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoVersao = false }) {
                    Text(stringResource(R.string.acao_cancelar))
                }
            },
        )
    }
}

@Composable
private fun Resumo(documento: Documento) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = documento.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                )
                EtiquetaStatus(documento.status)
            }
            Text(
                text = stringResource(R.string.documento_arquivo_versao, documento.nomeArquivo, documento.versao),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (documento.documentoOrigemId != null) {
                Text(
                    text = stringResource(R.string.acompanhamento_derivado),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            LinhaData(R.string.acompanhamento_criado, documento.criadoEm)
            LinhaData(R.string.acompanhamento_enviado, documento.enviadoEm)
            LinhaData(R.string.acompanhamento_concluido, documento.concluidoEm)
        }
    }
}

@Composable
private fun LinhaData(rotulo: Int, instante: Long?) {
    Row {
        Text(
            text = stringResource(rotulo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = instante?.let { formatarDataHora(it) } ?: stringResource(R.string.acompanhamento_sem_data),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun Acoes(
    documento: Documento,
    novaVersao: AcaoNovaVersao,
    onAbrirPdf: () -> Unit,
    onEditarRascunho: () -> Unit,
    onNovaVersao: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (documento.status == StatusDocumento.CONCLUIDO) {
            Button(onClick = onAbrirPdf, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.acompanhamento_abrir_final))
            }
        } else {
            OutlinedButton(onClick = onAbrirPdf, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.acompanhamento_ver_documento))
            }
        }
        if (documento.editavel) {
            Button(onClick = onEditarRascunho, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.acompanhamento_continuar_rascunho))
            }
        } else {
            OutlinedButton(
                onClick = onNovaVersao,
                enabled = !novaVersao.emAndamento,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (novaVersao.emAndamento) {
                            R.string.acompanhamento_nova_versao_criando
                        } else {
                            R.string.acompanhamento_nova_versao
                        },
                    ),
                )
            }
            Text(
                text = stringResource(
                    if (novaVersao.falhou) {
                        R.string.acompanhamento_nova_versao_erro
                    } else {
                        R.string.acompanhamento_imutavel
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (novaVersao.falhou) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun Signatarios(signatarios: List<SignatarioDoDocumento>) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.signatarios_titulo), style = MaterialTheme.typography.titleMedium)
            if (signatarios.isEmpty()) {
                Text(
                    text = stringResource(R.string.acompanhamento_sem_signatarios),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            signatarios.forEachIndexed { indice, signatario ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(corDoSignatario(indice), CircleShape),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(signatario.usuario.nome, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(signatario.vinculo.papel.rotulo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        signatario.vinculo.assinadoEm?.let { assinadoEm ->
                            Text(
                                text = stringResource(R.string.documento_assinado_em, formatarDataHora(assinadoEm)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    EtiquetaAssinatura(signatario.vinculo.status)
                }
            }
        }
    }
}

@Composable
private fun Historico(eventos: List<EventoDetalhado>) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.acompanhamento_historico), style = MaterialTheme.typography.titleMedium)
            if (eventos.isEmpty()) {
                Text(
                    text = stringResource(R.string.acompanhamento_historico_vazio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            eventos.forEach { item ->
                Column {
                    Text(
                        text = when (item.evento.tipo) {
                            TipoEvento.CRIADO -> stringResource(R.string.evento_criado, item.nomeUsuario)
                            TipoEvento.ENVIADO -> stringResource(R.string.evento_enviado, item.nomeUsuario)
                            TipoEvento.ASSINADO -> stringResource(R.string.evento_assinado, item.nomeUsuario)
                            TipoEvento.CONCLUIDO -> stringResource(R.string.evento_concluido)
                            TipoEvento.NOVA_VERSAO -> stringResource(R.string.evento_nova_versao, item.nomeUsuario)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = formatarDataHora(item.evento.dataHora),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
