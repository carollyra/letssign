package br.com.letssign.domain.model

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
)
