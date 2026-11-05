package br.com.letssign.domain.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.letssign.apoio.AmbienteDeTeste
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.domain.model.SemPermissaoException
import br.com.letssign.domain.model.SessaoEncerradaException
import br.com.letssign.domain.repository.SelecaoSignatario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RascunhoUseCasesTest {

    private val ambiente = AmbienteDeTeste()
    private val salvarRascunho = SalvarRascunhoUseCase(
        ambiente.usuarioLogado,
        ambiente.documentos,
        ambiente.armazenamento,
        ambiente.relogio,
    )
    private val definirSignatarios = DefinirSignatariosUseCase(ambiente.usuarioLogado, ambiente.documentos)

    @After
    fun fechar() = ambiente.fechar()

    @Test
    fun `rascunho novo exige titulo e arquivo`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }

        val resultado = salvarRascunho(null, "   ", null)

        assertEquals(ResultadoSalvarRascunho.Invalido(tituloInvalido = true, semArquivo = true), resultado)
        assertTrue(ambiente.documentos.observarDoAdmin(admin.id).first().isEmpty())
    }

    @Test
    fun `titulo acima do limite e recusado`() = runTest {
        ambiente.criarAdmin().also { ambiente.entrarComo(it) }

        val resultado = salvarRascunho(null, "a".repeat(121), ambiente.arquivo())

        assertEquals(ResultadoSalvarRascunho.Invalido(tituloInvalido = true, semArquivo = false), resultado)
    }

    @Test
    fun `rascunho fica gravado para o admin logado`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        ambiente.relogio.instante = 42L

        val resultado = salvarRascunho(null, "  Contrato de aluguel ", ambiente.arquivo())

        val id = (resultado as ResultadoSalvarRascunho.Sucesso).documentoId
        val documento = ambiente.documentos.buscarDetalheDoAdmin(id, admin.id)!!.documento
        assertEquals("Contrato de aluguel", documento.titulo)
        assertEquals(42L, documento.criadoEm)
    }

    @Test
    fun `trocar o pdf apaga o arquivo anterior do armazenamento`() = runTest {
        ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val antigo = ambiente.arquivo("antigo.pdf")
        val id = (salvarRascunho(null, "Contrato", antigo) as ResultadoSalvarRascunho.Sucesso).documentoId

        salvarRascunho(id, "Contrato", ambiente.arquivo("novo.pdf"))

        assertEquals(listOf(antigo.caminho), ambiente.armazenamento.removidos)
    }

    @Test
    fun `editar so o titulo mantem o arquivo`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val id = (salvarRascunho(null, "Contrato", ambiente.arquivo("a.pdf")) as ResultadoSalvarRascunho.Sucesso)
            .documentoId

        salvarRascunho(id, "Contrato revisado", null)

        val documento = ambiente.documentos.buscarDetalheDoAdmin(id, admin.id)!!.documento
        assertEquals("Contrato revisado", documento.titulo)
        assertEquals("a.pdf", documento.nomeArquivo)
        assertTrue(ambiente.armazenamento.removidos.isEmpty())
    }

    @Test
    fun `signatario nao cria documento`() = runTest {
        ambiente.criarSignatario("Carla Dias").also { ambiente.entrarComo(it) }

        try {
            salvarRascunho(null, "Contrato", ambiente.arquivo())
            fail("deveria ter recusado")
        } catch (e: SemPermissaoException) {
            // esperado
        }
    }

    @Test
    fun `sem sessao nao cria documento`() = runTest {
        try {
            salvarRascunho(null, "Contrato", ambiente.arquivo())
            fail("deveria ter recusado")
        } catch (e: SessaoEncerradaException) {
            // esperado
        }
    }

    @Test
    fun `selecao vazia nao e gravada`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val id = (salvarRascunho(null, "Contrato", ambiente.arquivo()) as ResultadoSalvarRascunho.Sucesso)
            .documentoId
        definirSignatarios(id, listOf(SelecaoSignatario(carla.id, PapelSignatario.CONTRATANTE)))

        val gravou = definirSignatarios(id, emptyList())

        assertFalse(gravou)
        assertEquals(1, ambiente.documentos.buscarDetalheDoAdmin(id, admin.id)!!.signatarios.size)
    }
}
