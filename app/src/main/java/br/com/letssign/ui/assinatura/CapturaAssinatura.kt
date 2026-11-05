package br.com.letssign.ui.assinatura

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path as PathAndroid
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.letssign.R
import java.io.ByteArrayOutputStream
import kotlin.math.ceil
import kotlin.math.max

private val COR_DA_TINTA = Color(0xFF0B1F3A)
private val ESPESSURA_DO_TRACO = 3.dp
private val COMPRIMENTO_MINIMO = 32.dp
private const val LARGURA_MAXIMA_PNG = 1000f

/** Diálogo com a área onde o signatário desenha. Devolve a assinatura como PNG de fundo transparente. */
@Composable
fun CapturaAssinaturaDialog(
    onCancelar: () -> Unit,
    onConfirmar: (ByteArray) -> Unit,
) {
    val tracos = remember { mutableStateListOf<List<Offset>>() }
    var tracoAtual by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val densidade = LocalDensity.current
    val espessuraPx = with(densidade) { ESPESSURA_DO_TRACO.toPx() }
    val comprimentoMinimoPx = with(densidade) { COMPRIMENTO_MINIMO.toPx() }
    val descricao = stringResource(R.string.assinatura_area_descricao)

    // Um toque acidental não pode valer como assinatura.
    val desenhoSuficiente = tracos.sumOf { comprimento(it).toDouble() } >= comprimentoMinimoPx

    Dialog(
        onDismissRequest = onCancelar,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.assinatura_dialogo_titulo),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(R.string.assinatura_dialogo_instrucao),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clipToBounds()
                        .semantics { contentDescription = descricao }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val toque = awaitFirstDown()
                                toque.consume()
                                tracoAtual = listOf(toque.position)
                                drag(toque.id) { mudanca ->
                                    mudanca.consume()
                                    tracoAtual = tracoAtual + mudanca.position
                                }
                                tracos += tracoAtual
                                tracoAtual = emptyList()
                            }
                        },
                ) {
                    drawLine(
                        color = Color(0xFFB0B8C4),
                        start = Offset(size.width * 0.08f, size.height * 0.78f),
                        end = Offset(size.width * 0.92f, size.height * 0.78f),
                        strokeWidth = 1.dp.toPx(),
                    )
                    tracos.forEach { desenharTraco(it, espessuraPx) }
                    desenharTraco(tracoAtual, espessuraPx)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            tracos.clear()
                            tracoAtual = emptyList()
                        },
                        enabled = tracos.isNotEmpty(),
                    ) {
                        Text(stringResource(R.string.assinatura_limpar))
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        TextButton(onClick = onCancelar) { Text(stringResource(R.string.acao_cancelar)) }
                        Button(
                            onClick = { onConfirmar(gerarPng(tracos.toList(), espessuraPx)) },
                            enabled = desenhoSuficiente,
                        ) {
                            Text(stringResource(R.string.assinatura_confirmar))
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.desenharTraco(pontos: List<Offset>, espessuraPx: Float) {
    when (pontos.size) {
        0 -> Unit
        1 -> drawCircle(COR_DA_TINTA, radius = espessuraPx / 2f, center = pontos.first())
        else -> {
            val caminho = Path().apply {
                moveTo(pontos.first().x, pontos.first().y)
                pontos.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = caminho,
                color = COR_DA_TINTA,
                style = Stroke(width = espessuraPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

private fun comprimento(pontos: List<Offset>): Float =
    pontos.zipWithNext { a, b -> (b - a).getDistance() }.sum()

/**
 * Recorta o desenho ao redor dos traços, para a assinatura ocupar o campo inteiro em vez de
 * levar junto o espaço vazio da área de captura.
 */
private fun gerarPng(tracos: List<List<Offset>>, espessuraPx: Float): ByteArray {
    val pontos = tracos.flatten()
    if (pontos.isEmpty()) return ByteArray(0)

    val margem = espessuraPx
    val esquerda = pontos.minOf { it.x } - margem
    val topo = pontos.minOf { it.y } - margem
    val largura = pontos.maxOf { it.x } + margem - esquerda
    val altura = pontos.maxOf { it.y } + margem - topo
    val escala = (LARGURA_MAXIMA_PNG / max(largura, altura)).coerceAtMost(2f)

    val bitmap = Bitmap.createBitmap(
        ceil(largura * escala).toInt().coerceAtLeast(1),
        ceil(altura * escala).toInt().coerceAtLeast(1),
        Bitmap.Config.ARGB_8888,
    )
    val tela = android.graphics.Canvas(bitmap)
    tela.scale(escala, escala)
    tela.translate(-esquerda, -topo)
    val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COR_DA_TINTA.toArgb()
        strokeWidth = espessuraPx
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    tracos.forEach { traco ->
        if (traco.size == 1) {
            pincel.style = Paint.Style.FILL
            tela.drawCircle(traco.first().x, traco.first().y, espessuraPx / 2f, pincel)
        } else if (traco.size > 1) {
            pincel.style = Paint.Style.STROKE
            val caminho = PathAndroid().apply {
                moveTo(traco.first().x, traco.first().y)
                traco.drop(1).forEach { lineTo(it.x, it.y) }
            }
            tela.drawPath(caminho, pincel)
        }
    }
    return ByteArrayOutputStream().use { saida ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, saida)
        bitmap.recycle()
        saida.toByteArray()
    }
}
