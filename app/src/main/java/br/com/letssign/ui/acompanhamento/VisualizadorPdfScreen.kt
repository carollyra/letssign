package br.com.letssign.ui.acompanhamento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.TituloDeSecao
import br.com.letssign.ui.pdf.EstadoPdf
import br.com.letssign.ui.pdf.PaginaPdf
import br.com.letssign.ui.pdf.rememberDocumentoPdf
import br.com.letssign.ui.theme.Espacos

@Composable
fun VisualizadorPdfScreen(
    onVoltar: () -> Unit,
    viewModel: VisualizadorPdfViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val documento = (estado as? EstadoVisualizador.Pronto)?.documento

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = when {
                    documento == null -> stringResource(R.string.assinatura_titulo)
                    documento.status == StatusDocumento.CONCLUIDO -> stringResource(R.string.visualizador_final)
                    else -> documento.titulo
                },
                onVoltar = onVoltar,
            )
        },
    ) { margens ->
        when {
            estado is EstadoVisualizador.Carregando -> Carregando(Modifier.padding(margens))
            documento == null -> EstadoErro(
                mensagem = stringResource(R.string.documento_erro_nao_encontrado),
                modifier = Modifier.padding(margens),
                onTentarNovamente = viewModel::carregar,
            )
            else -> when (val pdf = rememberDocumentoPdf(documento.caminhoAtual)) {
                EstadoPdf.Carregando -> Carregando(Modifier.padding(margens))
                EstadoPdf.Erro -> EstadoErro(
                    mensagem = stringResource(R.string.pdf_erro_abrir),
                    modifier = Modifier.padding(margens),
                )
                is EstadoPdf.Pronto -> LazyColumn(
                    modifier = Modifier.padding(margens),
                    contentPadding = PaddingValues(
                        start = Espacos.tela,
                        end = Espacos.tela,
                        top = Espacos.xs,
                        bottom = Espacos.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Espacos.l),
                ) {
                    items(count = pdf.documento.totalPaginas, key = { it }) { indice ->
                        Column(verticalArrangement = Arrangement.spacedBy(Espacos.xs)) {
                            TituloDeSecao(
                                texto = stringResource(
                                    R.string.campos_pagina,
                                    indice + 1,
                                    pdf.documento.totalPaginas,
                                ),
                            )
                            PaginaPdf(documento = pdf.documento, indice = indice)
                        }
                    }
                }
            }
        }
    }
}
