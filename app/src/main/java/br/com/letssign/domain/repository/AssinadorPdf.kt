package br.com.letssign.domain.repository

import br.com.letssign.domain.model.CampoAssinatura

class FalhaAoAssinarPdfException(causa: Throwable? = null) : Exception(causa)

interface AssinadorPdf {

    /**
     * Desenha [assinaturaPng] em cada um dos [campos] e grava o resultado num arquivo novo,
     * cujo caminho é devolvido. O arquivo em [caminhoOrigem] não é alterado.
     *
     * Lança [FalhaAoAssinarPdfException] se o PDF ou a imagem não puderem ser processados.
     */
    suspend fun aplicar(caminhoOrigem: String, assinaturaPng: ByteArray, campos: List<CampoAssinatura>): String
}
