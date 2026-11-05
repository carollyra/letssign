package br.com.letssign.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.letssign.apoio.AmbienteDeTeste
import br.com.letssign.data.local.entity.CampoAssinaturaEntity
import br.com.letssign.domain.model.DocumentoNaoEncontradoException
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.domain.repository.SelecaoSignatario
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
class DocumentoRepositoryLocalTest {

    private val ambiente = AmbienteDeTeste()
    private val documentos = ambiente.documentos

    @After
    fun fechar() = ambiente.fechar()

    @Test
    fun `rascunho nasce na versao 1 com o arquivo importado`() = runTest {
        val admin = ambiente.criarAdmin()

        val id = documentos.criarRascunho(admin.id, "Contrato", ambiente.arquivo(paginas = 3), agora = 500L)

        val documento = documentos.buscarDetalheDoAdmin(id, admin.id)!!.documento
        assertEquals(StatusDocumento.RASCUNHO, documento.status)
        assertEquals(1, documento.versao)
        assertEquals(3, documento.totalPaginas)
        assertEquals(documento.caminhoOriginal, documento.caminhoAtual)
        assertEquals(500L, documento.criadoEm)
        assertNull(documento.enviadoEm)
    }

    @Test
    fun `admin enxerga apenas os documentos que criou`() = runTest {
        val ana = ambiente.criarAdmin("Admin Ana")
        val bruno = ambiente.criarAdmin("Admin Bruno")
        val idDaAna = documentos.criarRascunho(ana.id, "Da Ana", ambiente.arquivo(), 1L)
        documentos.criarRascunho(bruno.id, "Do Bruno", ambiente.arquivo(), 2L)

        assertEquals(listOf("Da Ana"), documentos.observarDoAdmin(ana.id).first().map { it.titulo })
        assertEquals(listOf("Do Bruno"), documentos.observarDoAdmin(bruno.id).first().map { it.titulo })
        assertNull(documentos.buscarDetalheDoAdmin(idDaAna, bruno.id))
        assertNull(documentos.observarDetalheDoAdmin(idDaAna, bruno.id).first())
        assertNotNull(documentos.buscarDetalheDoAdmin(idDaAna, ana.id))
    }

    @Test
    fun `outro admin nao consegue alterar o rascunho`() = runTest {
        val ana = ambiente.criarAdmin("Admin Ana")
        val bruno = ambiente.criarAdmin("Admin Bruno")
        val signatario = ambiente.criarSignatario("Carla Dias")
        val id = documentos.criarRascunho(ana.id, "Da Ana", ambiente.arquivo(), 1L)

        try {
            documentos.atualizarRascunho(id, bruno.id, "Invadido", null)
            fail("deveria ter recusado")
        } catch (e: DocumentoNaoEncontradoException) {
            // esperado
        }
        try {
            documentos.definirSignatarios(
                id,
                bruno.id,
                listOf(SelecaoSignatario(signatario.id, PapelSignatario.TESTEMUNHA)),
            )
            fail("deveria ter recusado")
        } catch (e: DocumentoNaoEncontradoException) {
            // esperado
        }

        val detalhe = documentos.buscarDetalheDoAdmin(id, ana.id)!!
        assertEquals("Da Ana", detalhe.documento.titulo)
        assertTrue(detalhe.signatarios.isEmpty())
    }

    @Test
    fun `nova selecao remove quem saiu, atualiza papeis e preserva campos de quem ficou`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val davi = ambiente.criarSignatario("Davi Melo")
        val elis = ambiente.criarSignatario("Elis Prado")
        val id = documentos.criarRascunho(admin.id, "Contrato", ambiente.arquivo(), 1L)
        documentos.definirSignatarios(
            id,
            admin.id,
            listOf(
                SelecaoSignatario(carla.id, PapelSignatario.CONTRATANTE),
                SelecaoSignatario(davi.id, PapelSignatario.CONTRATADO),
            ),
        )
        val vinculos = ambiente.banco.documentoSignatarioDao().listarDoDocumento(id)
        ambiente.banco.campoAssinaturaDao().inserir(
            vinculos.map { CampoAssinaturaEntity(0, it.id, 0, 0.1f, 0.1f, 0.3f, 0.1f) },
        )

        documentos.definirSignatarios(
            id,
            admin.id,
            listOf(
                SelecaoSignatario(carla.id, PapelSignatario.TESTEMUNHA),
                SelecaoSignatario(elis.id, PapelSignatario.CONTRATADO),
            ),
        )

        val signatarios = documentos.buscarDetalheDoAdmin(id, admin.id)!!.signatarios
            .associateBy { it.usuario.nome }
        assertEquals(setOf("Carla Dias", "Elis Prado"), signatarios.keys)
        assertEquals(PapelSignatario.TESTEMUNHA, signatarios.getValue("Carla Dias").vinculo.papel)
        assertEquals(1, signatarios.getValue("Carla Dias").campos.size)
        assertTrue(signatarios.getValue("Elis Prado").campos.isEmpty())
        assertTrue(signatarios.getValue("Elis Prado").vinculo.obrigatorio)
    }

    @Test
    fun `selecao so aceita usuarios com perfil de signatario`() = runTest {
        val admin = ambiente.criarAdmin()
        val outroAdmin = ambiente.criarAdmin("Outro Admin")
        val id = documentos.criarRascunho(admin.id, "Contrato", ambiente.arquivo(), 1L)

        try {
            documentos.definirSignatarios(
                id,
                admin.id,
                listOf(SelecaoSignatario(outroAdmin.id, PapelSignatario.CONTRATANTE)),
            )
            fail("deveria ter recusado")
        } catch (e: IllegalArgumentException) {
            // esperado
        }

        assertTrue(documentos.buscarDetalheDoAdmin(id, admin.id)!!.signatarios.isEmpty())
    }

    @Test
    fun `trocar o arquivo do rascunho descarta os campos posicionados`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val id = documentos.criarRascunho(admin.id, "Contrato", ambiente.arquivo("a.pdf", 5), 1L)
        documentos.definirSignatarios(
            id,
            admin.id,
            listOf(SelecaoSignatario(carla.id, PapelSignatario.CONTRATANTE)),
        )
        val vinculo = ambiente.banco.documentoSignatarioDao().listarDoDocumento(id).single()
        ambiente.banco.campoAssinaturaDao().inserir(
            listOf(CampoAssinaturaEntity(0, vinculo.id, 4, 0.1f, 0.1f, 0.3f, 0.1f)),
        )

        documentos.atualizarRascunho(id, admin.id, "Contrato", ambiente.arquivo("b.pdf", 1))

        val detalhe = documentos.buscarDetalheDoAdmin(id, admin.id)!!
        assertEquals("b.pdf", detalhe.documento.nomeArquivo)
        assertEquals(1, detalhe.documento.totalPaginas)
        assertEquals(1, detalhe.signatarios.size)
        assertTrue(detalhe.signatarios.single().campos.isEmpty())
    }
}
