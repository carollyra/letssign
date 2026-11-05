package br.com.letssign.domain.model

import br.com.letssign.domain.regra.AreaNormalizada

/**
 * Posição e tamanho são frações (0 a 1) da página como ela é exibida, com origem no
 * canto superior esquerdo. A página é indexada a partir de zero.
 */
data class CampoAssinatura(
    val id: Long,
    val documentoSignatarioId: Long,
    val pagina: Int,
    val x: Float,
    val y: Float,
    val largura: Float,
    val altura: Float,
) {
    val area: AreaNormalizada get() = AreaNormalizada(x, y, largura, altura)
}
