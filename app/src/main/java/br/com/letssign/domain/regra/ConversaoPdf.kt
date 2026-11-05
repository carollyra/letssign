package br.com.letssign.domain.regra

/**
 * Parte visível da página no espaço de coordenadas do PDF: origem no canto inferior esquerdo,
 * medidas em pontos e ainda sem a rotação aplicada. [rotacao] é o ângulo, em graus no sentido
 * horário, com que o leitor exibe a página.
 */
data class CaixaPagina(
    val x: Float,
    val y: Float,
    val largura: Float,
    val altura: Float,
    val rotacao: Int,
)

data class PontoPdf(val x: Float, val y: Float)

/**
 * Transformação afim `[a b c d e f]` que leva o quadrado unitário (onde o PDF desenha uma
 * imagem) até a área do campo, já girada para a imagem aparecer de pé na página exibida.
 */
data class MatrizPdf(
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float,
    val e: Float,
    val f: Float,
) {
    fun aplicar(x: Float, y: Float) = PontoPdf(a * x + c * y + e, b * x + d * y + f)
}

object ConversaoPdf {

    /** Reduz qualquer ângulo a 0, 90, 180 ou 270; o PDF só admite múltiplos de 90. */
    fun rotacaoNormalizada(rotacao: Int): Int {
        val positiva = ((rotacao % 360) + 360) % 360
        return (positiva + 45) / 90 % 4 * 90
    }

    /** Largura e altura da página como aparece na tela, depois da rotação. */
    fun tamanhoExibido(caixa: CaixaPagina): Pair<Float, Float> =
        when (rotacaoNormalizada(caixa.rotacao)) {
            90, 270 -> caixa.altura to caixa.largura
            else -> caixa.largura to caixa.altura
        }

    /**
     * Converte um ponto da página exibida ([u], [v] em frações, origem no canto superior
     * esquerdo) para o espaço do PDF.
     */
    fun exibidoParaPdf(u: Float, v: Float, caixa: CaixaPagina): PontoPdf {
        val (dx, dy) = when (rotacaoNormalizada(caixa.rotacao)) {
            90 -> v * caixa.largura to u * caixa.altura
            180 -> (1f - u) * caixa.largura to v * caixa.altura
            270 -> (1f - v) * caixa.largura to (1f - u) * caixa.altura
            else -> u * caixa.largura to (1f - v) * caixa.altura
        }
        return PontoPdf(caixa.x + dx, caixa.y + dy)
    }

    /** Inversa de [exibidoParaPdf]: devolve as frações (u, v) na página exibida. */
    fun pdfParaExibido(ponto: PontoPdf, caixa: CaixaPagina): Pair<Float, Float> {
        val px = (ponto.x - caixa.x) / caixa.largura
        val py = (ponto.y - caixa.y) / caixa.altura
        return when (rotacaoNormalizada(caixa.rotacao)) {
            90 -> py to px
            180 -> 1f - px to py
            270 -> 1f - py to 1f - px
            else -> px to 1f - py
        }
    }

    fun matrizDoCampo(area: AreaNormalizada, caixa: CaixaPagina): MatrizPdf {
        val origem = exibidoParaPdf(area.x, area.base, caixa)
        val fimDaBase = exibidoParaPdf(area.direita, area.base, caixa)
        val topo = exibidoParaPdf(area.x, area.y, caixa)
        return MatrizPdf(
            a = fimDaBase.x - origem.x,
            b = fimDaBase.y - origem.y,
            c = topo.x - origem.x,
            d = topo.y - origem.y,
            e = origem.x,
            f = origem.y,
        )
    }

    /**
     * Maior área com a proporção [proporcaoImagem] (largura / altura) que cabe em [area],
     * centralizada, para a assinatura não ser esticada.
     */
    fun encaixar(area: AreaNormalizada, proporcaoImagem: Float, caixa: CaixaPagina): AreaNormalizada {
        if (proporcaoImagem <= 0f || proporcaoImagem.isNaN()) return area
        val (larguraPagina, alturaPagina) = tamanhoExibido(caixa)
        val larguraCampo = area.largura * larguraPagina
        val alturaCampo = area.altura * alturaPagina
        if (larguraCampo <= 0f || alturaCampo <= 0f) return area

        val largura: Float
        val altura: Float
        if (larguraCampo / alturaCampo > proporcaoImagem) {
            altura = alturaCampo
            largura = alturaCampo * proporcaoImagem
        } else {
            largura = larguraCampo
            altura = larguraCampo / proporcaoImagem
        }
        return AreaNormalizada(
            x = area.x + (larguraCampo - largura) / 2f / larguraPagina,
            y = area.y + (alturaCampo - altura) / 2f / alturaPagina,
            largura = largura / larguraPagina,
            altura = altura / alturaPagina,
        )
    }
}
