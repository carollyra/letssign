package br.com.letssign.domain.regra

import br.com.letssign.domain.model.DocumentoSignatario
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.domain.model.StatusAssinatura
import br.com.letssign.domain.model.StatusDocumento
import org.junit.Assert.assertEquals
import org.junit.Test

class SituacaoDocumentoTest {

    private fun vinculo(id: Long, assinou: Boolean, obrigatorio: Boolean = true) = DocumentoSignatario(
        id = id,
        documentoId = 1,
        signatarioId = id,
        papel = PapelSignatario.CONTRATANTE,
        obrigatorio = obrigatorio,
        status = if (assinou) StatusAssinatura.ASSINADO else StatusAssinatura.PENDENTE,
        assinadoEm = if (assinou) 1L else null,
    )

    @Test
    fun `sem nenhuma assinatura o documento continua enviado`() {
        val status = SituacaoDocumento.aposAssinaturas(listOf(vinculo(1, false), vinculo(2, false)))

        assertEquals(StatusDocumento.ENVIADO, status)
    }

    @Test
    fun `primeira assinatura deixa o documento parcialmente assinado`() {
        val status = SituacaoDocumento.aposAssinaturas(
            listOf(vinculo(1, true), vinculo(2, false), vinculo(3, false)),
        )

        assertEquals(StatusDocumento.PARCIALMENTE_ASSINADO, status)
    }

    @Test
    fun `falta de um unico obrigatorio impede a conclusao`() {
        val status = SituacaoDocumento.aposAssinaturas(
            listOf(vinculo(1, true), vinculo(2, true), vinculo(3, false)),
        )

        assertEquals(StatusDocumento.PARCIALMENTE_ASSINADO, status)
    }

    @Test
    fun `todos os obrigatorios assinados concluem o documento`() {
        val status = SituacaoDocumento.aposAssinaturas(listOf(vinculo(1, true), vinculo(2, true)))

        assertEquals(StatusDocumento.CONCLUIDO, status)
    }

    @Test
    fun `signatario unico conclui ao assinar`() {
        assertEquals(StatusDocumento.CONCLUIDO, SituacaoDocumento.aposAssinaturas(listOf(vinculo(1, true))))
    }

    @Test
    fun `opcional pendente nao impede a conclusao`() {
        val status = SituacaoDocumento.aposAssinaturas(
            listOf(vinculo(1, true), vinculo(2, false, obrigatorio = false)),
        )

        assertEquals(StatusDocumento.CONCLUIDO, status)
    }

    @Test
    fun `assinatura so do opcional nao conclui`() {
        val status = SituacaoDocumento.aposAssinaturas(
            listOf(vinculo(1, false), vinculo(2, true, obrigatorio = false)),
        )

        assertEquals(StatusDocumento.PARCIALMENTE_ASSINADO, status)
    }

    @Test
    fun `sem obrigatorios o documento so conclui quando todos assinam`() {
        val opcionais = listOf(vinculo(1, true, obrigatorio = false), vinculo(2, false, obrigatorio = false))

        assertEquals(StatusDocumento.PARCIALMENTE_ASSINADO, SituacaoDocumento.aposAssinaturas(opcionais))
        assertEquals(
            StatusDocumento.CONCLUIDO,
            SituacaoDocumento.aposAssinaturas(opcionais.map { it.copy(status = StatusAssinatura.ASSINADO) }),
        )
    }
}
