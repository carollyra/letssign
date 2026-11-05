package br.com.letssign.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.letssign.domain.model.CampoAssinatura
import br.com.letssign.domain.repository.FalhaAoAssinarPdfException
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssinadorPdfLocalTest {

    private val contexto: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val pastaDocumentos = PastaDocumentos(contexto)
    private val assinador = AssinadorPdfLocal(contexto, pastaDocumentos, Dispatchers.IO)
    private val origem = File(contexto.cacheDir, "assinador-teste")

    @Before
    fun preparar() {
        PdfBox.garantirIniciado(contexto)
        origem.deleteRecursively()
        origem.mkdirs()
        pastaDocumentos.pasta.deleteRecursively()
    }

    @After
    fun limpar() {
        origem.deleteRecursively()
        pastaDocumentos.pasta.deleteRecursively()
    }

    @Test
    fun assinaturaApareceNoCampoEDePeSemRotacao() = conferirRotacao(0)

    @Test
    fun assinaturaApareceNoCampoEDePeComRotacaoDe90() = conferirRotacao(90)

    @Test
    fun assinaturaApareceNoCampoEDePeComRotacaoDe180() = conferirRotacao(180)

    @Test
    fun assinaturaApareceNoCampoEDePeComRotacaoDe270() = conferirRotacao(270)

    @Test
    fun arquivoOriginalNaoEAlterado() = runBlocking {
        val pdf = criarPdf(rotacoes = listOf(0))
        val antes = pdf.readBytes()

        val assinado = assinador.aplicar(pdf.absolutePath, assinaturaPng(), listOf(campo(pagina = 0)))

        assertNotEquals(pdf.absolutePath, assinado)
        assertEquals(pastaDocumentos.pasta, File(assinado).parentFile)
        assertArrayEquals(antes, pdf.readBytes())
        assertTrue(File(assinado).length() > antes.size)
    }

    @Test
    fun assinaTodosOsCamposEmPaginasDiferentes() = runBlocking {
        val pdf = criarPdf(rotacoes = listOf(0, 0, 90))
        val campos = listOf(
            campo(pagina = 0),
            campo(pagina = 0, x = 0.5f, y = 0.7f),
            campo(pagina = 2),
        )

        val assinado = assinador.aplicar(pdf.absolutePath, assinaturaPng(), campos)

        campos.forEach { campo ->
            val pagina = renderizar(assinado, campo.pagina)
            assertTrue(
                "campo da página ${campo.pagina} em (${campo.x}, ${campo.y}) deveria ter tinta",
                pagina.temTintaEm(campo),
            )
        }
        assertTrue("página sem campo deveria continuar em branco", renderizar(assinado, 1).todaBranca())
    }

    @Test
    fun imagemInvalidaNaoGeraArquivo() = runBlocking {
        val pdf = criarPdf(rotacoes = listOf(0))

        try {
            assinador.aplicar(pdf.absolutePath, byteArrayOf(1, 2, 3), listOf(campo(pagina = 0)))
            fail("deveria ter recusado")
        } catch (e: FalhaAoAssinarPdfException) {
            // esperado
        }
        assertTrue(pastaDocumentos.pasta.listFiles().isNullOrEmpty())
    }

    @Test
    fun pdfDeOrigemIlegivelNaoGeraArquivo() = runBlocking {
        val quebrado = File(origem, "quebrado.pdf").apply { writeText("nada aqui") }

        try {
            assinador.aplicar(quebrado.absolutePath, assinaturaPng(), listOf(campo(pagina = 0)))
            fail("deveria ter recusado")
        } catch (e: FalhaAoAssinarPdfException) {
            // esperado
        }
        assertTrue(pastaDocumentos.pasta.listFiles().isNullOrEmpty())
    }

    /**
     * A imagem de teste só tem tinta no quadrante superior esquerdo. Depois de gravada, a
     * página exibida precisa mostrar tinta nesse mesmo quadrante do campo e em nenhum outro,
     * o que só acontece se posição, escala e orientação estiverem certas.
     */
    private fun conferirRotacao(rotacao: Int) = runBlocking {
        val pdf = criarPdf(rotacoes = listOf(rotacao))
        val (larguraExibida, alturaExibida) =
            if (rotacao % 180 == 0) LARGURA_RECORTE to ALTURA_RECORTE else ALTURA_RECORTE to LARGURA_RECORTE
        // Campo com a mesma proporção 2:1 da imagem, para ela ocupar o campo inteiro.
        val largura = 0.4f
        val altura = largura * larguraExibida / (2f * alturaExibida)
        val campo = campo(pagina = 0, x = 0.15f, y = 0.25f, largura = largura, altura = altura)

        val assinado = assinador.aplicar(pdf.absolutePath, assinaturaPng(), listOf(campo))
        val pagina = renderizar(assinado, 0)

        assertEquals(
            "proporção da página exibida",
            larguraExibida / alturaExibida,
            pagina.width / pagina.height.toFloat(),
            0.01f,
        )
        fun ponto(fx: Float, fy: Float) = campo.x + campo.largura * fx to campo.y + campo.altura * fy
        val (xTinta, yTinta) = ponto(0.25f, 0.25f)
        assertTrue("quadrante superior esquerdo deveria ter tinta", pagina.escuro(xTinta, yTinta))
        listOf(
            "superior direito" to ponto(0.75f, 0.25f),
            "inferior esquerdo" to ponto(0.25f, 0.75f),
            "inferior direito" to ponto(0.75f, 0.75f),
            "à esquerda do campo" to ponto(-0.15f, 0.25f),
            "acima do campo" to ponto(0.25f, -0.4f),
            "à direita do campo" to ponto(1.15f, 0.5f),
            "abaixo do campo" to ponto(0.5f, 1.4f),
        ).forEach { (onde, posicao) ->
            assertTrue("$onde deveria estar em branco", !pagina.escuro(posicao.first, posicao.second))
        }
        // Os limites da tinta coincidem com os do quadrante: nem deslocada nem esticada.
        assertTrue(pagina.escuro(ponto(0.04f, 0.08f).first, ponto(0.04f, 0.08f).second))
        assertTrue(pagina.escuro(ponto(0.46f, 0.42f).first, ponto(0.46f, 0.42f).second))
        assertTrue(!pagina.escuro(ponto(0.54f, 0.42f).first, ponto(0.54f, 0.42f).second))
        assertTrue(!pagina.escuro(ponto(0.46f, 0.58f).first, ponto(0.46f, 0.58f).second))
    }

    private fun campo(
        pagina: Int,
        x: Float = 0.1f,
        y: Float = 0.1f,
        largura: Float = 0.3f,
        altura: Float = 0.1f,
    ) = CampoAssinatura(0, 1, pagina, x, y, largura, altura)

    private fun Bitmap.escuro(u: Float, v: Float): Boolean {
        val pixel = getPixel(
            (u * width).roundToInt().coerceIn(0, width - 1),
            (v * height).roundToInt().coerceIn(0, height - 1),
        )
        return Color.red(pixel) < 128 && Color.green(pixel) < 128 && Color.blue(pixel) < 128
    }

    private fun Bitmap.temTintaEm(campo: CampoAssinatura): Boolean {
        for (passoX in 1..19) {
            for (passoY in 1..19) {
                if (escuro(campo.x + campo.largura * passoX / 20f, campo.y + campo.altura * passoY / 20f)) {
                    return true
                }
            }
        }
        return false
    }

    private fun Bitmap.todaBranca(): Boolean {
        for (x in 0 until width step 7) {
            for (y in 0 until height step 7) {
                if (getPixel(x, y) != Color.WHITE) return false
            }
        }
        return true
    }

    private fun renderizar(caminho: String, indice: Int): Bitmap =
        ParcelFileDescriptor.open(File(caminho), ParcelFileDescriptor.MODE_READ_ONLY).use { descritor ->
            PdfRenderer(descritor).use { renderizador ->
                renderizador.openPage(indice).use { pagina ->
                    Bitmap.createBitmap(pagina.width * 2, pagina.height * 2, Bitmap.Config.ARGB_8888).also {
                        it.eraseColor(Color.WHITE)
                        pagina.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }

    /** Páginas com a área visível deslocada em relação à origem, como em PDFs recortados. */
    private fun criarPdf(rotacoes: List<Int>): File {
        val arquivo = File(origem, "base-${rotacoes.joinToString("-")}.pdf")
        PDDocument().use { documento ->
            rotacoes.forEach { rotacao ->
                val pagina = PDPage(PDRectangle(0f, 0f, 400f, 600f))
                pagina.cropBox = PDRectangle(20f, 30f, LARGURA_RECORTE, ALTURA_RECORTE)
                pagina.rotation = rotacao
                documento.addPage(pagina)
            }
            documento.save(arquivo)
        }
        return arquivo
    }

    private fun assinaturaPng(): ByteArray {
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawRect(0f, 0f, 100f, 50f, Paint().apply { color = Color.BLACK })
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
    }

    private companion object {
        const val LARGURA_RECORTE = 360f
        const val ALTURA_RECORTE = 540f
    }
}
