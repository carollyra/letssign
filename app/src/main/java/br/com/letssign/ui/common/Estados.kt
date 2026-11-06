package br.com.letssign.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.letssign.R
import br.com.letssign.ui.theme.Espacos

@Composable
fun Carregando(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
            strokeCap = StrokeCap.Round,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
fun EstadoVazio(
    titulo: String,
    modifier: Modifier = Modifier,
    descricao: String? = null,
    icone: ImageVector = Icones.Bandeja,
    acao: (@Composable () -> Unit)? = null,
) {
    Mensagem(
        icone = icone,
        fundoDoIcone = MaterialTheme.colorScheme.surfaceVariant,
        corDoIcone = MaterialTheme.colorScheme.onSurfaceVariant,
        titulo = titulo,
        descricao = descricao,
        acao = acao,
        modifier = modifier,
    )
}

@Composable
fun EstadoErro(
    mensagem: String,
    modifier: Modifier = Modifier,
    onTentarNovamente: (() -> Unit)? = null,
) {
    Mensagem(
        icone = Icones.Alerta,
        fundoDoIcone = MaterialTheme.colorScheme.errorContainer,
        corDoIcone = MaterialTheme.colorScheme.error,
        titulo = mensagem,
        descricao = null,
        acao = onTentarNovamente?.let {
            { BotaoSecundario(texto = stringResource(R.string.acao_tentar_novamente), onClick = it) }
        },
        modifier = modifier,
    )
}

@Composable
private fun Mensagem(
    icone: ImageVector,
    fundoDoIcone: Color,
    corDoIcone: Color,
    titulo: String,
    descricao: String?,
    acao: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = Espacos.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(fundoDoIcone, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icone, contentDescription = null, tint = corDoIcone, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(Espacos.l))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (descricao != null) {
            Spacer(Modifier.height(Espacos.xs))
            Text(
                text = descricao,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (acao != null) {
            Spacer(Modifier.height(Espacos.xl))
            acao()
        }
    }
}

/** Faixa de erro dentro do conteúdo, para falhas que não ocupam a tela inteira. */
@Composable
fun AvisoDeErro(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
            .padding(horizontal = Espacos.m, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(Espacos.s),
    ) {
        Icon(
            imageVector = Icones.Alerta,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

@Composable
fun AvisoDeErro(texto: String, modifier: Modifier = Modifier) {
    AvisoDeErro(modifier = modifier) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
