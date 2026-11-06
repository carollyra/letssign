package br.com.letssign.ui.acompanhamento

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.model.Documento
import br.com.letssign.domain.model.EventoDetalhado
import br.com.letssign.domain.model.SignatarioDoDocumento
import br.com.letssign.domain.model.StatusAssinatura
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.domain.model.TipoEvento
import br.com.letssign.ui.common.AvatarDoSignatario
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.BotaoDeTexto
import br.com.letssign.ui.common.BotaoPrincipal
import br.com.letssign.ui.common.BotaoSecundario
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.Cartao
import br.com.letssign.ui.common.ChipAssinatura
import br.com.letssign.ui.common.ChipStatus
import br.com.letssign.ui.common.Divisor
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.Icones
import br.com.letssign.ui.common.TituloDeSecao
import br.com.letssign.ui.common.corDoSignatario
import br.com.letssign.ui.common.entradaSuave
import br.com.letssign.ui.common.formatarDataHora
import br.com.letssign.ui.common.progressoDoItem
import br.com.letssign.ui.common.rememberEntradaDeLista
import br.com.letssign.ui.common.rotulo
import br.com.letssign.ui.common.tom
import br.com.letssign.ui.theme.Espacos
import br.com.letssign.ui.theme.Movimento

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
                val entrada = rememberEntradaDeLista()
                LazyColumn(
                    modifier = Modifier.padding(margens),
                    contentPadding = PaddingValues(
                        start = Espacos.tela,
                        end = Espacos.tela,
                        top = Espacos.xs,
                        bottom = Espacos.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Espacos.s),
                ) {
                    item(key = "resumo") {
                        Resumo(
                            documento = documento,
                            signatarios = atual.detalhe.signatarios,
                            modifier = Modifier.entradaSuave(entrada.progressoDoItem(0)),
                        )
                    }
                    item(key = "acoes") {
                        Acoes(
                            documento = documento,
                            novaVersao = novaVersao,
                            onAbrirPdf = { onAbrirPdf(documento.id) },
                            onEditarRascunho = { onEditarRascunho(documento.id) },
                            onNovaVersao = { confirmandoVersao = true },
                            modifier = Modifier.entradaSuave(entrada.progressoDoItem(1)),
                        )
                    }
                    item(key = "signatarios") {
                        Signatarios(
                            signatarios = atual.detalhe.signatarios,
                            modifier = Modifier.entradaSuave(entrada.progressoDoItem(2)),
                        )
                    }
                    item(key = "datas") {
                        Datas(documento = documento, modifier = Modifier.entradaSuave(entrada.progressoDoItem(3)))
                    }
                    item(key = "historico") {
                        Historico(
                            eventos = atual.eventos,
                            modifier = Modifier.entradaSuave(entrada.progressoDoItem(4)),
                        )
                    }
                }
            }
        }
    }

    if (confirmandoVersao) {
        AlertDialog(
            onDismissRequest = { confirmandoVersao = false },
            title = {
                Text(
                    text = stringResource(R.string.acompanhamento_nova_versao),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.acompanhamento_nova_versao_explicacao),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                BotaoDeTexto(
                    texto = stringResource(R.string.acompanhamento_nova_versao_confirmar),
                    onClick = {
                        confirmandoVersao = false
                        viewModel.criarVersao()
                    },
                )
            },
            dismissButton = {
                BotaoDeTexto(
                    texto = stringResource(R.string.acao_cancelar),
                    onClick = { confirmandoVersao = false },
                    cor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            shape = MaterialTheme.shapes.extraLarge,
        )
    }
}

@Composable
private fun Resumo(
    documento: Documento,
    signatarios: List<SignatarioDoDocumento>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = Espacos.xs)) {
        ChipStatus(documento.status)
        Spacer(Modifier.height(Espacos.s))
        Text(
            text = documento.titulo,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Espacos.xxs))
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
        if (documento.status != StatusDocumento.RASCUNHO && signatarios.isNotEmpty()) {
            Spacer(Modifier.height(Espacos.m))
            ProgressoDasAssinaturas(documento = documento, signatarios = signatarios)
        }
    }
}

@Composable
private fun ProgressoDasAssinaturas(documento: Documento, signatarios: List<SignatarioDoDocumento>) {
    val assinaram = signatarios.count { it.vinculo.status == StatusAssinatura.ASSINADO }
    val fracao by animateFloatAsState(
        targetValue = assinaram / signatarios.size.toFloat(),
        animationSpec = Movimento.lento(),
        label = "progressoDasAssinaturas",
    )
    Column(verticalArrangement = Arrangement.spacedBy(Espacos.xs)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fracao)
                    .background(documento.status.tom.conteudo, CircleShape),
            )
        }
        Text(
            text = pluralStringResource(
                R.plurals.acompanhamento_assinaturas,
                signatarios.size,
                assinaram,
                signatarios.size,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Espacos.xs)) {
        when {
            documento.editavel -> {
                BotaoPrincipal(
                    texto = stringResource(R.string.acompanhamento_continuar_rascunho),
                    onClick = onEditarRascunho,
                    icone = Icones.Editar,
                    modifier = Modifier.fillMaxWidth(),
                )
                BotaoSecundario(
                    texto = stringResource(R.string.acompanhamento_ver_documento),
                    onClick = onAbrirPdf,
                    icone = Icones.Olho,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            documento.status == StatusDocumento.CONCLUIDO -> BotaoPrincipal(
                texto = stringResource(R.string.acompanhamento_abrir_final),
                onClick = onAbrirPdf,
                icone = Icones.Documento,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> BotaoSecundario(
                texto = stringResource(R.string.acompanhamento_ver_documento),
                onClick = onAbrirPdf,
                icone = Icones.Olho,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!documento.editavel) {
            BotaoDeTexto(
                texto = stringResource(
                    if (novaVersao.emAndamento) {
                        R.string.acompanhamento_nova_versao_criando
                    } else {
                        R.string.acompanhamento_nova_versao
                    },
                ),
                onClick = onNovaVersao,
                habilitado = !novaVersao.emAndamento,
                icone = Icones.Copia,
                modifier = Modifier.fillMaxWidth(),
            )
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
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun Signatarios(signatarios: List<SignatarioDoDocumento>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Espacos.xs)) {
        TituloDeSecao(
            texto = stringResource(R.string.signatarios_titulo),
            modifier = Modifier.padding(top = Espacos.xs),
        )
        Cartao(modifier = Modifier.fillMaxWidth(), margemInterna = PaddingValues(horizontal = Espacos.m)) {
            if (signatarios.isEmpty()) {
                Text(
                    text = stringResource(R.string.acompanhamento_sem_signatarios),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Espacos.m),
                )
            }
            signatarios.forEachIndexed { indice, signatario ->
                if (indice > 0) Divisor()
                Row(
                    modifier = Modifier.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarDoSignatario(nome = signatario.usuario.nome, cor = corDoSignatario(indice))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = Espacos.s),
                    ) {
                        Text(
                            text = signatario.usuario.nome,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(signatario.vinculo.papel.rotulo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        signatario.vinculo.assinadoEm?.let { assinadoEm ->
                            Text(
                                text = formatarDataHora(assinadoEm),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    ChipAssinatura(signatario.vinculo.status)
                }
            }
        }
    }
}

@Composable
private fun Datas(documento: Documento, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Espacos.xs)) {
        TituloDeSecao(
            texto = stringResource(R.string.acompanhamento_datas),
            modifier = Modifier.padding(top = Espacos.xs),
        )
        Cartao(modifier = Modifier.fillMaxWidth(), margemInterna = PaddingValues(horizontal = Espacos.m)) {
            LinhaData(R.string.acompanhamento_criado, documento.criadoEm)
            Divisor()
            LinhaData(R.string.acompanhamento_enviado, documento.enviadoEm)
            Divisor()
            LinhaData(R.string.acompanhamento_concluido, documento.concluidoEm)
        }
    }
}

@Composable
private fun LinhaData(@StringRes rotulo: Int, instante: Long?) {
    Row(modifier = Modifier.padding(vertical = 14.dp)) {
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
private fun Historico(eventos: List<EventoDetalhado>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Espacos.xs)) {
        TituloDeSecao(
            texto = stringResource(R.string.acompanhamento_historico),
            modifier = Modifier.padding(top = Espacos.xs),
        )
        Cartao(modifier = Modifier.fillMaxWidth()) {
            if (eventos.isEmpty()) {
                Text(
                    text = stringResource(R.string.acompanhamento_historico_vazio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            eventos.forEachIndexed { indice, item ->
                LinhaDoHistorico(item = item, ultimo = indice == eventos.lastIndex)
            }
        }
    }
}

@Composable
private fun LinhaDoHistorico(item: EventoDetalhado, ultimo: Boolean) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.evento.tipo.icone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (!ultimo) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .width(1.5.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Column(
            modifier = Modifier.padding(
                start = Espacos.s,
                top = 5.dp,
                bottom = if (ultimo) 0.dp else Espacos.m,
            ),
        ) {
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

private val TipoEvento.icone: ImageVector
    get() = when (this) {
        TipoEvento.CRIADO -> Icones.Documento
        TipoEvento.ENVIADO -> Icones.Enviar
        TipoEvento.ASSINADO -> Icones.Assinar
        TipoEvento.CONCLUIDO -> Icones.Check
        TipoEvento.NOVA_VERSAO -> Icones.Copia
    }
