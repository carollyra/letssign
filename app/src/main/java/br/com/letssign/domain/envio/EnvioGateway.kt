package br.com.letssign.domain.envio

data class Destinatario(val nome: String, val email: String)

data class SolicitacaoEnvio(
    /**
     * Igual em todas as tentativas de envio do mesmo documento, para o serviço de destino
     * reconhecer uma repetição e não disparar convites em dobro.
     */
    val chaveIdempotencia: String,
    val documentoId: Long,
    val titulo: String,
    val mensagem: String,
    val destinatarios: List<Destinatario>,
)

class FalhaNoEnvioException(mensagem: String? = null, causa: Throwable? = null) : Exception(mensagem, causa)

/** Dispara os convites de assinatura. Lança [FalhaNoEnvioException] quando o envio não acontece. */
interface EnvioGateway {
    suspend fun enviar(solicitacao: SolicitacaoEnvio)
}
