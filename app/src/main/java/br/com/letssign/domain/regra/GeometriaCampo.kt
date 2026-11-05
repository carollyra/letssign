package br.com.letssign.domain.regra

/**
 * Área de um campo em frações (0 a 1) da página como ela é exibida, com origem no canto
 * superior esquerdo. Não depende de tela nem de densidade.
 */
data class AreaNormalizada(
    val x: Float,
    val y: Float,
    val largura: Float,
    val altura: Float,
) {
    val direita: Float get() = x + largura
    val base: Float get() = y + altura
}

data class AreaEmPixels(
    val x: Float,
    val y: Float,
    val largura: Float,
    val altura: Float,
)

object GeometriaCampo {

    const val LARGURA_MINIMA = 0.08f
    const val ALTURA_MINIMA = 0.03f
    const val LARGURA_PADRAO = 0.36f
    const val ALTURA_PADRAO = 0.07f

    // Folga para erro de arredondamento em ponto flutuante na soma posição + tamanho.
    private const val TOLERANCIA = 1e-4f

    fun dentroDaPagina(area: AreaNormalizada): Boolean =
        area.largura > 0f && area.altura > 0f &&
            area.x >= -TOLERANCIA && area.y >= -TOLERANCIA &&
            area.direita <= 1f + TOLERANCIA && area.base <= 1f + TOLERANCIA

    /** Ajusta tamanho e posição para a área caber inteira na página. */
    fun limitar(area: AreaNormalizada): AreaNormalizada {
        val largura = area.largura.limpo().coerceIn(LARGURA_MINIMA, 1f)
        val altura = area.altura.limpo().coerceIn(ALTURA_MINIMA, 1f)
        return AreaNormalizada(
            x = area.x.limpo().coerceIn(0f, 1f - largura),
            y = area.y.limpo().coerceIn(0f, 1f - altura),
            largura = largura,
            altura = altura,
        )
    }

    /** Desloca a área mantendo o tamanho; ela para nas bordas da página. */
    fun mover(area: AreaNormalizada, dx: Float, dy: Float): AreaNormalizada =
        limitar(area.copy(x = area.x + dx, y = area.y + dy))

    /** Redimensiona pelo canto inferior direito; o canto superior esquerdo fica fixo. */
    fun redimensionar(area: AreaNormalizada, dLargura: Float, dAltura: Float): AreaNormalizada {
        val base = limitar(area)
        return base.copy(
            largura = (base.largura + dLargura).coerceIn(LARGURA_MINIMA, 1f - base.x),
            altura = (base.altura + dAltura).coerceIn(ALTURA_MINIMA, 1f - base.y),
        )
    }

    fun paraPixels(area: AreaNormalizada, larguraPagina: Float, alturaPagina: Float) = AreaEmPixels(
        x = area.x * larguraPagina,
        y = area.y * alturaPagina,
        largura = area.largura * larguraPagina,
        altura = area.altura * alturaPagina,
    )

    fun dePixels(area: AreaEmPixels, larguraPagina: Float, alturaPagina: Float) = AreaNormalizada(
        x = area.x / larguraPagina,
        y = area.y / alturaPagina,
        largura = area.largura / larguraPagina,
        altura = area.altura / alturaPagina,
    )

    private fun Float.limpo(): Float = if (isNaN()) 0f else this
}
