package br.com.letssign.ui.documento

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.model.ArquivoPdf
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.pdf.EstadoPdf
import br.com.letssign.ui.pdf.PaginaPdf
import br.com.letssign.ui.pdf.rememberDocumentoPdf

private const val TIPO_PDF = "application/pdf"

@Composable
fun NovoDocumentoScreen(
    onVoltar: () -> Unit,
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

    Scaffold(
        topBar = {
            BarraSuperior(titulo = stringResource(R.string.documento_novo_titulo), onVoltar = onVoltar)
        },
    ) { margens ->
        when {
            estado.carregando -> Carregando(Modifier.padding(margens))
            estado.erroCarregar != null -> EstadoErro(
                mensagem = stringResource(estado.erroCarregar!!),
                modifier = Modifier.padding(margens),
                onTentarNovamente = viewModel::carregar,
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(margens)
                    .imePadding(),
            ) {
                IndicadorEtapa(EtapaDocumento.ARQUIVO)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = estado.titulo,
                        onValueChange = viewModel::aoMudarTitulo,
                        label = { Text(stringResource(R.string.campo_titulo)) },
                        singleLine = true,
                        isError = estado.erroTitulo != null,
                        supportingText = estado.erroTitulo?.let { { Text(stringResource(it)) } },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    ArquivoSelecionado(
                        arquivo = estado.arquivo,
                        importando = estado.importando,
                        onSelecionar = { seletor.launch(arrayOf(TIPO_PDF)) },
                    )

                    val erroArquivo = estado.erroPdf
                        ?: R.string.documento_erro_sem_arquivo.takeIf { estado.erroSemArquivo }
                    if (erroArquivo != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(erroArquivo),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }

                    estado.erroGeral?.let { erro ->
                        Text(
                            text = stringResource(erro),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Button(
                    onClick = viewModel::continuar,
                    enabled = !estado.salvando && !estado.importando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
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

@Composable
private fun ArquivoSelecionado(
    arquivo: ArquivoPdf?,
    importando: Boolean,
    onSelecionar: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (arquivo == null) {
                Text(
                    text = stringResource(R.string.documento_nenhum_pdf),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = arquivo.nomeArquivo,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.documento_paginas,
                        arquivo.totalPaginas,
                        arquivo.totalPaginas,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PreviaPrimeiraPagina(arquivo.caminho)
            }

            if (importando) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.documento_verificando_pdf))
                }
            } else {
                OutlinedButton(onClick = onSelecionar) {
                    Text(
                        stringResource(
                            if (arquivo == null) R.string.documento_selecionar_pdf else R.string.documento_trocar_pdf,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviaPrimeiraPagina(caminho: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (val pdf = rememberDocumentoPdf(caminho)) {
            EstadoPdf.Carregando -> CircularProgressIndicator()
            EstadoPdf.Erro -> Text(
                text = stringResource(R.string.pdf_erro_abrir),
                color = MaterialTheme.colorScheme.error,
            )
            is EstadoPdf.Pronto -> PaginaPdf(documento = pdf.documento, indice = 0)
        }
    }
}
