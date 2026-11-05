package br.com.letssign.domain.model

/** PDF já copiado para o armazenamento do app. */
data class ArquivoPdf(
    val nomeArquivo: String,
    val caminho: String,
    val totalPaginas: Int,
)
