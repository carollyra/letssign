package br.com.letssign.domain.regra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometriaCampoTest {

    private val campo = AreaNormalizada(x = 0.2f, y = 0.5f, largura = 0.3f, altura = 0.1f)

    @Test
    fun `mover dentro da pagina preserva o tamanho`() {
        val movido = GeometriaCampo.mover(campo, 0.1f, -0.2f)

        assertArea(AreaNormalizada(0.3f, 0.3f, 0.3f, 0.1f), movido)
    }

    @Test
    fun `arraste para alem da borda direita e inferior para na borda`() {
        val movido = GeometriaCampo.mover(campo, 5f, 5f)

        assertArea(AreaNormalizada(0.7f, 0.9f, 0.3f, 0.1f), movido)
        assertTrue(GeometriaCampo.dentroDaPagina(movido))
    }

    @Test
    fun `arraste para alem da borda esquerda e superior para na origem`() {
        val movido = GeometriaCampo.mover(campo, -5f, -5f)

        assertArea(AreaNormalizada(0f, 0f, 0.3f, 0.1f), movido)
    }

    @Test
    fun `redimensionar nao passa da borda da pagina`() {
        val maior = GeometriaCampo.redimensionar(campo, 2f, 2f)

        assertArea(AreaNormalizada(0.2f, 0.5f, 0.8f, 0.5f), maior)
        assertTrue(GeometriaCampo.dentroDaPagina(maior))
    }

    @Test
    fun `redimensionar respeita o tamanho minimo`() {
        val menor = GeometriaCampo.redimensionar(campo, -2f, -2f)

        assertEquals(GeometriaCampo.LARGURA_MINIMA, menor.largura, DELTA)
        assertEquals(GeometriaCampo.ALTURA_MINIMA, menor.altura, DELTA)
        assertEquals(campo.x, menor.x, DELTA)
        assertEquals(campo.y, menor.y, DELTA)
    }

    @Test
    fun `limitar traz para dentro um campo que nasceu fora`() {
        val limitado = GeometriaCampo.limitar(AreaNormalizada(0.9f, -0.3f, 0.5f, 0.2f))

        assertArea(AreaNormalizada(0.5f, 0f, 0.5f, 0.2f), limitado)
    }

    @Test
    fun `limitar reduz campo maior que a pagina`() {
        val limitado = GeometriaCampo.limitar(AreaNormalizada(0.1f, 0.1f, 3f, 3f))

        assertArea(AreaNormalizada(0f, 0f, 1f, 1f), limitado)
    }

    @Test
    fun `valores invalidos viram um campo valido`() {
        val limitado = GeometriaCampo.limitar(AreaNormalizada(Float.NaN, Float.NaN, Float.NaN, Float.NaN))

        assertTrue(GeometriaCampo.dentroDaPagina(limitado))
    }

    @Test
    fun `dentroDaPagina recusa o que ultrapassa qualquer borda`() {
        assertTrue(GeometriaCampo.dentroDaPagina(AreaNormalizada(0f, 0f, 1f, 1f)))
        assertFalse(GeometriaCampo.dentroDaPagina(AreaNormalizada(-0.01f, 0f, 0.5f, 0.5f)))
        assertFalse(GeometriaCampo.dentroDaPagina(AreaNormalizada(0f, -0.01f, 0.5f, 0.5f)))
        assertFalse(GeometriaCampo.dentroDaPagina(AreaNormalizada(0.6f, 0f, 0.5f, 0.5f)))
        assertFalse(GeometriaCampo.dentroDaPagina(AreaNormalizada(0f, 0.6f, 0.5f, 0.5f)))
        assertFalse(GeometriaCampo.dentroDaPagina(AreaNormalizada(0.1f, 0.1f, 0f, 0.5f)))
    }

    @Test
    fun `ida e volta entre pixels e fracoes nao altera o campo`() {
        val pixels = GeometriaCampo.paraPixels(campo, 1080f, 1527f)
        val volta = GeometriaCampo.dePixels(pixels, 1080f, 1527f)

        assertEquals(216f, pixels.x, 0.01f)
        assertEquals(763.5f, pixels.y, 0.01f)
        assertEquals(324f, pixels.largura, 0.01f)
        assertEquals(152.7f, pixels.altura, 0.01f)
        assertArea(campo, volta)
    }

    @Test
    fun `mesmo campo ocupa a mesma proporcao em telas de tamanhos diferentes`() {
        val celular = GeometriaCampo.paraPixels(campo, 720f, 1018f)
        val tablet = GeometriaCampo.paraPixels(campo, 1600f, 2263f)

        assertEquals(celular.x / 720f, tablet.x / 1600f, DELTA)
        assertEquals(celular.y / 1018f, tablet.y / 2263f, DELTA)
        assertEquals(celular.largura / 720f, tablet.largura / 1600f, DELTA)
        assertEquals(celular.altura / 1018f, tablet.altura / 2263f, DELTA)
    }

    @Test
    fun `arraste em pixels numa tela equivale ao mesmo deslocamento em fracoes noutra`() {
        val noCelular = GeometriaCampo.mover(campo, 72f / 720f, 0f)
        val noTablet = GeometriaCampo.mover(campo, 160f / 1600f, 0f)

        assertArea(noCelular, noTablet)
    }

    private fun assertArea(esperada: AreaNormalizada, obtida: AreaNormalizada) {
        assertEquals("x", esperada.x, obtida.x, DELTA)
        assertEquals("y", esperada.y, obtida.y, DELTA)
        assertEquals("largura", esperada.largura, obtida.largura, DELTA)
        assertEquals("altura", esperada.altura, obtida.altura, DELTA)
    }

    private companion object {
        const val DELTA = 1e-5f
    }
}
