package br.com.letssign.domain.usecase

import br.com.letssign.domain.Relogio
import br.com.letssign.domain.model.ArquivoPdf
import br.com.letssign.domain.model.DocumentoNaoEncontradoException
import br.com.letssign.domain.repository.ArmazenamentoPdf
import br.com.letssign.domain.repository.DocumentoRepository
import javax.inject.Inject

class CriarNovaVersaoUseCase @Inject constructor(
    private val usuarioLogado: ObterUsuarioLogadoUseCase,
    private val documentos: DocumentoRepository,
    private val armazenamento: ArmazenamentoPdf,
    private val relogio: Relogio,
) {
    /** Devolve o id do rascunho criado a partir do documento já enviado. */
    suspend operator fun invoke(documentoId: Long): Long {
        val admin = usuarioLogado.admin()
        val origem = documentos.buscarDetalheDoAdmin(documentoId, admin.id)?.documento
            ?: throw DocumentoNaoEncontradoException()
        check(!origem.editavel) { "Rascunho não gera nova versão" }

        // A nova versão parte do PDF sem assinaturas e ganha uma cópia própria, para a troca
        // do arquivo no rascunho novo nunca tocar no documento já enviado.
        val copia = armazenamento.duplicar(origem.caminhoOriginal)
        return try {
            documentos.criarNovaVersao(
                documentoId = documentoId,
                adminId = admin.id,
                arquivo = ArquivoPdf(origem.nomeArquivo, copia, origem.totalPaginas),
                agora = relogio.agora(),
            )
        } catch (e: Exception) {
            armazenamento.remover(copia)
            throw e
        }
    }
}
