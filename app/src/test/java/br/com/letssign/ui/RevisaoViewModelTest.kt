package br.com.letssign.ui

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.letssign.apoio.AmbienteDeTeste
import br.com.letssign.domain.model.TipoEvento
import br.com.letssign.domain.regra.ErroEnvio
import br.com.letssign.domain.usecase.EnviarDocumentoUseCase
import br.com.letssign.ui.navigation.Rotas
import br.com.letssign.ui.revisao.FalhaRevisao
import br.com.letssign.ui.revisao.RevisaoUiState
import br.com.letssign.ui.revisao.RevisaoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RevisaoViewModelTest {

    private val ambiente = AmbienteDeTeste()

    @Before
    fun preparar() = Dispatchers.setMain(Dispatchers.Unconfined)

    @After
    fun limpar() {
        Dispatchers.resetMain()
        ambiente.fechar()
    }

    private fun viewModel(documentoId: Long) = RevisaoViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Rotas.ARG_DOCUMENTO_ID to documentoId)),
        usuarioLogado = ambiente.usuarioLogado,
        documentos = ambiente.documentos,
        enviarDocumento = EnviarDocumentoUseCase(
            ambiente.usuarioLogado,
            ambiente.documentos,
            ambiente.gateway,
            ambiente.relogio,
        ),
    )

    private fun RevisaoViewModel.aguardar(condicao: (RevisaoUiState) -> Boolean) =
        runBlocking { estado.first(condicao) }

    @Test
    fun `falha no envio orienta a tentar de novo e a nova tentativa conclui sem duplicar`() {
        val (adminId, documentoId) = runBlocking {
            val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
            admin.id to ambiente.rascunhoPronto(admin, ambiente.criarSignatario("Carla Dias"))
        }
        ambiente.gateway.falhasRestantes = 1
        val viewModel = viewModel(documentoId)
        viewModel.definirMensagemPadrao("Assine, por favor.")
        viewModel.aguardar { !it.carregando }

        viewModel.enviar()
        val comFalha = viewModel.aguardar { !it.enviando && it.falha != null }

        assertEquals(FalhaRevisao.ENVIO, comFalha.falha)
        assertFalse(comFalha.enviado)

        viewModel.enviar()
        val enviado = viewModel.aguardar { it.enviado }

        assertNull(enviado.falha)
        val eventos = runBlocking {
            ambiente.documentos.observarEventos(documentoId, adminId).first().map { it.evento.tipo }
        }
        assertEquals(listOf(TipoEvento.CRIADO, TipoEvento.ENVIADO), eventos)
    }

    @Test
    fun `mensagem padrao e sugerida e pode ser editada`() {
        val documentoId = runBlocking {
            val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
            ambiente.rascunhoPronto(admin, ambiente.criarSignatario("Carla Dias"))
        }
        val viewModel = viewModel(documentoId)
        viewModel.definirMensagemPadrao("Assine, por favor.")

        assertEquals("Assine, por favor.", viewModel.aguardar { !it.carregando }.mensagem)

        viewModel.aoMudarMensagem("")
        viewModel.enviar()
        val invalido = viewModel.aguardar { !it.enviando && it.problemas.isNotEmpty() }

        assertEquals(listOf(ErroEnvio.MENSAGEM_VAZIA), invalido.problemas.map { it.erro })
        assertTrue(ambiente.gateway.solicitacoes.isEmpty())
    }
}
