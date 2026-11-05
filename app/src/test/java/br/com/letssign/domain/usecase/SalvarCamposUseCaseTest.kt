package br.com.letssign.domain.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.letssign.apoio.AmbienteDeTeste
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.domain.model.Usuario
import br.com.letssign.domain.regra.AreaNormalizada
import br.com.letssign.domain.regra.GeometriaCampo
import br.com.letssign.domain.repository.NovoCampo
import br.com.letssign.domain.repository.SelecaoSignatario
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SalvarCamposUseCaseTest {

    private val ambiente = AmbienteDeTeste()
    private val salvarCampos = SalvarCamposUseCase(ambiente.usuarioLogado, ambiente.documentos)
    private val area = AreaNormalizada(0.1f, 0.1f, 0.3f, 0.1f)

    @After
    fun fechar() = ambiente.fechar()

    private suspend fun rascunhoCom(admin: Usuario, vararg signatarios: Usuario, paginas: Int = 2): Long {
        val id = ambiente.documentos.criarRascunho(admin.id, "Contrato", ambiente.arquivo(paginas = paginas), 1L)
        ambiente.documentos.definirSignatarios(
            id,
            admin.id,
            signatarios.map { SelecaoSignatario(it.id, PapelSignatario.CONTRATANTE) },
        )
        return id
    }

    private suspend fun camposDe(admin: Usuario, documentoId: Long) =
        ambiente.documentos.buscarDetalheDoAdmin(documentoId, admin.id)!!.signatarios.flatMap { it.campos }

    @Test
    fun `um signatario pode ter varios campos, em paginas diferentes`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val id = rascunhoCom(admin, carla)

        salvarCampos(id, listOf(NovoCampo(carla.id, 0, area), NovoCampo(carla.id, 1, area.copy(y = 0.8f))))

        val campos = camposDe(admin, id)
        assertEquals(listOf(0, 1), campos.map { it.pagina })
        assertEquals(0.8f, campos[1].y, 1e-6f)
    }

    @Test
    fun `salvar de novo substitui os campos anteriores`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val id = rascunhoCom(admin, carla)
        salvarCampos(id, listOf(NovoCampo(carla.id, 0, area), NovoCampo(carla.id, 1, area)))

        salvarCampos(id, listOf(NovoCampo(carla.id, 1, area.copy(x = 0.5f))))

        val campo = camposDe(admin, id).single()
        assertEquals(1, campo.pagina)
        assertEquals(0.5f, campo.x, 1e-6f)
    }

    @Test
    fun `campo que ultrapassa a pagina e trazido para dentro antes de gravar`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val id = rascunhoCom(admin, carla)

        salvarCampos(id, listOf(NovoCampo(carla.id, 0, AreaNormalizada(0.9f, 0.97f, 0.3f, 0.1f))))

        val campo = camposDe(admin, id).single()
        assertTrue(GeometriaCampo.dentroDaPagina(campo.area))
        assertEquals(0.7f, campo.x, 1e-5f)
        assertEquals(0.9f, campo.y, 1e-5f)
    }

    @Test
    fun `repositorio recusa campo fora da pagina mesmo sem passar pelo caso de uso`() = runTest {
        val admin = ambiente.criarAdmin()
        val carla = ambiente.criarSignatario("Carla Dias")
        val id = rascunhoCom(admin, carla)

        try {
            ambiente.documentos.salvarCampos(
                id,
                admin.id,
                listOf(NovoCampo(carla.id, 0, AreaNormalizada(0.9f, 0.1f, 0.3f, 0.1f))),
            )
            fail("deveria ter recusado")
        } catch (e: IllegalArgumentException) {
            // esperado
        }
        assertTrue(camposDe(admin, id).isEmpty())
    }

    @Test
    fun `campo de quem nao e signatario do documento e recusado e nada e gravado`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val davi = ambiente.criarSignatario("Davi Melo")
        val id = rascunhoCom(admin, carla)
        salvarCampos(id, listOf(NovoCampo(carla.id, 0, area)))

        try {
            salvarCampos(id, listOf(NovoCampo(carla.id, 0, area), NovoCampo(davi.id, 0, area)))
            fail("deveria ter recusado")
        } catch (e: IllegalArgumentException) {
            // esperado
        }

        assertEquals(1, camposDe(admin, id).size)
    }

    @Test
    fun `campo em pagina que nao existe e recusado`() = runTest {
        val admin = ambiente.criarAdmin().also { ambiente.entrarComo(it) }
        val carla = ambiente.criarSignatario("Carla Dias")
        val id = rascunhoCom(admin, carla, paginas = 2)

        listOf(-1, 2).forEach { pagina ->
            try {
                salvarCampos(id, listOf(NovoCampo(carla.id, pagina, area)))
                fail("deveria ter recusado a página $pagina")
            } catch (e: IllegalArgumentException) {
                // esperado
            }
        }
    }
}
