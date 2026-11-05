package br.com.letssign.ui.common

import androidx.compose.ui.graphics.Color

private val CORES_SIGNATARIO = listOf(
    Color(0xFF1565C0),
    Color(0xFFC62828),
    Color(0xFF2E7D32),
    Color(0xFF6A1B9A),
    Color(0xFFEF6C00),
    Color(0xFF00838F),
    Color(0xFFAD1457),
    Color(0xFF4E342E),
)

/** Cor de um signatário pela posição dele na lista do documento. */
fun corDoSignatario(indice: Int): Color = CORES_SIGNATARIO[indice.mod(CORES_SIGNATARIO.size)]
