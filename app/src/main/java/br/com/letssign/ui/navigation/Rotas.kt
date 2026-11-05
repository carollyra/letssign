package br.com.letssign.ui.navigation

object Rotas {
    const val HOME = "home"

    const val SIGNATARIOS = "signatarios"
    const val ARG_SIGNATARIO_ID = "signatarioId"
    const val SIGNATARIO_FORM = "signatarios/form?$ARG_SIGNATARIO_ID={$ARG_SIGNATARIO_ID}"

    const val ARG_DOCUMENTO_ID = "documentoId"
    const val DOCUMENTO_ARQUIVO = "documento/arquivo?$ARG_DOCUMENTO_ID={$ARG_DOCUMENTO_ID}"
    const val DOCUMENTO_SIGNATARIOS = "documento/{$ARG_DOCUMENTO_ID}/signatarios"
    const val DOCUMENTO_CAMPOS = "documento/{$ARG_DOCUMENTO_ID}/campos"

    fun signatarioForm(id: Long? = null) = "signatarios/form?$ARG_SIGNATARIO_ID=${id ?: 0}"

    fun documentoArquivo(id: Long? = null) = "documento/arquivo?$ARG_DOCUMENTO_ID=${id ?: 0}"

    fun documentoSignatarios(id: Long) = "documento/$id/signatarios"

    fun documentoCampos(id: Long) = "documento/$id/campos"
}
