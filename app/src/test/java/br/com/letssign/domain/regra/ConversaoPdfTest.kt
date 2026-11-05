package br.com.letssign.domain.regra

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversaoPdfTest {

    // Campo no quadrante superior esquerdo da página exibida.
    private val campo = AreaNormalizada(x = 0.1f, y = 0.2f, largura = 0.3f, altura = 0.1f)

    private fun a4(rotacao: Int) = CaixaPagina(0f, 0f, 595f, 842f, rotacao)

    @Test
    fun `sem rotacao a origem vai para o canto inferior esquerdo do campo`() {
        val matriz = ConversaoPdf.matrizDoCampo(campo, a4(0))

        // y do PDF cresce para cima: a base do campo fica a (1 - 0.3) da altura.
        assertMatriz(MatrizPdf(178.5f, 0f, 0f, 84.2f, 59.5f, 589.4f), matriz)
    }

    @Test
    fun `pagina girada 90 graus`() {
        val matriz = ConversaoPdf.matrizDoCampo(campo, a4(90))

        // Exibida em paisagem (842 x 595). A largura do campo corre ao longo do eixo y do PDF.
        assertMatriz(MatrizPdf(0f, 252.6f, -59.5f, 0f, 178.5f, 84.2f), matriz)
    }

    @Test
    fun `pagina girada 180 graus`() {
        val matriz = ConversaoPdf.matrizDoCampo(campo, a4(180))

        assertMatriz(MatrizPdf(-178.5f, 0f, 0f, -84.2f, 535.5f, 252.6f), matriz)
    }

    @Test
    fun `pagina girada 270 graus`() {
        val matriz = ConversaoPdf.matrizDoCampo(campo, a4(270))

        assertMatriz(MatrizPdf(0f, -252.6f, 59.5f, 0f, 416.5f, 757.8f), matriz)
    }

    @Test
    fun `caixa de recorte deslocada soma o deslocamento`() {
        val recorte = CaixaPagina(x = 20f, y = 30f, largura = 400f, altura = 600f, rotacao = 0)

        val matriz = ConversaoPdf.matrizDoCampo(campo, recorte)

        assertMatriz(MatrizPdf(120f, 0f, 0f, 60f, 60f, 450f), matriz)
    }

    @Test
    fun `os quatro cantos da imagem caem nos quatro cantos do campo em qualquer rotacao`() {
        val recorte = CaixaPagina(x = 15f, y = 25f, largura = 500f, altura = 700f, rotacao = 0)
        listOf(0, 90, 180, 270).forEach { rotacao ->
            val caixa = recorte.copy(rotacao = rotacao)
            val matriz = ConversaoPdf.matrizDoCampo(campo, caixa)

            // No quadrado unitário do PDF, (0,1) é o topo esquerdo da imagem e (1,0) a base direita.
            assertExibido("topo esquerdo, rotação $rotacao", campo.x, campo.y, matriz.aplicar(0f, 1f), caixa)
            assertExibido("topo direito, rotação $rotacao", campo.direita, campo.y, matriz.aplicar(1f, 1f), caixa)
            assertExibido("base esquerda, rotação $rotacao", campo.x, campo.base, matriz.aplicar(0f, 0f), caixa)
            assertExibido("base direita, rotação $rotacao", campo.direita, campo.base, matriz.aplicar(1f, 0f), caixa)
        }
    }

    @Test
    fun `conversao de ponto e reversivel`() {
        listOf(0, 90, 180, 270).forEach { rotacao ->
            val caixa = CaixaPagina(10f, 20f, 300f, 800f, rotacao)
            val ponto = ConversaoPdf.exibidoParaPdf(0.25f, 0.6f, caixa)
            val (u, v) = ConversaoPdf.pdfParaExibido(ponto, caixa)

            assertEquals("u, rotação $rotacao", 0.25f, u, DELTA)
            assertEquals("v, rotação $rotacao", 0.6f, v, DELTA)
        }
    }

    @Test
    fun `canto superior esquerdo exibido corresponde ao canto certo do pdf em cada rotacao`() {
        assertPonto(PontoPdf(0f, 842f), ConversaoPdf.exibidoParaPdf(0f, 0f, a4(0)))
        assertPonto(PontoPdf(0f, 0f), ConversaoPdf.exibidoParaPdf(0f, 0f, a4(90)))
        assertPonto(PontoPdf(595f, 0f), ConversaoPdf.exibidoParaPdf(0f, 0f, a4(180)))
        assertPonto(PontoPdf(595f, 842f), ConversaoPdf.exibidoParaPdf(0f, 0f, a4(270)))
    }

    @Test
    fun `rotacao e normalizada para um dos quatro angulos`() {
        assertEquals(0, ConversaoPdf.rotacaoNormalizada(0))
        assertEquals(0, ConversaoPdf.rotacaoNormalizada(360))
        assertEquals(90, ConversaoPdf.rotacaoNormalizada(450))
        assertEquals(270, ConversaoPdf.rotacaoNormalizada(-90))
        assertEquals(180, ConversaoPdf.rotacaoNormalizada(-180))
    }

    @Test
    fun `pagina girada troca largura e altura exibidas`() {
        assertEquals(595f to 842f, ConversaoPdf.tamanhoExibido(a4(0)))
        assertEquals(842f to 595f, ConversaoPdf.tamanhoExibido(a4(90)))
        assertEquals(595f to 842f, ConversaoPdf.tamanhoExibido(a4(180)))
        assertEquals(842f to 595f, ConversaoPdf.tamanhoExibido(a4(270)))
    }

    @Test
    fun `assinatura larga encaixa pela largura e fica centralizada na vertical`() {
        // Campo de 200 x 200 pontos numa página de 1000 x 1000; imagem 4:1.
        val caixa = CaixaPagina(0f, 0f, 1000f, 1000f, 0)
        val quadrado = AreaNormalizada(0.1f, 0.1f, 0.2f, 0.2f)

        val encaixado = ConversaoPdf.encaixar(quadrado, proporcaoImagem = 4f, caixa = caixa)

        assertEquals(0.1f, encaixado.x, DELTA)
        assertEquals(0.2f, encaixado.largura, DELTA)
        assertEquals(0.05f, encaixado.altura, DELTA)
        assertEquals(0.175f, encaixado.y, DELTA)
    }

    @Test
    fun `assinatura alta encaixa pela altura e fica centralizada na horizontal`() {
        val caixa = CaixaPagina(0f, 0f, 1000f, 1000f, 0)
        val largo = AreaNormalizada(0.1f, 0.1f, 0.4f, 0.1f)

        val encaixado = ConversaoPdf.encaixar(largo, proporcaoImagem = 2f, caixa = caixa)

        assertEquals(0.1f, encaixado.altura, DELTA)
        assertEquals(0.2f, encaixado.largura, DELTA)
        assertEquals(0.2f, encaixado.x, DELTA)
        assertEquals(0.1f, encaixado.y, DELTA)
    }

    @Test
    fun `encaixe usa as medidas exibidas quando a pagina esta girada`() {
        // Girada 90°, a página exibida mede 842 x 595: um campo 0.2 x 0.2 tem 168.4 x 119 pontos.
        val quadradoNormalizado = AreaNormalizada(0.1f, 0.1f, 0.2f, 0.2f)

        val encaixado = ConversaoPdf.encaixar(quadradoNormalizado, proporcaoImagem = 1f, caixa = a4(90))

        assertEquals(119f, encaixado.largura * 842f, 0.01f)
        assertEquals(119f, encaixado.altura * 595f, 0.01f)
    }

    private fun assertExibido(rotulo: String, u: Float, v: Float, ponto: PontoPdf, caixa: CaixaPagina) {
        val (uObtido, vObtido) = ConversaoPdf.pdfParaExibido(ponto, caixa)
        assertEquals("$rotulo (u)", u, uObtido, DELTA)
        assertEquals("$rotulo (v)", v, vObtido, DELTA)
    }

    private fun assertPonto(esperado: PontoPdf, obtido: PontoPdf) {
        assertEquals(esperado.x, obtido.x, 0.01f)
        assertEquals(esperado.y, obtido.y, 0.01f)
    }

    private fun assertMatriz(esperada: MatrizPdf, obtida: MatrizPdf) {
        assertEquals("a", esperada.a, obtida.a, 0.01f)
        assertEquals("b", esperada.b, obtida.b, 0.01f)
        assertEquals("c", esperada.c, obtida.c, 0.01f)
        assertEquals("d", esperada.d, obtida.d, 0.01f)
        assertEquals("e", esperada.e, obtida.e, 0.01f)
        assertEquals("f", esperada.f, obtida.f, 0.01f)
    }

    private companion object {
        const val DELTA = 1e-4f
    }
}
