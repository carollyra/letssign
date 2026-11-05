package br.com.letssign.domain.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.letssign.apoio.AmbienteDeTeste
import br.com.letssign.domain.model.DocumentoNaoEditavelException
import br.com.letssign.domain.model.DocumentoNaoEncontradoException
import br.com.letssign.domain.model.EnvioInvalidoException
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.domain.model.StatusAssinatura
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.domain.model.TipoEvento
import br.com.letssign.domain.model.Usuario
import br.com.letssign.domain.regra.AreaNormalizada
import br.com.letssign.domain.regra.ErroEnvio
import br.com.letssign.domain.regra.ProblemaEnvio
import br.com.letssign.domain.repository.NovoCampo
import br.com.letssign.domain.repository.ResultadoRegistroEnvio
import br.com.letssign.domain.repository.SelecaoSignatario
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
class EnviarDocumentoUseCaseTest {

    private val ambiente = AmbienteDeTeste()
    private val documentos = ambiente.documentos
    private val enviar = EnviarDocumentoUseCase(
        ambiente.usuarioLogado,
        documentos,
        ambiente.gateway,
        ambiente.relogio,
    )

    @After
    fun fechar() = ambiente.fechar()

    private suspend fun statusDe(admin: Usuario, id: Long) =
        documentos.buscarDetalheDoAdmin(id, admin.id)!!.documento.status

    private suspend fun eventosDe(admin: Usuario, id: Long) =
        documentos.observarEventos(id, admin.id).first().map { it.evento.tipo }

    private suspend fun titulosVistosPor(signatario: Usuario) =
        documentos.observarDoSignatario(signatario.id).first().map { it.documento.titulo }

    @Test
    fun `envio muda a situacao, registra o evento e entrega aos signatarios`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val davi = ambiente.criarSignatario("Davi Melo")
        val id = ambiente.rascunhoPronto(admin, carla, davi)
        ambiente.relogio.instante = 5_000L

        val resultado = enviar(id, "  Assinem, por favor.  ")

        assertEquals(ResultadoEnvio.Enviado(repetido = false), resultado)
        val documento = documentos.buscarDetalheDoAdmin(id, admin.id)!!.documento
        assertEquals(StatusDocumento.ENVIADO, documento.status)
        assertEquals(5_000L, documento.enviadoEm)
        assertEquals("Assinem, por favor.", documento.mensagemConvite)
        assertEquals(listOf(TipoEvento.CRIADO, TipoEvento.ENVIADO), eventosDe(admin, id))
        assertEquals(listOf("Contrato"), titulosVistosPor(carla))
        assertEquals(listOf("Contrato"), titulosVistosPor(davi))
        assertEquals(
            StatusAssinatura.PENDENTE,
            documentos.observarDoSignatario(carla.id).first().single().vinculo.status,
        )
        assertEquals(
            listOf("Carla Dias", "Davi Melo"),
            ambiente.gateway.solicitacoes.single().destinatarios.map { it.nome },
        )
    }

    @Test
    fun `documento sem signatario nao e enviado`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val id = documentos.criarRascunho(admin.id, "Contrato", ambiente.arquivo(), 1L)

        val resultado = enviar(id, "Assinem.")

        assertEquals(ResultadoEnvio.Invalido(listOf(ProblemaEnvio(ErroEnvio.SEM_SIGNATARIOS))), resultado)
        assertEquals(StatusDocumento.RASCUNHO, statusDe(admin, id))
        assertTrue(ambiente.gateway.solicitacoes.isEmpty())
    }

    @Test
    fun `signatario sem campo impede o envio`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val davi = ambiente.criarSignatario("Davi Melo")
        val id = documentos.criarRascunho(admin.id, "Contrato", ambiente.arquivo(), 1L)
        documentos.definirSignatarios(
            id,
            admin.id,
            listOf(
                SelecaoSignatario(carla.id, PapelSignatario.CONTRATANTE),
                SelecaoSignatario(davi.id, PapelSignatario.CONTRATADO),
            ),
        )
        documentos.salvarCampos(id, admin.id, listOf(NovoCampo(carla.id, 0, AreaNormalizada(0.1f, 0.1f, 0.3f, 0.1f))))

        val resultado = enviar(id, "Assinem.")

        assertEquals(
            ResultadoEnvio.Invalido(listOf(ProblemaEnvio(ErroEnvio.SIGNATARIO_SEM_CAMPO, listOf("Davi Melo")))),
            resultado,
        )
        assertEquals(StatusDocumento.RASCUNHO, statusDe(admin, id))
        assertTrue(titulosVistosPor(carla).isEmpty())
    }

    @Test
    fun `mensagem em branco impede o envio`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val id = ambiente.rascunhoPronto(admin, ambiente.criarSignatario("Carla Dias"))

        val resultado = enviar(id, "   ")

        assertEquals(ResultadoEnvio.Invalido(listOf(ProblemaEnvio(ErroEnvio.MENSAGEM_VAZIA))), resultado)
    }

    @Test
    fun `falha no disparo nao registra nada e a nova tentativa envia uma unica vez`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val id = ambiente.rascunhoPronto(admin, carla)
        ambiente.gateway.falhasRestantes = 1

        val primeira = enviar(id, "Assine.")

        assertEquals(ResultadoEnvio.FalhaNoEnvio, primeira)
        assertEquals(StatusDocumento.RASCUNHO, statusDe(admin, id))
        assertEquals(listOf(TipoEvento.CRIADO), eventosDe(admin, id))
        assertTrue(titulosVistosPor(carla).isEmpty())

        val segunda = enviar(id, "Assine.")

        assertEquals(ResultadoEnvio.Enviado(repetido = false), segunda)
        assertEquals(listOf(TipoEvento.CRIADO, TipoEvento.ENVIADO), eventosDe(admin, id))
        assertEquals(1, documentos.observarDoSignatario(carla.id).first().size)
        assertEquals(1, ambiente.banco.documentoSignatarioDao().listarDoDocumento(id).size)
    }

    @Test
    fun `tentativas do mesmo documento usam a mesma chave de idempotencia`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val id = ambiente.rascunhoPronto(admin, ambiente.criarSignatario("Carla Dias"))
        val outro = ambiente.rascunhoPronto(admin, ambiente.criarSignatario("Davi Melo"), titulo = "Outro")
        ambiente.gateway.falhasRestantes = 2

        enviar(id, "Assine.")
        enviar(id, "Assine.")
        enviar(id, "Assine.")
        enviar(outro, "Assine.")

        val chaves = ambiente.gateway.solicitacoes.map { it.chaveIdempotencia }
        assertEquals(4, chaves.size)
        assertEquals(1, chaves.take(3).toSet().size)
        assertTrue(chaves[3] != chaves[0])
    }

    @Test
    fun `enviar de novo um documento ja enviado nao duplica nada`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val id = ambiente.rascunhoPronto(admin, carla)
        ambiente.relogio.instante = 5_000L
        enviar(id, "Assine.")
        ambiente.relogio.instante = 9_000L

        val repeticao = enviar(id, "Outra mensagem.")

        assertEquals(ResultadoEnvio.Enviado(repetido = true), repeticao)
        val documento = documentos.buscarDetalheDoAdmin(id, admin.id)!!.documento
        assertEquals(5_000L, documento.enviadoEm)
        assertEquals("Assine.", documento.mensagemConvite)
        assertEquals(listOf(TipoEvento.CRIADO, TipoEvento.ENVIADO), eventosDe(admin, id))
        assertEquals(1, ambiente.gateway.solicitacoes.size)
        assertEquals(1, documentos.observarDoSignatario(carla.id).first().size)
    }

    @Test
    fun `registro de envio e idempotente tambem direto no repositorio`() = runTest {
        val admin = ambiente.criarAdmin()
        val id = ambiente.rascunhoPronto(admin, ambiente.criarSignatario("Carla Dias"))

        val primeiro = documentos.registrarEnvio(id, admin.id, "Assine.", 100L)
        val segundo = documentos.registrarEnvio(id, admin.id, "Assine.", 200L)

        assertEquals(ResultadoRegistroEnvio.REGISTRADO, primeiro)
        assertEquals(ResultadoRegistroEnvio.JA_ESTAVA_REGISTRADO, segundo)
        assertEquals(1, eventosDe(admin, id).count { it == TipoEvento.ENVIADO })
        assertEquals(100L, documentos.buscarDetalheDoAdmin(id, admin.id)!!.documento.enviadoEm)
    }

    @Test
    fun `repositorio recusa registrar envio de rascunho incompleto`() = runTest {
        val admin = ambiente.criarAdmin()
        val id = documentos.criarRascunho(admin.id, "Contrato", ambiente.arquivo(), 1L)

        try {
            documentos.registrarEnvio(id, admin.id, "Assine.", 100L)
            fail("deveria ter recusado")
        } catch (e: EnvioInvalidoException) {
            assertEquals(listOf(ErroEnvio.SEM_SIGNATARIOS), e.problemas.map { it.erro })
        }
        assertEquals(StatusDocumento.RASCUNHO, statusDe(admin, id))
    }

    @Test
    fun `documento enviado nao aceita mais alteracoes`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val davi = ambiente.criarSignatario("Davi Melo")
        val id = ambiente.rascunhoPronto(admin, carla)
        enviar(id, "Assine.")
        val antes = documentos.buscarDetalheDoAdmin(id, admin.id)

        val alteracoes: List<suspend () -> Unit> = listOf(
            { documentos.atualizarRascunho(id, admin.id, "Outro título", null) },
            { documentos.atualizarRascunho(id, admin.id, "Contrato", ambiente.arquivo("outro.pdf")) },
            { documentos.definirSignatarios(id, admin.id, listOf(SelecaoSignatario(davi.id, PapelSignatario.TESTEMUNHA))) },
            { documentos.salvarCampos(id, admin.id, emptyList()) },
            { documentos.salvarMensagemConvite(id, admin.id, "Outra") },
        )
        alteracoes.forEachIndexed { indice, alterar ->
            try {
                alterar()
                fail("a alteração $indice deveria ter sido recusada")
            } catch (e: DocumentoNaoEditavelException) {
                // esperado
            }
        }

        assertEquals(antes, documentos.buscarDetalheDoAdmin(id, admin.id))
    }

    @Test
    fun `signatario nunca ve rascunho nem documento enviado a outra pessoa`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val davi = ambiente.criarSignatario("Davi Melo")
        ambiente.rascunhoPronto(admin, carla, davi, titulo = "Ainda rascunho")
        val soDaCarla = ambiente.rascunhoPronto(admin, carla, titulo = "Só da Carla")
        val dosDois = ambiente.rascunhoPronto(admin, carla, davi, titulo = "Dos dois")
        enviar(soDaCarla, "Assine.")
        enviar(dosDois, "Assinem.")

        assertEquals(setOf("Só da Carla", "Dos dois"), titulosVistosPor(carla).toSet())
        assertEquals(listOf("Dos dois"), titulosVistosPor(davi))
        assertTrue(titulosVistosPor(admin).isEmpty())
    }

    @Test
    fun `admin nao envia documento de outro admin`() = runTest {
        val dona = ambiente.criarAdmin("Admin Dona")
        val intruso = ambiente.criarAdmin("Admin Intruso").also { ambiente.entrarComo(it) }
        val id = ambiente.rascunhoPronto(dona, ambiente.criarSignatario("Carla Dias"))

        try {
            enviar(id, "Assine.")
            fail("deveria ter recusado")
        } catch (e: DocumentoNaoEncontradoException) {
            // esperado
        }
        try {
            documentos.registrarEnvio(id, intruso.id, "Assine.", 1L)
            fail("deveria ter recusado")
        } catch (e: DocumentoNaoEncontradoException) {
            // esperado
        }

        assertEquals(StatusDocumento.RASCUNHO, statusDe(dona, id))
        assertTrue(documentos.observarEventos(id, intruso.id).first().isEmpty())
        assertNull(documentos.buscarDetalheDoAdmin(id, intruso.id))
    }
}
