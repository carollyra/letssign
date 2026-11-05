package br.com.letssign.domain.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.letssign.apoio.AmbienteDeTeste
import br.com.letssign.domain.model.DocumentoNaoEncontradoException
import br.com.letssign.domain.model.SemPermissaoException
import br.com.letssign.domain.model.StatusAssinatura
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.domain.model.TipoEvento
import br.com.letssign.domain.model.Usuario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssinarDocumentoUseCaseTest {

    private val ambiente = AmbienteDeTeste()
    private val documentos = ambiente.documentos
    private val assinar = AssinarDocumentoUseCase(
        ambiente.usuarioLogado,
        documentos,
        ambiente.assinador,
        ambiente.armazenamento,
        ambiente.relogio,
    )
    private val png = byteArrayOf(1, 2, 3)

    @After
    fun fechar() = ambiente.fechar()

    private suspend fun enviado(admin: Usuario, vararg signatarios: Usuario, obrigatorios: Set<Long>? = null): Long {
        val id = if (obrigatorios == null) {
            ambiente.rascunhoPronto(admin, *signatarios)
        } else {
            ambiente.rascunhoPronto(admin, *signatarios, obrigatorios = obrigatorios)
        }
        documentos.registrarEnvio(id, admin.id, "Assinem.", ambiente.relogio.agora())
        return id
    }

    private suspend fun documentoDe(admin: Usuario, id: Long) =
        documentos.buscarDetalheDoAdmin(id, admin.id)!!.documento

    private suspend fun eventosDe(admin: Usuario, id: Long) =
        documentos.observarEventos(id, admin.id).first().map { it.evento.tipo }

    @Test
    fun `primeira assinatura deixa o documento parcialmente assinado`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val davi = ambiente.criarSignatario("Davi Melo")
        val id = enviado(admin, carla, davi)
        ambiente.entrarComo(carla)
        ambiente.relogio.instante = 7_000L

        val resultado = assinar(id, png)

        assertEquals(ResultadoAssinatura.Assinado(StatusDocumento.PARCIALMENTE_ASSINADO), resultado)
        val detalhe = documentos.buscarDetalheDoAdmin(id, admin.id)!!
        assertEquals(StatusDocumento.PARCIALMENTE_ASSINADO, detalhe.documento.status)
        assertNull(detalhe.documento.concluidoEm)
        val vinculos = detalhe.signatarios.associate { it.usuario.nome to it.vinculo }
        assertEquals(StatusAssinatura.ASSINADO, vinculos.getValue("Carla Dias").status)
        assertEquals(7_000L, vinculos.getValue("Carla Dias").assinadoEm)
        assertEquals(StatusAssinatura.PENDENTE, vinculos.getValue("Davi Melo").status)
        assertEquals(listOf(TipoEvento.CRIADO, TipoEvento.ENVIADO, TipoEvento.ASSINADO), eventosDe(admin, id))
    }

    @Test
    fun `documento so e concluido quando o ultimo obrigatorio assina`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val davi = ambiente.criarSignatario("Davi Melo")
        val id = enviado(admin, carla, davi)
        ambiente.entrarComo(carla)
        assinar(id, png)
        ambiente.entrarComo(davi)
        ambiente.relogio.instante = 9_000L

        val resultado = assinar(id, png)

        assertEquals(ResultadoAssinatura.Assinado(StatusDocumento.CONCLUIDO), resultado)
        val documento = documentoDe(admin, id)
        assertEquals(StatusDocumento.CONCLUIDO, documento.status)
        assertEquals(9_000L, documento.concluidoEm)
        assertEquals(
            listOf(
                TipoEvento.CRIADO,
                TipoEvento.ENVIADO,
                TipoEvento.ASSINADO,
                TipoEvento.ASSINADO,
                TipoEvento.CONCLUIDO,
            ),
            eventosDe(admin, id),
        )
    }

    @Test
    fun `signatario opcional pendente nao segura a conclusao e depois nao assina mais`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val testemunha = ambiente.criarSignatario("Tales Reis")
        val id = enviado(admin, carla, testemunha, obrigatorios = setOf(carla.id))
        ambiente.entrarComo(carla)

        assertEquals(ResultadoAssinatura.Assinado(StatusDocumento.CONCLUIDO), assinar(id, png))

        ambiente.entrarComo(testemunha)
        val finalAntes = documentoDe(admin, id).caminhoAtual
        assertEquals(ResultadoAssinatura.DocumentoConcluido, assinar(id, png))
        assertEquals(finalAntes, documentoDe(admin, id).caminhoAtual)
    }

    @Test
    fun `assinatura gera arquivo novo e preserva o original`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val davi = ambiente.criarSignatario("Davi Melo")
        val id = enviado(admin, carla, davi)
        val original = documentoDe(admin, id).caminhoOriginal

        ambiente.entrarComo(carla)
        assinar(id, png)
        val aposCarla = documentoDe(admin, id)
        ambiente.entrarComo(davi)
        assinar(id, png)
        val aposDavi = documentoDe(admin, id)

        assertEquals(original, aposCarla.caminhoOriginal)
        assertEquals(original, aposDavi.caminhoOriginal)
        assertEquals("/documentos/assinado-1.pdf", aposCarla.caminhoAtual)
        assertEquals("/documentos/assinado-2.pdf", aposDavi.caminhoAtual)
        // A segunda assinatura é aplicada sobre o arquivo que já tem a primeira.
        assertEquals(listOf(original, "/documentos/assinado-1.pdf"), ambiente.assinador.chamadas.map { it.origem })
        // Só a versão intermediária é descartada; o original nunca.
        assertEquals(listOf("/documentos/assinado-1.pdf"), ambiente.armazenamento.removidos)
    }

    @Test
    fun `assinatura e aplicada apenas nos campos de quem assina`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val davi = ambiente.criarSignatario("Davi Melo")
        val id = enviado(admin, carla, davi)
        val camposDaCarla = documentos.buscarDetalheDoAdmin(id, admin.id)!!.signatarios
            .single { it.usuario.id == carla.id }.campos
        ambiente.entrarComo(carla)

        assinar(id, png)

        assertEquals(camposDaCarla, ambiente.assinador.chamadas.single().campos)
        assertEquals(camposDaCarla, documentos.buscarParaAssinar(id, carla.id)!!.campos)
    }

    @Test
    fun `assinar duas vezes nao duplica evento nem gera outro arquivo`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val davi = ambiente.criarSignatario("Davi Melo")
        val id = enviado(admin, carla, davi)
        ambiente.entrarComo(carla)
        assinar(id, png)

        val repeticao = assinar(id, png)

        assertEquals(ResultadoAssinatura.JaAssinado, repeticao)
        assertEquals(1, ambiente.assinador.chamadas.size)
        assertEquals(1, eventosDe(admin, id).count { it == TipoEvento.ASSINADO })
    }

    @Test
    fun `falha ao gravar o pdf nao altera o registro`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val id = enviado(admin, carla)
        ambiente.entrarComo(carla)
        ambiente.assinador.falhar = true

        val resultado = assinar(id, png)

        assertEquals(ResultadoAssinatura.FalhaAoGravar, resultado)
        val documento = documentoDe(admin, id)
        assertEquals(StatusDocumento.ENVIADO, documento.status)
        assertEquals(documento.caminhoOriginal, documento.caminhoAtual)
        assertEquals(StatusAssinatura.PENDENTE, documentos.buscarParaAssinar(id, carla.id)!!.vinculo.status)

        ambiente.assinador.falhar = false
        assertEquals(ResultadoAssinatura.Assinado(StatusDocumento.CONCLUIDO), assinar(id, png))
    }

    @Test
    fun `assinatura vazia e recusada`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val id = enviado(admin, carla)
        ambiente.entrarComo(carla)

        assertEquals(ResultadoAssinatura.AssinaturaVazia, assinar(id, ByteArray(0)))
        assertTrue(ambiente.assinador.chamadas.isEmpty())
    }

    @Test
    fun `se outra assinatura entra no meio, o arquivo e refeito sobre a versao mais nova`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val davi = ambiente.criarSignatario("Davi Melo")
        val id = enviado(admin, carla, davi)
        val original = documentoDe(admin, id).caminhoOriginal
        ambiente.entrarComo(carla)
        ambiente.assinador.antesDeDevolver = {
            documentos.registrarAssinatura(id, davi.id, original, "/documentos/do-davi.pdf", 50L)
        }

        val resultado = assinar(id, png)

        assertEquals(ResultadoAssinatura.Assinado(StatusDocumento.CONCLUIDO), resultado)
        assertEquals(listOf(original, "/documentos/do-davi.pdf"), ambiente.assinador.chamadas.map { it.origem })
        assertEquals("/documentos/assinado-2.pdf", documentoDe(admin, id).caminhoAtual)
        assertEquals(
            listOf("/documentos/assinado-1.pdf", "/documentos/do-davi.pdf"),
            ambiente.armazenamento.removidos,
        )
    }

    @Test
    fun `quem nao e signatario do documento nao o enxerga nem assina`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val estranho = ambiente.criarSignatario("Enzo Braga")
        val id = enviado(admin, carla)
        ambiente.entrarComo(estranho)

        assertNull(documentos.buscarParaAssinar(id, estranho.id))
        try {
            assinar(id, png)
            fail("deveria ter recusado")
        } catch (e: DocumentoNaoEncontradoException) {
            // esperado
        }
        assertEquals(StatusDocumento.ENVIADO, documentoDe(admin, id).status)
    }

    @Test
    fun `rascunho nao pode ser assinado`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val id = ambiente.rascunhoPronto(admin, carla)
        ambiente.entrarComo(carla)

        assertNull(documentos.buscarParaAssinar(id, carla.id))
        try {
            assinar(id, png)
            fail("deveria ter recusado")
        } catch (e: DocumentoNaoEncontradoException) {
            // esperado
        }
    }

    @Test
    fun `admin nao assina pelo signatario`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val id = enviado(admin, carla)
        ambiente.entrarComo(admin)

        try {
            assinar(id, png)
            fail("deveria ter recusado")
        } catch (e: SemPermissaoException) {
            // esperado
        }
        assertNotNull(documentos.buscarParaAssinar(id, carla.id))
    }
}
