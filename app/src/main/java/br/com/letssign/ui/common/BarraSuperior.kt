package br.com.letssign.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.letssign.R
import br.com.letssign.domain.model.Usuario
import br.com.letssign.ui.theme.Movimento

/**
 * Barra superior com título centralizado. Em telas com [TituloGrande], [tituloVisivel] deixa o
 * título pequeno aparecer só depois que o grande saiu da tela.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(
    titulo: String,
    modifier: Modifier = Modifier,
    onVoltar: (() -> Unit)? = null,
    tituloVisivel: Boolean = true,
    acoes: @Composable RowScope.() -> Unit = {},
) {
    val opacidade by animateFloatAsState(
        targetValue = if (tituloVisivel) 1f else 0f,
        animationSpec = Movimento.rapido(),
        label = "tituloDaBarra",
    )
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { alpha = opacidade },
            )
        },
        navigationIcon = {
            if (onVoltar != null) {
                BotaoIcone(
                    icone = Icones.Voltar,
                    descricao = stringResource(R.string.acao_voltar),
                    onClick = onVoltar,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        },
        actions = {
            acoes()
            Spacer(Modifier.width(10.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
        modifier = modifier,
    )
}

/** Título de abertura das telas iniciais, com uma linha discreta acima dele. */
@Composable
fun TituloGrande(titulo: String, modifier: Modifier = Modifier, sobretitulo: String? = null) {
    Column(modifier = modifier) {
        if (sobretitulo != null) {
            Text(
                text = sobretitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** Círculo com as iniciais do usuário que abre o menu da conta, de onde se sai do app. */
@Composable
fun MenuDaConta(usuario: Usuario, onSair: () -> Unit, modifier: Modifier = Modifier) {
    var aberto by remember { mutableStateOf(false) }
    val descricao = stringResource(R.string.conta_menu)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClickLabel = descricao, role = Role.Button) { aberto = true },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = iniciaisDe(usuario.nome),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.semantics { contentDescription = descricao },
                )
            }
        }
        DropdownMenu(
            expanded = aberto,
            onDismissRequest = { aberto = false },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 220.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(usuario.nome, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = usuario.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DropdownMenuItem(
                text = { Text(stringResource(R.string.acao_sair), style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icones.Sair, contentDescription = null, modifier = Modifier.size(20.dp)) },
                onClick = {
                    aberto = false
                    onSair()
                },
            )
        }
    }
}
