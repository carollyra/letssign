package br.com.letssign.domain.regra

import br.com.letssign.domain.model.DocumentoSignatario
import br.com.letssign.domain.model.StatusAssinatura
import br.com.letssign.domain.model.StatusDocumento

object SituacaoDocumento {

    /**
     * Situação de um documento já enviado a partir das assinaturas registradas. Ele só é
     * concluído quando todos os signatários obrigatórios assinaram; se nenhum for
     * obrigatório, quando todos assinaram.
     */
    fun aposAssinaturas(vinculos: List<DocumentoSignatario>): StatusDocumento {
        val assinou = { vinculo: DocumentoSignatario -> vinculo.status == StatusAssinatura.ASSINADO }
        if (vinculos.none(assinou)) return StatusDocumento.ENVIADO

        val necessarios = vinculos.filter { it.obrigatorio }.ifEmpty { vinculos }
        return if (necessarios.all(assinou)) StatusDocumento.CONCLUIDO else StatusDocumento.PARCIALMENTE_ASSINADO
    }
}
