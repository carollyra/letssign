package br.com.letssign.domain.usecase

import br.com.letssign.domain.Relogio
import br.com.letssign.domain.envio.Destinatario
import br.com.letssign.domain.envio.EnvioGateway
import br.com.letssign.domain.envio.FalhaNoEnvioException
import br.com.letssign.domain.envio.SolicitacaoEnvio
import br.com.letssign.domain.model.DocumentoNaoEncontradoException
import br.com.letssign.domain.regra.ProblemaEnvio
import br.com.letssign.domain.regra.ValidadorEnvio
import br.com.letssign.domain.repository.DocumentoRepository
import br.com.letssign.domain.repository.ResultadoRegistroEnvio
import javax.inject.Inject

sealed interface ResultadoEnvio {
    /** [repetido] indica que o documento já tinha sido enviado antes desta chamada. */
    data class Enviado(val repetido: Boolean) : ResultadoEnvio
    data class Invalido(val problemas: List<ProblemaEnvio>) : ResultadoEnvio

    /** O disparo falhou e nada foi registrado: o documento continua rascunho e pode ser reenviado. */
    data object FalhaNoEnvio : ResultadoEnvio
}

class EnviarDocumentoUseCase @Inject constructor(
    private val usuarioLogado: ObterUsuarioLogadoUseCase,
    private val documentos: DocumentoRepository,
    private val gateway: EnvioGateway,
    private val relogio: Relogio,
) {
    suspend operator fun invoke(documentoId: Long, mensagem: String): ResultadoEnvio {
        val admin = usuarioLogado.admin()
        val detalhe = documentos.buscarDetalheDoAdmin(documentoId, admin.id)
            ?: throw DocumentoNaoEncontradoException()
        if (!detalhe.documento.editavel) return ResultadoEnvio.Enviado(repetido = true)

        val mensagemLimpa = mensagem.trim()
        val problemas = ValidadorEnvio.validar(detalhe, mensagemLimpa)
        if (problemas.isNotEmpty()) return ResultadoEnvio.Invalido(problemas)

        // Gravada antes do disparo para a edição não se perder se o envio falhar.
        documentos.salvarMensagemConvite(documentoId, admin.id, mensagemLimpa)

        try {
            gateway.enviar(
                SolicitacaoEnvio(
                    chaveIdempotencia = "documento-${detalhe.documento.id}-v${detalhe.documento.versao}",
                    documentoId = documentoId,
                    titulo = detalhe.documento.titulo,
                    mensagem = mensagemLimpa,
                    destinatarios = detalhe.signatarios.map { Destinatario(it.usuario.nome, it.usuario.email) },
                ),
            )
        } catch (e: FalhaNoEnvioException) {
            return ResultadoEnvio.FalhaNoEnvio
        }

        val registro = documentos.registrarEnvio(documentoId, admin.id, mensagemLimpa, relogio.agora())
        return ResultadoEnvio.Enviado(repetido = registro == ResultadoRegistroEnvio.JA_ESTAVA_REGISTRADO)
    }
}
