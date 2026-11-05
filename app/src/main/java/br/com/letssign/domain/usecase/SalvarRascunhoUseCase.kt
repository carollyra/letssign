package br.com.letssign.domain.usecase

import br.com.letssign.domain.Relogio
import br.com.letssign.domain.model.ArquivoPdf
import br.com.letssign.domain.model.DocumentoNaoEncontradoException
import br.com.letssign.domain.repository.ArmazenamentoPdf
import br.com.letssign.domain.repository.DocumentoRepository
import javax.inject.Inject

sealed interface ResultadoSalvarRascunho {
    data class Sucesso(val documentoId: Long) : ResultadoSalvarRascunho
    data class Invalido(val tituloInvalido: Boolean, val semArquivo: Boolean) : ResultadoSalvarRascunho
}

class SalvarRascunhoUseCase @Inject constructor(
    private val usuarioLogado: ObterUsuarioLogadoUseCase,
    private val documentos: DocumentoRepository,
    private val armazenamento: ArmazenamentoPdf,
    private val relogio: Relogio,
) {
    /**
     * Cria o rascunho quando [documentoId] é nulo; caso contrário atualiza o título e,
     * se [novoArquivo] vier preenchido, troca o PDF.
     */
    suspend operator fun invoke(
        documentoId: Long?,
        titulo: String,
        novoArquivo: ArquivoPdf?,
    ): ResultadoSalvarRascunho {
        val admin = usuarioLogado.admin()
        val tituloLimpo = titulo.trim()
        val tituloInvalido = tituloLimpo.isEmpty() || tituloLimpo.length > TAMANHO_MAXIMO_TITULO
        val semArquivo = documentoId == null && novoArquivo == null
        if (tituloInvalido || semArquivo) {
            return ResultadoSalvarRascunho.Invalido(tituloInvalido, semArquivo)
        }

        if (documentoId == null) {
            val id = documentos.criarRascunho(admin.id, tituloLimpo, novoArquivo!!, relogio.agora())
            return ResultadoSalvarRascunho.Sucesso(id)
        }

        val anterior = documentos.buscarDetalheDoAdmin(documentoId, admin.id)?.documento
            ?: throw DocumentoNaoEncontradoException()
        documentos.atualizarRascunho(documentoId, admin.id, tituloLimpo, novoArquivo)
        if (novoArquivo != null && anterior.caminhoOriginal != novoArquivo.caminho) {
            armazenamento.remover(anterior.caminhoOriginal)
        }
        return ResultadoSalvarRascunho.Sucesso(documentoId)
    }

    companion object {
        const val TAMANHO_MAXIMO_TITULO = 120
    }
}
