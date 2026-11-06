package br.com.letssign.ui.pdf

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import br.com.letssign.R
import kotlinx.coroutines.CancellationException

sealed interface EstadoPdf {
    data object Carregando : EstadoPdf
    data object Erro : EstadoPdf
    data class Pronto(val documento: DocumentoPdf) : EstadoPdf
}

@Composable
fun rememberDocumentoPdf(caminho: String?): EstadoPdf {
    val estado by produceState<EstadoPdf>(EstadoPdf.Carregando, caminho) {
        value = EstadoPdf.Carregando
        if (caminho == null) return@produceState
        val documento = try {
            DocumentoPdf.abrir(caminho)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            value = EstadoPdf.Erro
            return@produceState
        }
        value = EstadoPdf.Pronto(documento)
        awaitDispose { documento.fechar() }
    }
    return estado
}

private sealed interface EstadoPagina {
    data object Carregando : EstadoPagina
    data object Erro : EstadoPagina
    data class Pronta(val imagem: ImageBitmap) : EstadoPagina
}

/**
 * Desenha uma página ocupando o espaço disponível sem distorcer. [sobreposicao] é composto
 * por cima da página e recebe o tamanho dela em pixels de tela.
 */
@Composable
fun PaginaPdf(
    documento: DocumentoPdf,
    indice: Int,
    modifier: Modifier = Modifier,
    sobreposicao: @Composable BoxScope.(tamanho: IntSize) -> Unit = {},
) {
    val proporcao by produceState(PROPORCAO_A4, documento, indice) {
        value = try {
            documento.proporcao(indice)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PROPORCAO_A4
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(proporcao)
            .sombraDaFolha()
            .background(Color.White),
    ) {
        val tamanho = IntSize(constraints.maxWidth, constraints.maxHeight)
        val pagina by produceState<EstadoPagina>(EstadoPagina.Carregando, documento, indice, tamanho.width) {
            value = try {
                EstadoPagina.Pronta(documento.renderizar(indice, tamanho.width).asImageBitmap())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EstadoPagina.Erro
            }
        }

        when (val atual = pagina) {
            EstadoPagina.Carregando -> CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp),
                strokeWidth = 2.5.dp,
                color = COR_DO_CARREGAMENTO,
            )
            EstadoPagina.Erro -> Text(
                text = stringResource(R.string.pdf_erro_pagina),
                style = MaterialTheme.typography.bodyMedium,
                color = COR_DO_ERRO,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
            )
            is EstadoPagina.Pronta -> Image(
                bitmap = atual.imagem,
                contentDescription = stringResource(R.string.pdf_pagina, indice + 1),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
            )
        }
        sobreposicao(tamanho)
    }
}

private const val PROPORCAO_A4 = 210f / 297f

// A folha é sempre branca, então o que vai por cima dela não acompanha o tema escuro.
private val COR_DO_CARREGAMENTO = Color(0xFF2B5BE0)
private val COR_DO_ERRO = Color(0xFFC9372C)

/** Sombra curta e fria, para a folha se destacar do fundo sem parecer um cartão. */
private fun Modifier.sombraDaFolha(): Modifier = shadow(
    elevation = 3.dp,
    ambientColor = Color(0x331B2540),
    spotColor = Color(0x331B2540),
)
