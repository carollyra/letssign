package br.com.letssign.domain.model

enum class StatusDocumento { RASCUNHO, ENVIADO, PARCIALMENTE_ASSINADO, CONCLUIDO }

data class Documento(
    val id: Long,
    val titulo: String,
    val nomeArquivo: String,
    val caminhoOriginal: String,
    val caminhoAtual: String,
    val totalPaginas: Int,
    val status: StatusDocumento,
    val versao: Int,
    val documentoOrigemId: Long?,
    val adminId: Long,
    val mensagemConvite: String,
    val criadoEm: Long,
    val enviadoEm: Long?,
    val concluidoEm: Long?,
) {
    val editavel: Boolean get() = status == StatusDocumento.RASCUNHO
}
