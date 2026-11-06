package br.com.letssign.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.letssign.ui.theme.Movimento

private val ALTURA_DO_BOTAO = 54.dp
private val ALTURA_DO_BOTAO_DE_TEXTO = 44.dp

/** Ação principal da tela. Com [carregando] mostra o indicador e ignora toques sem mudar de tamanho. */
@Composable
fun BotaoPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    carregando: Boolean = false,
    icone: ImageVector? = null,
) {
    val cores = MaterialTheme.colorScheme
    BotaoBase(
        texto = texto,
        onClick = onClick,
        modifier = modifier,
        habilitado = habilitado,
        carregando = carregando,
        icone = icone,
        fundo = if (habilitado) cores.primary else cores.onSurface.copy(alpha = 0.10f),
        conteudo = if (habilitado) cores.onPrimary else cores.onSurface.copy(alpha = 0.38f),
        altura = ALTURA_DO_BOTAO,
    )
}

/** Ação secundária: mesma altura do botão principal, com fundo suave e sem borda. */
@Composable
fun BotaoSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    carregando: Boolean = false,
    icone: ImageVector? = null,
) {
    val cores = MaterialTheme.colorScheme
    BotaoBase(
        texto = texto,
        onClick = onClick,
        modifier = modifier,
        habilitado = habilitado,
        carregando = carregando,
        icone = icone,
        fundo = if (habilitado) cores.primaryContainer else cores.onSurface.copy(alpha = 0.06f),
        conteudo = if (habilitado) cores.primary else cores.onSurface.copy(alpha = 0.38f),
        altura = ALTURA_DO_BOTAO,
    )
}

@Composable
fun BotaoDeTexto(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    icone: ImageVector? = null,
    cor: Color = MaterialTheme.colorScheme.primary,
) {
    BotaoBase(
        texto = texto,
        onClick = onClick,
        modifier = modifier,
        habilitado = habilitado,
        carregando = false,
        icone = icone,
        fundo = Color.Transparent,
        conteudo = if (habilitado) cor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        altura = ALTURA_DO_BOTAO_DE_TEXTO,
        margemLateral = 12.dp,
    )
}

@Composable
private fun BotaoBase(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier,
    habilitado: Boolean,
    carregando: Boolean,
    icone: ImageVector?,
    fundo: Color,
    conteudo: Color,
    altura: Dp,
    margemLateral: Dp = 20.dp,
) {
    val interacoes = remember { MutableInteractionSource() }
    val escala by escalaAoPressionar(interacoes)
    val corDoFundo by animateColorAsState(fundo, Movimento.rapido(), label = "fundoDoBotao")
    val corDoConteudo by animateColorAsState(conteudo, Movimento.rapido(), label = "conteudoDoBotao")

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .defaultMinSize(minHeight = altura)
            .clip(MaterialTheme.shapes.medium)
            .drawBehind { drawRect(corDoFundo) }
            .clickable(
                interactionSource = interacoes,
                indication = ripple(color = conteudo),
                enabled = habilitado && !carregando,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = margemLateral),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedVisibility(visible = carregando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = corDoConteudo,
                    strokeWidth = 2.dp,
                )
            }
            if (icone != null && !carregando) {
                Icon(icone, contentDescription = null, tint = corDoConteudo, modifier = Modifier.size(20.dp))
            }
            Text(
                text = texto,
                style = MaterialTheme.typography.labelLarge,
                color = corDoConteudo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun BotaoIcone(
    icone: ImageVector,
    descricao: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val interacoes = remember { MutableInteractionSource() }
    val escala by escalaAoPressionar(interacoes, pressionado = 0.9f)
    val tinta by animateColorAsState(
        targetValue = if (habilitado) cor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
        animationSpec = Movimento.rapido(),
        label = "tintaDoIcone",
    )
    Box(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interacoes,
                indication = ripple(bounded = true),
                enabled = habilitado,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icone, contentDescription = descricao, tint = tinta, modifier = Modifier.size(22.dp))
    }
}
