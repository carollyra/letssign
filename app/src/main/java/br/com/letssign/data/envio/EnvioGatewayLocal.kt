package br.com.letssign.data.envio

import br.com.letssign.domain.envio.EnvioGateway
import br.com.letssign.domain.envio.SolicitacaoEnvio
import javax.inject.Inject

/**
 * Sem backend não há para onde mandar convite: o documento chega ao signatário pela mudança
 * de situação gravada no banco, então aqui não há nada a fazer.
 */
class EnvioGatewayLocal @Inject constructor() : EnvioGateway {
    override suspend fun enviar(solicitacao: SolicitacaoEnvio) = Unit
}
