package br.com.letssign.ui.assinatura

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.model.DocumentoParaAssinar
import br.com.letssign.domain.model.StatusAssinatura
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.corDoSignatario
import br.com.letssign.ui.common.formatarDataHora
import br.com.letssign.ui.common.rotulo
import br.com.letssign.ui.pdf.CampoVisual
import br.com.letssign.ui.pdf.CamposSobreAPagina
import br.com.letssign.ui.pdf.EstadoPdf
import br.com.letssign.ui.pdf.PaginaPdf
import br.com.letssign.ui.pdf.rememberDocumentoPdf

@Composable
fun AssinaturaScreen(
    onVoltar: () -> Unit,
    viewModel: AssinaturaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dados = estado.dados

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = dados?.documento?.titulo ?: stringResource(R.string.assinatura_titulo),
                onVoltar = onVoltar,
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
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(margens),
            ) {
                PaginasDoDocumento(estado = estado, dados = dados, modifier = Modifier.weight(1f))

                estado.erro?.let { erro ->
                    Text(
                        text = stringResource(erro),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                if (estado.podeAssinar) {
                    Button(
                        onClick = viewModel::abrirCaptura,
                        enabled = !estado.assinando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        if (estado.assinando) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(stringResource(R.string.assinatura_gravando))
                            }
                        } else {
                            Text(stringResource(R.string.assinatura_assinar))
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
private fun PaginasDoDocumento(
    estado: AssinaturaUiState,
    dados: DocumentoParaAssinar,
    modifier: Modifier = Modifier,
) {
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "resumo") { Resumo(estado = estado, dados = dados) }
        when (pdf) {
            EstadoPdf.Carregando -> item(key = "pdf-carregando") { Carregando(Modifier.height(200.dp)) }
            EstadoPdf.Erro -> item(key = "pdf-erro") {
                Text(stringResource(R.string.pdf_erro_abrir), color = MaterialTheme.colorScheme.error)
            }
            is EstadoPdf.Pronto -> items(count = pdf.documento.totalPaginas, key = { "pagina-$it" }) { indice ->
                val campos = camposPorPagina[indice].orEmpty()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.campos_pagina, indice + 1, pdf.documento.totalPaginas),
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
private fun Resumo(estado: AssinaturaUiState, dados: DocumentoParaAssinar) {
    val assinadoEm = dados.vinculo.assinadoEm
    if (assinadoEm != null) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(
                        if (estado.acabouDeAssinar) R.string.assinatura_sucesso_titulo else R.string.assinatura_ja_assinado,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.documento_assinado_em, formatarDataHora(assinadoEm)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    } else {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (dados.documento.mensagemConvite.isNotBlank()) {
                    Text(dados.documento.mensagemConvite, style = MaterialTheme.typography.bodyMedium)
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
}
