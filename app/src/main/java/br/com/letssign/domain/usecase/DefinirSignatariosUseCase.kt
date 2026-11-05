package br.com.letssign.domain.usecase

import br.com.letssign.domain.repository.DocumentoRepository
import br.com.letssign.domain.repository.SelecaoSignatario
import javax.inject.Inject

class DefinirSignatariosUseCase @Inject constructor(
    private val usuarioLogado: ObterUsuarioLogadoUseCase,
    private val documentos: DocumentoRepository,
) {
    /** Devolve `false`, sem gravar nada, quando a seleção está vazia. */
    suspend operator fun invoke(documentoId: Long, selecao: List<SelecaoSignatario>): Boolean {
        val admin = usuarioLogado.admin()
        if (selecao.isEmpty()) return false
        documentos.definirSignatarios(documentoId, admin.id, selecao.distinctBy { it.signatarioId })
        return true
    }
}
