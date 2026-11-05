package br.com.letssign.domain.regra

import br.com.letssign.domain.model.CampoAssinatura
import br.com.letssign.domain.model.Documento
import br.com.letssign.domain.model.DocumentoDetalhado
import br.com.letssign.domain.model.DocumentoSignatario
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.domain.model.Perfil
import br.com.letssign.domain.model.SignatarioDoDocumento
import br.com.letssign.domain.model.StatusAssinatura
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.domain.model.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorEnvioTest {

    private val documento = Documento(
        id = 1,
        titulo = "Contrato",
        nomeArquivo = "contrato.pdf",
        caminhoOriginal = "/documentos/contrato.pdf",
        caminhoAtual = "/documentos/contrato.pdf",
        totalPaginas = 2,
        status = StatusDocumento.RASCUNHO,
        versao = 1,
        documentoOrigemId = null,
        adminId = 10,
        mensagemConvite = "",
        criadoEm = 0,
        enviadoEm = null,
        concluidoEm = null,
    )

    private fun signatario(id: Long, nome: String, vararg campos: CampoAssinatura) = SignatarioDoDocumento(
        vinculo = DocumentoSignatario(id, 1, id, PapelSignatario.CONTRATANTE, true, StatusAssinatura.PENDENTE, null),
        usuario = Usuario(id, nome, "$id@teste.com", "11999990000", Perfil.SIGNATARIO),
        campos = campos.toList(),
    )

    private fun campo(vinculoId: Long, pagina: Int = 0, x: Float = 0.1f, largura: Float = 0.3f) =
        CampoAssinatura(0, vinculoId, pagina, x, 0.1f, largura, 0.1f)

    private fun validar(
        vararg signatarios: SignatarioDoDocumento,
        documento: Documento = this.documento,
        mensagem: String = "Assine, por favor.",
    ) = ValidadorEnvio.validar(DocumentoDetalhado(documento, signatarios.toList()), mensagem)

    @Test
    fun `documento completo pode ser enviado`() {
        val problemas = validar(signatario(1, "Ana", campo(1)), signatario(2, "Bia", campo(2, pagina = 1)))

        assertTrue(problemas.isEmpty())
    }

    @Test
    fun `sem signatarios nao envia`() {
        assertEquals(listOf(ProblemaEnvio(ErroEnvio.SEM_SIGNATARIOS)), validar())
    }

    @Test
    fun `todo signatario precisa de ao menos um campo`() {
        val problemas = validar(signatario(1, "Ana", campo(1)), signatario(2, "Bia"), signatario(3, "Caio"))

        assertEquals(listOf(ProblemaEnvio(ErroEnvio.SIGNATARIO_SEM_CAMPO, listOf("Bia", "Caio"))), problemas)
    }

    @Test
    fun `campo precisa pertencer ao vinculo do signatario`() {
        val problemas = validar(signatario(1, "Ana", campo(vinculoId = 99)))

        assertEquals(listOf(ProblemaEnvio(ErroEnvio.CAMPO_SEM_SIGNATARIO, listOf("Ana"))), problemas)
    }

    @Test
    fun `campo que ultrapassa a pagina impede o envio`() {
        val problemas = validar(signatario(1, "Ana", campo(1, x = 0.8f, largura = 0.3f)))

        assertEquals(listOf(ProblemaEnvio(ErroEnvio.CAMPO_FORA_DA_PAGINA, listOf("Ana"))), problemas)
    }

    @Test
    fun `campo em pagina inexistente impede o envio`() {
        val problemas = validar(signatario(1, "Ana", campo(1, pagina = 2)))

        assertEquals(listOf(ProblemaEnvio(ErroEnvio.CAMPO_EM_PAGINA_INEXISTENTE, listOf("Ana"))), problemas)
    }

    @Test
    fun `titulo, arquivo e mensagem sao obrigatorios`() {
        val incompleto = documento.copy(titulo = "  ", caminhoOriginal = "", totalPaginas = 0)

        val erros = validar(signatario(1, "Ana", campo(1)), documento = incompleto, mensagem = " ").map { it.erro }

        assertTrue(ErroEnvio.TITULO_VAZIO in erros)
        assertTrue(ErroEnvio.SEM_ARQUIVO in erros)
        assertTrue(ErroEnvio.MENSAGEM_VAZIA in erros)
    }

    @Test
    fun `todos os problemas sao apontados de uma vez`() {
        val problemas = validar(
            signatario(1, "Ana"),
            signatario(2, "Bia", campo(2, x = 0.9f)),
            mensagem = "",
        )

        assertEquals(
            setOf(ErroEnvio.MENSAGEM_VAZIA, ErroEnvio.SIGNATARIO_SEM_CAMPO, ErroEnvio.CAMPO_FORA_DA_PAGINA),
            problemas.map { it.erro }.toSet(),
        )
    }
}
