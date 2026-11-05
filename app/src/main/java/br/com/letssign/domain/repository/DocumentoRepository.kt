package br.com.letssign.domain.repository

import br.com.letssign.domain.model.ArquivoPdf
import br.com.letssign.domain.model.Documento
import br.com.letssign.domain.model.DocumentoDetalhado
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.domain.regra.AreaNormalizada
import kotlinx.coroutines.flow.Flow

data class SelecaoSignatario(
    val signatarioId: Long,
    val papel: PapelSignatario,
    val obrigatorio: Boolean = true,
)

data class NovoCampo(
    val signatarioId: Long,
    val pagina: Int,
    val area: AreaNormalizada,
)

/**
 * Todas as operações recebem quem está consultando. Um documento de outro Admin se comporta
 * como inexistente: leituras devolvem `null` e escritas lançam
 * [br.com.letssign.domain.model.DocumentoNaoEncontradoException].
 *
 * Escritas em documento já enviado lançam
 * [br.com.letssign.domain.model.DocumentoNaoEditavelException].
 */
interface DocumentoRepository {

    fun observarDoAdmin(adminId: Long): Flow<List<Documento>>

    fun observarDetalheDoAdmin(documentoId: Long, adminId: Long): Flow<DocumentoDetalhado?>

    suspend fun buscarDetalheDoAdmin(documentoId: Long, adminId: Long): DocumentoDetalhado?

    suspend fun criarRascunho(adminId: Long, titulo: String, arquivo: ArquivoPdf, agora: Long): Long

    /**
     * Com [novoArquivo] o PDF do rascunho é trocado e os campos já posicionados são
     * descartados, porque pertenciam às páginas do arquivo anterior.
     */
    suspend fun atualizarRascunho(
        documentoId: Long,
        adminId: Long,
        titulo: String,
        novoArquivo: ArquivoPdf?,
    )

    /** Substitui a seleção inteira. Quem continua selecionado mantém os campos que já tinha. */
    suspend fun definirSignatarios(documentoId: Long, adminId: Long, selecao: List<SelecaoSignatario>)

    /**
     * Substitui todos os campos do rascunho. Lança [IllegalArgumentException] se algum campo
     * for de quem não é signatário do documento, apontar para uma página inexistente ou
     * ultrapassar os limites da página.
     */
    suspend fun salvarCampos(documentoId: Long, adminId: Long, campos: List<NovoCampo>)
}
