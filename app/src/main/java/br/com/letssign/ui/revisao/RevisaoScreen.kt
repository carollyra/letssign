package br.com.letssign.ui.revisao

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.model.DocumentoDetalhado
import br.com.letssign.domain.model.SignatarioDoDocumento
import br.com.letssign.domain.regra.ErroEnvio
import br.com.letssign.domain.regra.ProblemaEnvio
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.corDoSignatario
import br.com.letssign.ui.common.rotulo
import br.com.letssign.ui.documento.EtapaDocumento
import br.com.letssign.ui.documento.IndicadorEtapa
import br.com.letssign.ui.pdf.CampoVisual
import br.com.letssign.ui.pdf.CamposSobreAPagina
import br.com.letssign.ui.pdf.EstadoPdf
import br.com.letssign.ui.pdf.PaginaPdf
import br.com.letssign.ui.pdf.rememberDocumentoPdf

@Composable
fun RevisaoScreen(
    onVoltar: () -> Unit,
    onConcluir: () -> Unit,
    onAcompanhar: (Long) -> Unit,
    viewModel: RevisaoViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensagemPadrao = stringResource(R.string.revisao_mensagem_padrao)

    LaunchedEffect(mensagemPadrao) {
        viewModel.definirMensagemPadrao(mensagemPadrao)
    }
    // Depois do envio o documento é imutável: voltar levaria a telas de edição que não servem mais.
    BackHandler(enabled = estado.enviado, onBack = onConcluir)

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = stringResource(R.string.revisao_titulo),
                onVoltar = if (estado.enviado) onConcluir else onVoltar,
            )
        },
    ) { margens ->
        val detalhe = estado.detalhe
        when {
            estado.carregando -> Carregando(Modifier.padding(margens))
            estado.erroCarregar || detalhe == null -> EstadoErro(
                mensagem = stringResource(R.string.documento_erro_nao_encontrado),
                modifier = Modifier.padding(margens),
                onTentarNovamente = viewModel::carregar,
            )
            estado.enviado -> ResultadoDoEnvio(
                detalhe = detalhe,
                onConcluir = onConcluir,
                onAcompanhar = { onAcompanhar(detalhe.documento.id) },
                modifier = Modifier.padding(margens),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(margens)
                    .imePadding(),
            ) {
                IndicadorEtapa(EtapaDocumento.REVISAO)
                ConteudoDaRevisao(
                    estado = estado,
                    detalhe = detalhe,
                    onMudarMensagem = viewModel::aoMudarMensagem,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    OutlinedButton(onClick = onVoltar, enabled = !estado.enviando) {
                        Text(stringResource(R.string.acao_voltar))
                    }
                    Button(onClick = viewModel::enviar, enabled = !estado.enviando) {
                        Text(
                            stringResource(
                                when {
                                    estado.enviando -> R.string.revisao_enviando
                                    estado.falha != null -> R.string.acao_tentar_novamente
                                    else -> R.string.revisao_enviar
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConteudoDaRevisao(
    estado: RevisaoUiState,
    detalhe: DocumentoDetalhado,
    onMudarMensagem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pdf = rememberDocumentoPdf(detalhe.documento.caminhoOriginal)
    val camposPorPagina = remember(detalhe) {
        detalhe.signatarios.flatMapIndexed { indice, signatario ->
            signatario.campos.map { campo ->
                campo.pagina to CampoVisual(campo.area, corDoSignatario(indice), signatario.usuario.nome)
            }
        }.groupBy({ it.first }, { it.second })
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (estado.falha != null || estado.problemas.isNotEmpty()) {
            item(key = "avisos") { Avisos(estado) }
        }
        item(key = "documento") {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(detalhe.documento.titulo, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = stringResource(
                            R.string.documento_arquivo_versao,
                            detalhe.documento.nomeArquivo,
                            detalhe.documento.versao,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.documento_paginas,
                            detalhe.documento.totalPaginas,
                            detalhe.documento.totalPaginas,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item(key = "signatarios") {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.signatarios_titulo),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    detalhe.signatarios.forEachIndexed { indice, signatario ->
                        LinhaSignatario(signatario = signatario, indiceCor = indice)
                    }
                }
            }
        }
        item(key = "mensagem") {
            OutlinedTextField(
                value = estado.mensagem,
                onValueChange = onMudarMensagem,
                label = { Text(stringResource(R.string.revisao_mensagem)) },
                isError = estado.problemas.any { it.erro == ErroEnvio.MENSAGEM_VAZIA },
                minLines = 3,
                maxLines = 6,
                enabled = !estado.enviando,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when (pdf) {
            EstadoPdf.Carregando -> item(key = "pdf-carregando") {
                Carregando(Modifier.height(160.dp))
            }
            EstadoPdf.Erro -> item(key = "pdf-erro") {
                Text(
                    text = stringResource(R.string.pdf_erro_abrir),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            is EstadoPdf.Pronto -> items(count = pdf.documento.totalPaginas, key = { "pagina-$it" }) { indice ->
                val campos = camposPorPagina[indice].orEmpty()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.campos_pagina, indice + 1, pdf.documento.totalPaginas) +
                            " · " + pluralStringResource(R.plurals.campos_total, campos.size, campos.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        PaginaPdf(documento = pdf.documento, indice = indice) { tamanho ->
                            CamposSobreAPagina(campos = campos, tamanho = tamanho)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaSignatario(signatario: SignatarioDoDocumento, indiceCor: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .background(corDoSignatario(indiceCor), CircleShape),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(signatario.usuario.nome, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(signatario.vinculo.papel.rotulo) + " · " + signatario.usuario.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val paginas = signatario.campos.map { it.pagina + 1 }.distinct().sorted()
            Text(
                text = if (paginas.isEmpty()) {
                    stringResource(R.string.revisao_sem_campos)
                } else {
                    pluralStringResource(
                        R.plurals.revisao_campos_nas_paginas,
                        signatario.campos.size,
                        signatario.campos.size,
                        paginas.joinToString(", "),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (paginas.isEmpty()) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun Avisos(estado: RevisaoUiState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when (estado.falha) {
                FalhaRevisao.ENVIO -> {
                    Text(
                        stringResource(R.string.revisao_falha_titulo),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        stringResource(R.string.revisao_falha_envio),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                FalhaRevisao.INESPERADA -> Text(
                    stringResource(R.string.erro_inesperado),
                    style = MaterialTheme.typography.bodyMedium,
                )
                null -> Unit
            }
            if (estado.problemas.isNotEmpty()) {
                Text(
                    stringResource(R.string.revisao_problemas_titulo),
                    style = MaterialTheme.typography.titleSmall,
                )
                estado.problemas.forEach { problema ->
                    Text("• " + problema.descricao(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ProblemaEnvio.descricao(): String {
    val nomes = signatarios.joinToString(", ")
    return when (erro) {
        ErroEnvio.TITULO_VAZIO -> stringResource(R.string.envio_erro_titulo)
        ErroEnvio.SEM_ARQUIVO -> stringResource(R.string.envio_erro_arquivo)
        ErroEnvio.MENSAGEM_VAZIA -> stringResource(R.string.envio_erro_mensagem)
        ErroEnvio.SEM_SIGNATARIOS -> stringResource(R.string.envio_erro_sem_signatarios)
        ErroEnvio.SIGNATARIO_SEM_CAMPO -> stringResource(R.string.envio_erro_sem_campo, nomes)
        ErroEnvio.CAMPO_SEM_SIGNATARIO -> stringResource(R.string.envio_erro_campo_sem_dono, nomes)
        ErroEnvio.CAMPO_FORA_DA_PAGINA -> stringResource(R.string.envio_erro_campo_fora, nomes)
        ErroEnvio.CAMPO_EM_PAGINA_INEXISTENTE -> stringResource(R.string.envio_erro_pagina, nomes)
    }
}

@Composable
private fun ResultadoDoEnvio(
    detalhe: DocumentoDetalhado,
    onConcluir: () -> Unit,
    onAcompanhar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.revisao_enviado_titulo),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = pluralStringResource(
                R.plurals.revisao_enviado_descricao,
                detalhe.signatarios.size,
                detalhe.documento.titulo,
                detalhe.signatarios.size,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAcompanhar) { Text(stringResource(R.string.revisao_acompanhar)) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onConcluir) { Text(stringResource(R.string.revisao_voltar_inicio)) }
    }
}
