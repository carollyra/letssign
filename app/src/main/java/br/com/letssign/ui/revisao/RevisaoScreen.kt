package br.com.letssign.ui.revisao

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.model.DocumentoDetalhado
import br.com.letssign.domain.model.SignatarioDoDocumento
import br.com.letssign.domain.regra.ErroEnvio
import br.com.letssign.domain.regra.ProblemaEnvio
import br.com.letssign.ui.common.AvatarDoSignatario
import br.com.letssign.ui.common.AvisoDeErro
import br.com.letssign.ui.common.BotaoDeTexto
import br.com.letssign.ui.common.BotaoPrincipal
import br.com.letssign.ui.common.CampoTexto
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.Cartao
import br.com.letssign.ui.common.CheckAnimado
import br.com.letssign.ui.common.Divisor
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.Icones
import br.com.letssign.ui.common.TituloDeSecao
import br.com.letssign.ui.common.corDoSignatario
import br.com.letssign.ui.common.rotulo
import br.com.letssign.ui.documento.AcoesDaEtapa
import br.com.letssign.ui.pdf.CampoVisual
import br.com.letssign.ui.pdf.CamposSobreAPagina
import br.com.letssign.ui.pdf.EstadoPdf
import br.com.letssign.ui.pdf.PaginaPdf
import br.com.letssign.ui.pdf.rememberDocumentoPdf
import br.com.letssign.ui.theme.Espacos
import br.com.letssign.ui.theme.Movimento

@Composable
fun RevisaoScreen(
    onVoltar: () -> Unit,
    onConcluir: () -> Unit,
    onAcompanhar: (Long) -> Unit,
    onEnviado: (Boolean) -> Unit,
    viewModel: RevisaoViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensagemPadrao = stringResource(R.string.revisao_mensagem_padrao)

    LaunchedEffect(mensagemPadrao) {
        viewModel.definirMensagemPadrao(mensagemPadrao)
    }
    LaunchedEffect(estado.enviado) {
        onEnviado(estado.enviado)
    }
    // Depois do envio o documento é imutável: voltar levaria a telas de edição que não servem mais.
    BackHandler(enabled = estado.enviado, onBack = onConcluir)

    val detalhe = estado.detalhe
    when {
        estado.carregando -> Carregando()
        estado.erroCarregar || detalhe == null -> EstadoErro(
            mensagem = stringResource(R.string.documento_erro_nao_encontrado),
            onTentarNovamente = viewModel::carregar,
        )
        else -> AnimatedContent(
            targetState = estado.enviado,
            transitionSpec = {
                (fadeIn(Movimento.lento()) + scaleIn(Movimento.lento(), initialScale = 0.96f))
                    .togetherWith(fadeOut(Movimento.rapido()))
            },
            label = "envio",
        ) { enviado ->
            if (enviado) {
                ResultadoDoEnvio(
                    detalhe = detalhe,
                    onConcluir = onConcluir,
                    onAcompanhar = { onAcompanhar(detalhe.documento.id) },
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                ) {
                    ConteudoDaRevisao(
                        estado = estado,
                        detalhe = detalhe,
                        onMudarMensagem = viewModel::aoMudarMensagem,
                        modifier = Modifier.weight(1f),
                    )
                    AcoesDaEtapa(
                        textoPrincipal = stringResource(
                            when {
                                estado.enviando -> R.string.revisao_enviando
                                estado.falha != null -> R.string.acao_tentar_novamente
                                else -> R.string.revisao_enviar
                            },
                        ),
                        onPrincipal = viewModel::enviar,
                        carregando = estado.enviando,
                        iconePrincipal = Icones.Enviar.takeIf { estado.falha == null },
                        onVoltar = onVoltar,
                    )
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
        contentPadding = PaddingValues(horizontal = Espacos.tela, vertical = Espacos.xs),
        verticalArrangement = Arrangement.spacedBy(Espacos.s),
    ) {
        if (estado.falha != null || estado.problemas.isNotEmpty()) {
            item(key = "avisos") { Avisos(estado = estado, modifier = Modifier.animateItem()) }
        }
        item(key = "documento") { ResumoDoDocumento(detalhe) }
        item(key = "signatarios-titulo") {
            TituloDeSecao(
                texto = stringResource(R.string.signatarios_titulo),
                modifier = Modifier.padding(top = Espacos.xs),
            )
        }
        item(key = "signatarios") {
            Cartao(modifier = Modifier.fillMaxWidth(), margemInterna = PaddingValues(horizontal = Espacos.m)) {
                detalhe.signatarios.forEachIndexed { indice, signatario ->
                    if (indice > 0) Divisor()
                    LinhaSignatario(signatario = signatario, indiceCor = indice)
                }
            }
        }
        item(key = "mensagem-titulo") {
            TituloDeSecao(
                texto = stringResource(R.string.revisao_mensagem),
                modifier = Modifier.padding(top = Espacos.xs),
            )
        }
        item(key = "mensagem") {
            CampoTexto(
                valor = estado.mensagem,
                aoMudar = onMudarMensagem,
                rotulo = stringResource(R.string.revisao_mensagem_rotulo),
                marcarErro = estado.problemas.any { it.erro == ErroEnvio.MENSAGEM_VAZIA },
                umaLinha = false,
                minimoDeLinhas = 3,
                maximoDeLinhas = 6,
                habilitado = !estado.enviando,
            )
        }
        when (pdf) {
            EstadoPdf.Carregando -> item(key = "pdf-carregando") {
                Carregando(Modifier.height(160.dp))
            }
            EstadoPdf.Erro -> item(key = "pdf-erro") {
                AvisoDeErro(texto = stringResource(R.string.pdf_erro_abrir))
            }
            is EstadoPdf.Pronto -> items(count = pdf.documento.totalPaginas, key = { "pagina-$it" }) { indice ->
                val campos = camposPorPagina[indice].orEmpty()
                Column(
                    modifier = Modifier.padding(top = Espacos.xs),
                    verticalArrangement = Arrangement.spacedBy(Espacos.xs),
                ) {
                    TituloDeSecao(
                        texto = stringResource(R.string.campos_pagina, indice + 1, pdf.documento.totalPaginas) +
                            " · " + pluralStringResource(R.plurals.campos_total, campos.size, campos.size),
                    )
                    PaginaPdf(documento = pdf.documento, indice = indice) { tamanho ->
                        CamposSobreAPagina(campos = campos, tamanho = tamanho)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumoDoDocumento(detalhe: DocumentoDetalhado) {
    Cartao(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icones.Documento,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.padding(start = Espacos.s)) {
                Text(
                    text = detalhe.documento.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.documento_arquivo_versao,
                        detalhe.documento.nomeArquivo,
                        detalhe.documento.versao,
                    ) + " · " + pluralStringResource(
                        R.plurals.documento_paginas,
                        detalhe.documento.totalPaginas,
                        detalhe.documento.totalPaginas,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LinhaSignatario(signatario: SignatarioDoDocumento, indiceCor: Int) {
    Row(
        modifier = Modifier.padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarDoSignatario(nome = signatario.usuario.nome, cor = corDoSignatario(indiceCor))
        Column(modifier = Modifier.padding(start = Espacos.s)) {
            Text(
                text = signatario.usuario.nome,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(signatario.vinculo.papel.rotulo) + " · " + signatario.usuario.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
private fun Avisos(estado: RevisaoUiState, modifier: Modifier = Modifier) {
    val corDoTexto = MaterialTheme.colorScheme.onErrorContainer
    AvisoDeErro(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Espacos.xxs)) {
            when (estado.falha) {
                FalhaRevisao.ENVIO -> {
                    Text(
                        text = stringResource(R.string.revisao_falha_titulo),
                        style = MaterialTheme.typography.titleSmall,
                        color = corDoTexto,
                    )
                    Text(
                        text = stringResource(R.string.revisao_falha_envio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = corDoTexto,
                    )
                }
                FalhaRevisao.INESPERADA -> Text(
                    text = stringResource(R.string.erro_inesperado),
                    style = MaterialTheme.typography.bodyMedium,
                    color = corDoTexto,
                )
                null -> Unit
            }
            if (estado.problemas.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.revisao_problemas_titulo),
                    style = MaterialTheme.typography.titleSmall,
                    color = corDoTexto,
                )
                estado.problemas.forEach { problema ->
                    Text(
                        text = "• " + problema.descricao(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = corDoTexto,
                    )
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
) {
    val haptico = LocalHapticFeedback.current
    var vibrou by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!vibrou) {
            vibrou = true
            haptico.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Espacos.tela, vertical = Espacos.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        CheckAnimado()
        Spacer(Modifier.height(Espacos.xl))
        Text(
            text = stringResource(R.string.revisao_enviado_titulo),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Espacos.xs))
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
            modifier = Modifier.padding(horizontal = Espacos.m),
        )
        Spacer(Modifier.weight(1.3f))
        BotaoPrincipal(
            texto = stringResource(R.string.revisao_acompanhar),
            onClick = onAcompanhar,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Espacos.xxs))
        BotaoDeTexto(
            texto = stringResource(R.string.revisao_voltar_inicio),
            onClick = onConcluir,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
