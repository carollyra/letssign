package br.com.letssign.ui.assinatura

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.model.DocumentoParaAssinar
import br.com.letssign.domain.model.StatusAssinatura
import br.com.letssign.ui.common.AvisoDeErro
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.BotaoDeTexto
import br.com.letssign.ui.common.BotaoPrincipal
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.Cartao
import br.com.letssign.ui.common.CheckAnimado
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.Icones
import br.com.letssign.ui.common.TituloDeSecao
import br.com.letssign.ui.common.corDoSignatario
import br.com.letssign.ui.common.formatarDataHora
import br.com.letssign.ui.common.rotulo
import br.com.letssign.ui.pdf.CampoVisual
import br.com.letssign.ui.pdf.CamposSobreAPagina
import br.com.letssign.ui.pdf.EstadoPdf
import br.com.letssign.ui.pdf.PaginaPdf
import br.com.letssign.ui.pdf.rememberDocumentoPdf
import br.com.letssign.ui.theme.Espacos
import br.com.letssign.ui.theme.Movimento
import br.com.letssign.ui.theme.TemaLetsSign

@Composable
fun AssinaturaScreen(
    onVoltar: () -> Unit,
    viewModel: AssinaturaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dados = estado.dados
    // A confirmação aparece uma vez, logo depois de assinar; dispensada, fica o documento assinado.
    var confirmacaoVista by rememberSaveable { mutableStateOf(false) }
    val mostrarConfirmacao = estado.acabouDeAssinar && !confirmacaoVista

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = dados?.documento?.titulo ?: stringResource(R.string.assinatura_titulo),
                onVoltar = onVoltar,
                tituloVisivel = !mostrarConfirmacao,
            )
        },
    ) { margens ->
        when {
            estado.carregando -> Carregando(Modifier.padding(margens))
            estado.erroCarregar || dados == null -> EstadoErro(
                mensagem = stringResource(R.string.assinatura_erro_carregar),
                modifier = Modifier.padding(margens),
                onTentarNovamente = viewModel::carregar,
            )
            else -> AnimatedContent(
                targetState = mostrarConfirmacao,
                transitionSpec = {
                    (fadeIn(Movimento.lento()) + scaleIn(Movimento.lento(), initialScale = 0.96f))
                        .togetherWith(fadeOut(Movimento.rapido()))
                },
                modifier = Modifier.padding(margens),
                label = "assinatura",
            ) { confirmando ->
                if (confirmando) {
                    AssinaturaRegistrada(
                        onVerDocumento = { confirmacaoVista = true },
                        onVoltarAoInicio = onVoltar,
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        PaginasDoDocumento(dados = dados, modifier = Modifier.weight(1f))

                        estado.erro?.let { erro ->
                            AvisoDeErro(
                                texto = stringResource(erro),
                                modifier = Modifier.padding(horizontal = Espacos.tela, vertical = Espacos.xxs),
                            )
                        }
                        if (estado.podeAssinar) {
                            BotaoPrincipal(
                                texto = stringResource(
                                    if (estado.assinando) {
                                        R.string.assinatura_gravando
                                    } else {
                                        R.string.assinatura_assinar
                                    },
                                ),
                                onClick = viewModel::abrirCaptura,
                                carregando = estado.assinando,
                                icone = Icones.Assinar,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = Espacos.tela,
                                        end = Espacos.tela,
                                        top = Espacos.s,
                                        bottom = Espacos.m,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }

    if (estado.capturando) {
        CapturaAssinaturaDialog(
            onCancelar = viewModel::cancelarCaptura,
            onConfirmar = viewModel::confirmarAssinatura,
        )
    }
}

@Composable
private fun PaginasDoDocumento(dados: DocumentoParaAssinar, modifier: Modifier = Modifier) {
    val pdf = rememberDocumentoPdf(dados.documento.caminhoAtual)
    val rotuloCampo = stringResource(R.string.assinatura_assine_aqui)
    // Depois de assinar, a própria assinatura já está no PDF e o destaque só atrapalharia.
    val camposPorPagina = remember(dados, rotuloCampo) {
        if (dados.vinculo.status == StatusAssinatura.ASSINADO) {
            emptyMap()
        } else {
            dados.campos.groupBy(
                keySelector = { it.pagina },
                valueTransform = { CampoVisual(it.area, corDoSignatario(0), rotuloCampo) },
            )
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = Espacos.tela,
            end = Espacos.tela,
            top = Espacos.xs,
            bottom = Espacos.l,
        ),
        verticalArrangement = Arrangement.spacedBy(Espacos.l),
    ) {
        item(key = "resumo") { Resumo(dados = dados) }
        when (pdf) {
            EstadoPdf.Carregando -> item(key = "pdf-carregando") { Carregando(Modifier.height(200.dp)) }
            EstadoPdf.Erro -> item(key = "pdf-erro") {
                AvisoDeErro(texto = stringResource(R.string.pdf_erro_abrir))
            }
            is EstadoPdf.Pronto -> items(count = pdf.documento.totalPaginas, key = { "pagina-$it" }) { indice ->
                val campos = camposPorPagina[indice].orEmpty()
                Column(verticalArrangement = Arrangement.spacedBy(Espacos.xs)) {
                    TituloDeSecao(
                        texto = stringResource(R.string.campos_pagina, indice + 1, pdf.documento.totalPaginas),
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
private fun Resumo(dados: DocumentoParaAssinar) {
    val assinadoEm = dados.vinculo.assinadoEm
    Cartao(modifier = Modifier.fillMaxWidth()) {
        if (assinadoEm != null) {
            val tom = TemaLetsSign.cores.concluido
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(tom.fundo, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icones.Check,
                        contentDescription = null,
                        tint = tom.conteudo,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.padding(start = Espacos.s)) {
                    Text(
                        text = stringResource(R.string.assinatura_ja_assinado),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.documento_assinado_em, formatarDataHora(assinadoEm)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            if (dados.documento.mensagemConvite.isNotBlank()) {
                Text(text = dados.documento.mensagemConvite, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(Espacos.xs))
            }
            val paginas = dados.campos.map { it.pagina + 1 }.distinct().sorted().joinToString(", ")
            Text(
                text = stringResource(
                    R.string.home_signatario_papel,
                    stringResource(dados.vinculo.papel.rotulo),
                ) + " · " + pluralStringResource(
                    R.plurals.revisao_campos_nas_paginas,
                    dados.campos.size,
                    dados.campos.size,
                    paginas,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AssinaturaRegistrada(onVerDocumento: () -> Unit, onVoltarAoInicio: () -> Unit) {
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
            text = stringResource(R.string.assinatura_sucesso_titulo),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Espacos.xs))
        Text(
            text = stringResource(R.string.assinatura_sucesso_descricao),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Espacos.m),
        )
        Spacer(Modifier.weight(1.3f))
        BotaoPrincipal(
            texto = stringResource(R.string.assinatura_ver_documento),
            onClick = onVerDocumento,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Espacos.xxs))
        BotaoDeTexto(
            texto = stringResource(R.string.revisao_voltar_inicio),
            onClick = onVoltarAoInicio,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
