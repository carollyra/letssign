package br.com.letssign.domain.usecase

import br.com.letssign.domain.regra.GeometriaCampo
import br.com.letssign.domain.repository.DocumentoRepository
import br.com.letssign.domain.repository.NovoCampo
import javax.inject.Inject

class SalvarCamposUseCase @Inject constructor(
    private val usuarioLogado: ObterUsuarioLogadoUseCase,
    private val documentos: DocumentoRepository,
) {
    suspend operator fun invoke(documentoId: Long, campos: List<NovoCampo>) {
        val admin = usuarioLogado.admin()
        val ajustados = campos.map { it.copy(area = GeometriaCampo.limitar(it.area)) }
        documentos.salvarCampos(documentoId, admin.id, ajustados)
    }
}
