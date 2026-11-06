package br.com.letssign.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Escuras o bastante para o traço e o nome serem lidos sobre a folha branca do PDF.
private val CORES_SIGNATARIO = listOf(
    Color(0xFF2F6FDE),
    Color(0xFFD9602A),
    Color(0xFF1F9367),
    Color(0xFF8A4FD8),
    Color(0xFFD23C78),
    Color(0xFF0E8C9E),
    Color(0xFFA9700F),
    Color(0xFF5B6475),
)

/** Cor de um signatário pela posição dele na lista do documento. */
fun corDoSignatario(indice: Int): Color = CORES_SIGNATARIO[indice.mod(CORES_SIGNATARIO.size)]

/** Iniciais do signatário sobre a cor que identifica os campos dele. */
@Composable
fun AvatarDoSignatario(nome: String, cor: Color, modifier: Modifier = Modifier, tamanho: Dp = 38.dp) {
    Box(
        modifier = modifier
            .size(tamanho)
            .background(cor, CircleShape)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = iniciaisDe(nome),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}
