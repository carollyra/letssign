package br.com.letssign.domain.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.letssign.apoio.AmbienteDeTeste
import br.com.letssign.domain.model.DocumentoNaoEncontradoException
import br.com.letssign.domain.model.StatusAssinatura
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.domain.model.TipoEvento
import br.com.letssign.domain.model.Usuario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CriarNovaVersaoUseCaseTest {

    private val ambiente = AmbienteDeTeste()
    private val documentos = ambiente.documentos
    private val criarNovaVersao = CriarNovaVersaoUseCase(
        ambiente.usuarioLogado,
        documentos,
        ambiente.armazenamento,
        ambiente.relogio,
    )
    private val assinar = AssinarDocumentoUseCase(
        ambiente.usuarioLogado,
        documentos,
        ambiente.assinador,
        ambiente.armazenamento,
        ambiente.relogio,
    )
    private val enviar = EnviarDocumentoUseCase(
        ambiente.usuarioLogado,
        documentos,
        ambiente.gateway,
        ambiente.relogio,
    )

    @After
    fun fechar() = ambiente.fechar()

    private suspend fun enviado(admin: Usuario, vararg signatarios: Usuario): Long {
        val id = ambiente.rascunhoPronto(admin, *signatarios)
        documentos.registrarEnvio(id, admin.id, "Assinem.", ambiente.relogio.agora())
        return id
    }

    private suspend fun eventosDe(admin: Usuario, id: Long) =
        documentos.observarEventos(id, admin.id).first().map { it.evento.tipo }

    @Test
    fun `nova versao nasce como rascunho ligado ao original, com os mesmos signatarios e campos`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val davi = ambiente.criarSignatario("Davi Melo")
        val origemId = enviado(admin, carla, davi)
        val origem = documentos.buscarDetalheDoAdmin(origemId, admin.id)!!
        ambiente.relogio.instante = 8_000L

        val novoId = criarNovaVersao(origemId)

        val novo = documentos.buscarDetalheDoAdmin(novoId, admin.id)!!
        assertEquals(StatusDocumento.RASCUNHO, novo.documento.status)
        assertEquals(origem.documento.versao + 1, novo.documento.versao)
        assertEquals(origemId, novo.documento.documentoOrigemId)
        assertEquals(origem.documento.titulo, novo.documento.titulo)
        assertEquals(origem.documento.mensagemConvite, novo.documento.mensagemConvite)
        assertEquals(8_000L, novo.documento.criadoEm)
        assertNull(novo.documento.enviadoEm)
        assertEquals(
            origem.signatarios.map { Triple(it.usuario.id, it.vinculo.papel, it.campos.map { c -> c.area to c.pagina }) },
            novo.signatarios.map { Triple(it.usuario.id, it.vinculo.papel, it.campos.map { c -> c.area to c.pagina }) },
        )
        assertTrue(novo.signatarios.all { it.vinculo.status == StatusAssinatura.PENDENTE })
    }

    @Test
    fun `documento original fica intacto e ganha o evento de nova versao`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val origemId = enviado(admin, carla)
        val antes = documentos.buscarDetalheDoAdmin(origemId, admin.id)

        val novoId = criarNovaVersao(origemId)

        assertEquals(antes, documentos.buscarDetalheDoAdmin(origemId, admin.id))
        assertEquals(
            listOf(TipoEvento.CRIADO, TipoEvento.ENVIADO, TipoEvento.NOVA_VERSAO),
            eventosDe(admin, origemId),
        )
        assertEquals(listOf(TipoEvento.CRIADO), eventosDe(admin, novoId))
    }

    @Test
    fun `nova versao de documento assinado parte do pdf sem assinaturas, numa copia propria`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val origemId = enviado(admin, carla)
        ambiente.entrarComo(carla)
        assinar(origemId, byteArrayOf(1))
        ambiente.entrarComo(admin)
        val origem = documentos.buscarDetalheDoAdmin(origemId, admin.id)!!.documento

        val novo = documentos.buscarDetalheDoAdmin(criarNovaVersao(origemId), admin.id)!!

        assertEquals(StatusDocumento.CONCLUIDO, origem.status)
        assertEquals("${origem.caminhoOriginal}.copia1", novo.documento.caminhoOriginal)
        assertEquals(novo.documento.caminhoOriginal, novo.documento.caminhoAtual)
        assertEquals(StatusAssinatura.PENDENTE, novo.signatarios.single().vinculo.status)
        assertNull(novo.signatarios.single().vinculo.assinadoEm)
        assertNull(novo.documento.concluidoEm)
    }

    @Test
    fun `nova versao pode ser alterada e enviada, e so entao chega ao signatario`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val origemId = enviado(admin, carla)
        val novoId = criarNovaVersao(origemId)

        documentos.atualizarRascunho(novoId, admin.id, "Contrato revisado", null)
        assertEquals(listOf(origemId), documentos.observarDoSignatario(carla.id).first().map { it.documento.id })

        assertEquals(ResultadoEnvio.Enviado(repetido = false), enviar(novoId, "Nova versão para assinar."))
        assertEquals(
            setOf(origemId, novoId),
            documentos.observarDoSignatario(carla.id).first().map { it.documento.id }.toSet(),
        )
        assertEquals("Contrato", documentos.buscarDetalheDoAdmin(origemId, admin.id)!!.documento.titulo)
    }

    @Test
    fun `rascunho nao gera nova versao`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val rascunhoId = ambiente.rascunhoPronto(admin, ambiente.criarSignatario("Carla Dias"))

        try {
            criarNovaVersao(rascunhoId)
            fail("deveria ter recusado")
        } catch (e: IllegalStateException) {
            // esperado
        }
        assertEquals(1, documentos.observarDoAdmin(admin.id).first().size)
        assertTrue(ambiente.armazenamento.removidos.isEmpty())
    }

    @Test
    fun `admin nao cria versao de documento de outro admin`() = runTest {
        val dona = ambiente.criarAdmin("Admin Dona")
        val intruso = ambiente.criarAdmin("Admin Intruso")
        val origemId = enviado(dona, ambiente.criarSignatario("Carla Dias"))
        ambiente.entrarComo(intruso)

        try {
            criarNovaVersao(origemId)
            fail("deveria ter recusado")
        } catch (e: DocumentoNaoEncontradoException) {
            // esperado
        }
        assertTrue(documentos.observarDoAdmin(intruso.id).first().isEmpty())
        assertEquals(1, documentos.observarDoAdmin(dona.id).first().size)
    }

    @Test
    fun `historico traz criacao, envio e assinaturas na ordem, com quem fez e quando`() = runTest {
        val admin = ambiente.criarAdmin("Admin Teste")
        val carla = ambiente.criarSignatario("Carla Dias")
        val davi = ambiente.criarSignatario("Davi Melo")
        ambiente.relogio.instante = 100L
        val id = ambiente.rascunhoPronto(admin, carla, davi)
        documentos.registrarEnvio(id, admin.id, "Assinem.", 200L)
        ambiente.entrarComo(carla)
        ambiente.relogio.instante = 300L
        assinar(id, byteArrayOf(1))
        ambiente.entrarComo(davi)
        ambiente.relogio.instante = 400L
        assinar(id, byteArrayOf(1))

        val historico = documentos.observarEventos(id, admin.id).first()
            .map { Triple(it.evento.tipo, it.nomeUsuario, it.evento.dataHora) }

        assertEquals(
            listOf(
                Triple(TipoEvento.CRIADO, "Admin Teste", 100L),
                Triple(TipoEvento.ENVIADO, "Admin Teste", 200L),
                Triple(TipoEvento.ASSINADO, "Carla Dias", 300L),
                Triple(TipoEvento.ASSINADO, "Davi Melo", 400L),
                Triple(TipoEvento.CONCLUIDO, "Davi Melo", 400L),
            ),
            historico,
        )
        val detalhe = documentos.buscarDetalheDoAdmin(id, admin.id)!!
        assertEquals(400L, detalhe.documento.concluidoEm)
        assertEquals(listOf(300L, 400L), detalhe.signatarios.map { it.vinculo.assinadoEm })
    }
}
