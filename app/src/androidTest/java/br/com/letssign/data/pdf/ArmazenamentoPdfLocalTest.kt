package br.com.letssign.data.pdf

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.letssign.domain.repository.MotivoFalhaPdf
import br.com.letssign.domain.repository.ResultadoImportacao
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArmazenamentoPdfLocalTest {

    private val contexto: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val armazenamento = ArmazenamentoPdfLocal(contexto, Dispatchers.IO)
    private val origem = File(contexto.cacheDir, "origem-teste")
    private val pastaDocumentos = File(contexto.filesDir, "documentos")

    @Before
    fun preparar() {
        PdfBox.garantirIniciado(contexto)
        origem.deleteRecursively()
        origem.mkdirs()
        pastaDocumentos.deleteRecursively()
    }

    @After
    fun limpar() {
        origem.deleteRecursively()
        pastaDocumentos.deleteRecursively()
    }

    @Test
    fun importaPdfValidoParaDentroDoApp() = runBlocking {
        val pdf = criarPdf("contrato.pdf", paginas = 3)

        val resultado = armazenamento.importar(Uri.fromFile(pdf).toString())

        val arquivo = (resultado as ResultadoImportacao.Sucesso).arquivo
        assertEquals("contrato.pdf", arquivo.nomeArquivo)
        assertEquals(3, arquivo.totalPaginas)
        val copia = File(arquivo.caminho)
        assertEquals(pastaDocumentos, copia.parentFile)
        assertEquals(pdf.length(), copia.length())
    }

    @Test
    fun recusaArquivoQueNaoEPdf() = runBlocking {
        val falso = File(origem, "falso.pdf").apply { writeText("apenas um texto qualquer") }

        assertFalha(MotivoFalhaPdf.NAO_E_PDF, armazenamento.importar(Uri.fromFile(falso).toString()))
    }

    @Test
    fun recusaArquivoVazio() = runBlocking {
        val vazio = File(origem, "vazio.pdf").apply { createNewFile() }

        assertFalha(MotivoFalhaPdf.NAO_E_PDF, armazenamento.importar(Uri.fromFile(vazio).toString()))
    }

    @Test
    fun recusaPdfTruncado() = runBlocking {
        val inteiro = criarPdf("inteiro.pdf", paginas = 2)
        val bytes = inteiro.readBytes()
        val truncado = File(origem, "truncado.pdf").apply { writeBytes(bytes.copyOf(bytes.size / 3)) }

        val resultado = armazenamento.importar(Uri.fromFile(truncado).toString())

        val motivo = (resultado as ResultadoImportacao.Falha).motivo
        assertTrue(
            "motivo inesperado: $motivo",
            motivo == MotivoFalhaPdf.CORROMPIDO || motivo == MotivoFalhaPdf.SEM_PAGINAS,
        )
    }

    @Test
    fun recusaPdfComSenhaDeAbertura() = runBlocking {
        val protegido = criarPdf("protegido.pdf", paginas = 1, senhaUsuario = "segredo")

        assertFalha(
            MotivoFalhaPdf.PROTEGIDO_POR_SENHA,
            armazenamento.importar(Uri.fromFile(protegido).toString()),
        )
    }

    @Test
    fun recusaPdfComRestricaoDeEdicao() = runBlocking {
        val restrito = criarPdf("restrito.pdf", paginas = 1, senhaUsuario = "")

        assertFalha(
            MotivoFalhaPdf.PROTEGIDO_POR_SENHA,
            armazenamento.importar(Uri.fromFile(restrito).toString()),
        )
    }

    @Test
    fun recusaOrigemInexistente() = runBlocking {
        val sumiu = File(origem, "nao-existe.pdf")

        assertFalha(
            MotivoFalhaPdf.NAO_FOI_POSSIVEL_LER,
            armazenamento.importar(Uri.fromFile(sumiu).toString()),
        )
    }

    @Test
    fun removerSoApagaArquivosDaPastaDeDocumentos() = runBlocking {
        val externo = criarPdf("externo.pdf", paginas = 1)
        val importado = (armazenamento.importar(Uri.fromFile(externo).toString())
            as ResultadoImportacao.Sucesso).arquivo

        armazenamento.remover(externo.absolutePath)
        armazenamento.remover(importado.caminho)

        assertTrue(externo.exists())
        assertFalse(File(importado.caminho).exists())
    }

    private fun assertFalha(esperado: MotivoFalhaPdf, resultado: ResultadoImportacao) {
        assertEquals(ResultadoImportacao.Falha(esperado), resultado)
        assertTrue(
            "a cópia recusada deveria ter sido apagada",
            pastaDocumentos.listFiles().isNullOrEmpty(),
        )
    }

    private fun criarPdf(nome: String, paginas: Int, senhaUsuario: String? = null): File {
        val arquivo = File(origem, nome)
        PDDocument().use { documento ->
            repeat(paginas) { documento.addPage(PDPage()) }
            if (senhaUsuario != null) {
                val politica = StandardProtectionPolicy("dono", senhaUsuario, AccessPermission())
                politica.encryptionKeyLength = 128
                documento.protect(politica)
            }
            documento.save(arquivo)
        }
        return arquivo
    }
}
