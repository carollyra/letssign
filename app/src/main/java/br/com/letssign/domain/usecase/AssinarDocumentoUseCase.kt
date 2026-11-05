package br.com.letssign.domain.usecase

import br.com.letssign.domain.Relogio
import br.com.letssign.domain.model.DocumentoNaoEncontradoException
import br.com.letssign.domain.model.StatusAssinatura
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.domain.repository.ArmazenamentoPdf
import br.com.letssign.domain.repository.AssinadorPdf
import br.com.letssign.domain.repository.DocumentoRepository
import br.com.letssign.domain.repository.FalhaAoAssinarPdfException
import br.com.letssign.domain.repository.ResultadoRegistroAssinatura
import javax.inject.Inject

sealed interface ResultadoAssinatura {
    data class Assinado(val statusDocumento: StatusDocumento) : ResultadoAssinatura
    data object JaAssinado : ResultadoAssinatura

    /** O documento foi concluído sem depender desta assinatura. */
    data object DocumentoConcluido : ResultadoAssinatura
    data object AssinaturaVazia : ResultadoAssinatura
    data object FalhaAoGravar : ResultadoAssinatura
}

class AssinarDocumentoUseCase @Inject constructor(
    private val usuarioLogado: ObterUsuarioLogadoUseCase,
    private val documentos: DocumentoRepository,
    private val assinador: AssinadorPdf,
    private val armazenamento: ArmazenamentoPdf,
    private val relogio: Relogio,
) {
    suspend operator fun invoke(documentoId: Long, assinaturaPng: ByteArray): ResultadoAssinatura {
        val signatario = usuarioLogado.signatario()
        if (assinaturaPng.isEmpty()) return ResultadoAssinatura.AssinaturaVazia

        // Se outra assinatura entrar no meio do caminho, o arquivo é refeito sobre a versão nova.
        repeat(TENTATIVAS) {
            val paraAssinar = documentos.buscarParaAssinar(documentoId, signatario.id)
                ?: throw DocumentoNaoEncontradoException()
            if (paraAssinar.vinculo.status == StatusAssinatura.ASSINADO) return ResultadoAssinatura.JaAssinado
            if (paraAssinar.documento.status == StatusDocumento.CONCLUIDO) {
                return ResultadoAssinatura.DocumentoConcluido
            }

            val base = paraAssinar.documento.caminhoAtual
            val assinado = try {
                assinador.aplicar(base, assinaturaPng, paraAssinar.campos)
            } catch (e: FalhaAoAssinarPdfException) {
                return ResultadoAssinatura.FalhaAoGravar
            }

            val registro = try {
                documentos.registrarAssinatura(documentoId, signatario.id, base, assinado, relogio.agora())
            } catch (e: Exception) {
                armazenamento.remover(assinado)
                throw e
            }
            when (registro) {
                is ResultadoRegistroAssinatura.Registrada -> {
                    // O PDF enviado pelo Admin fica guardado; só versões intermediárias são apagadas.
                    if (registro.caminhoSubstituido != paraAssinar.documento.caminhoOriginal) {
                        armazenamento.remover(registro.caminhoSubstituido)
                    }
                    return ResultadoAssinatura.Assinado(registro.status)
                }
                ResultadoRegistroAssinatura.JaAssinado -> {
                    armazenamento.remover(assinado)
                    return ResultadoAssinatura.JaAssinado
                }
                ResultadoRegistroAssinatura.DocumentoConcluido -> {
                    armazenamento.remover(assinado)
                    return ResultadoAssinatura.DocumentoConcluido
                }
                ResultadoRegistroAssinatura.ArquivoDesatualizado -> armazenamento.remover(assinado)
            }
        }
        return ResultadoAssinatura.FalhaAoGravar
    }

    private companion object {
        const val TENTATIVAS = 3
    }
}
