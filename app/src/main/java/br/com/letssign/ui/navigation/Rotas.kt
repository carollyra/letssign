package br.com.letssign.ui.navigation

object Rotas {
    const val HOME = "home"

    const val SIGNATARIOS = "signatarios"
    const val ARG_SIGNATARIO_ID = "signatarioId"
    const val SIGNATARIO_FORM = "signatarios/form?$ARG_SIGNATARIO_ID={$ARG_SIGNATARIO_ID}"

    fun signatarioForm(id: Long? = null) = "signatarios/form?$ARG_SIGNATARIO_ID=${id ?: 0}"
}
