package br.com.letssign.ui.documento

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.model.ArquivoPdf
import br.com.letssign.ui.common.AvisoDeErro
import br.com.letssign.ui.common.BotaoDeTexto
import br.com.letssign.ui.common.CampoTexto
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.Cartao
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.Icones
import br.com.letssign.ui.pdf.EstadoPdf
import br.com.letssign.ui.pdf.PaginaPdf
import br.com.letssign.ui.pdf.rememberDocumentoPdf
import br.com.letssign.ui.theme.Espacos
import br.com.letssign.ui.theme.Movimento

private const val TIPO_PDF = "application/pdf"

@Composable
fun NovoDocumentoScreen(
    onProximaEtapa: (Long) -> Unit,
    viewModel: NovoDocumentoViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val seletor = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.aoEscolherPdf(uri.toString())
    }

    LaunchedEffect(viewModel) {
        viewModel.proximaEtapa.collect(onProximaEtapa)
    }

    when {
        estado.carregando -> Carregando()
        estado.erroCarregar != null -> EstadoErro(
            mensagem = stringResource(estado.erroCarregar!!),
            onTentarNovamente = viewModel::carregar,
        )
        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Espacos.tela, vertical = Espacos.s),
                verticalArrangement = Arrangement.spacedBy(Espacos.m),
            ) {
                CampoTexto(
                    valor = estado.titulo,
                    aoMudar = viewModel::aoMudarTitulo,
                    rotulo = stringResource(R.string.campo_titulo),
                    erro = estado.erroTitulo?.let { stringResource(it) },
                    opcoesDoTeclado = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                )

                val abrirSeletor = { seletor.launch(arrayOf(TIPO_PDF)) }
                AnimatedContent(
                    targetState = estado.arquivo,
                    transitionSpec = { fadeIn(Movimento.medio()) togetherWith fadeOut(Movimento.rapido()) },
                    label = "arquivo",
                ) { arquivo ->
                    if (arquivo == null) {
                        SemArquivo(importando = estado.importando, onSelecionar = abrirSeletor)
                    } else {
                        ArquivoEscolhido(
                            arquivo = arquivo,
                            importando = estado.importando,
                            onTrocar = abrirSeletor,
                        )
                    }
                }

                val erroArquivo = estado.erroPdf
                    ?: R.string.documento_erro_sem_arquivo.takeIf { estado.erroSemArquivo }
                    ?: estado.erroGeral
                AnimatedContent(
                    targetState = erroArquivo,
                    transitionSpec = { fadeIn(Movimento.medio()) togetherWith fadeOut(Movimento.rapido()) },
                    label = "erroDoArquivo",
                ) { erro ->
                    if (erro != null) AvisoDeErro(texto = stringResource(erro))
                }
            }
            AcoesDaEtapa(
                textoPrincipal = stringResource(
                    if (estado.salvando) R.string.acao_salvando else R.string.acao_continuar,
                ),
                onPrincipal = viewModel::continuar,
                carregando = estado.salvando,
                habilitado = !estado.importando,
            )
        }
    }
}

@Composable
private fun SemArquivo(importando: Boolean, onSelecionar: () -> Unit) {
    val forma = MaterialTheme.shapes.large
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, forma)
            .clickable(enabled = !importando, onClick = onSelecionar)
            .padding(horizontal = Espacos.xl, vertical = Espacos.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (importando) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
            } else {
                Icon(
                    imageVector = Icones.DocumentoNovo,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.height(Espacos.m))
        Text(
            text = stringResource(
                if (importando) R.string.documento_verificando_pdf else R.string.documento_selecionar_pdf,
            ),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Espacos.xxs))
        Text(
            text = stringResource(R.string.documento_nenhum_pdf),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ArquivoEscolhido(arquivo: ArquivoPdf, importando: Boolean, onTrocar: () -> Unit) {
    Cartao(modifier = Modifier.fillMaxWidth(), margemInterna = PaddingValues(Espacos.m)) {
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Espacos.s),
            ) {
                Text(
                    text = arquivo.nomeArquivo,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (importando) {
                        stringResource(R.string.documento_verificando_pdf)
                    } else {
                        pluralStringResource(
                            R.plurals.documento_paginas,
                            arquivo.totalPaginas,
                            arquivo.totalPaginas,
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (importando) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = Espacos.s)
                        .size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                BotaoDeTexto(texto = stringResource(R.string.documento_trocar_pdf), onClick = onTrocar)
            }
        }
        Spacer(Modifier.height(Espacos.m))
        PreviaPrimeiraPagina(arquivo.caminho)
    }
}

@Composable
private fun PreviaPrimeiraPagina(caminho: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(Espacos.s),
        contentAlignment = Alignment.Center,
    ) {
        when (val pdf = rememberDocumentoPdf(caminho)) {
            EstadoPdf.Carregando -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
            EstadoPdf.Erro -> Text(
                text = stringResource(R.string.pdf_erro_abrir),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            is EstadoPdf.Pronto -> PaginaPdf(documento = pdf.documento, indice = 0)
        }
    }
}
