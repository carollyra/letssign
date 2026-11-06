package br.com.letssign.ui.common

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import br.com.letssign.ui.theme.Espacos
import br.com.letssign.ui.theme.TemaLetsSign

// Antes do Android 9 a sombra não aceita cor e sai preta, então fica bem mais baixa.
private val ELEVACAO_DO_CARTAO = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) 12.dp else 1.dp

/** Superfície padrão do app: cantos bem arredondados e sombra quase imperceptível. */
@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    margemInterna: PaddingValues = PaddingValues(Espacos.m),
    content: @Composable ColumnScope.() -> Unit,
) {
    val forma = MaterialTheme.shapes.large
    val cores = TemaLetsSign.cores
    val interacoes = remember { MutableInteractionSource() }
    val escala by escalaAoPressionar(interacoes, pressionado = 0.985f)

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .then(
                if (cores.sombra == Color.Transparent) {
                    Modifier.border(1.dp, cores.bordaDoCartao, forma)
                } else {
                    Modifier.shadow(
                        elevation = ELEVACAO_DO_CARTAO,
                        shape = forma,
                        clip = false,
                        ambientColor = cores.sombra,
                        spotColor = cores.sombra,
                    )
                },
            )
            .clip(forma)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interacoes, indication = ripple(), onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(margemInterna),
        content = content,
    )
}
