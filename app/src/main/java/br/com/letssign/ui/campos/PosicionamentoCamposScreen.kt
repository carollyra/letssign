package br.com.letssign.ui.campos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.regra.AreaNormalizada
import br.com.letssign.domain.regra.GeometriaCampo
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.corDoSignatario
import br.com.letssign.ui.common.rotulo
import br.com.letssign.ui.documento.EtapaDocumento
import br.com.letssign.ui.documento.IndicadorEtapa
import br.com.letssign.ui.pdf.EstadoPdf
import br.com.letssign.ui.pdf.PaginaPdf
import br.com.letssign.ui.pdf.rememberDocumentoPdf
import kotlin.math.roundToInt

private val TAMANHO_ALCA = 28.dp

@Composable
fun PosicionamentoCamposScreen(
    onVoltar: () -> Unit,
    onProximaEtapa: (Long) -> Unit,
    viewModel: PosicionamentoCamposViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.proximaEtapa.collect(onProximaEtapa)
    }

    Scaffold(
        topBar = {
            BarraSuperior(titulo = stringResource(R.string.campos_titulo), onVoltar = onVoltar)
        },
    ) { margens ->
        when {
            estado.carregando -> Carregando(Modifier.padding(margens))
            estado.erroCarregar -> EstadoErro(
                mensagem = stringResource(R.string.campos_erro_carregar),
                modifier = Modifier.padding(margens),
                onTentarNovamente = viewModel::carregar,
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(margens),
            ) {
                IndicadorEtapa(EtapaDocumento.CAMPOS)
                SeletorSignatario(estado = estado, onSelecionar = viewModel::selecionarSignatario)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when (val pdf = rememberDocumentoPdf(estado.caminhoPdf)) {
                        EstadoPdf.Carregando -> Carregando()
                        EstadoPdf.Erro -> EstadoErro(mensagem = stringResource(R.string.pdf_erro_abrir))
                        is EstadoPdf.Pronto -> PaginaPdf(
                            documento = pdf.documento,
                            indice = estado.paginaAtual,
                        ) { tamanho ->
                            CamposDaPagina(estado = estado, tamanhoPagina = tamanho, viewModel = viewModel)
                        }
                    }
                }

                ControlesDaPagina(estado = estado, viewModel = viewModel)

                val erro = when {
                    estado.semCampo.isNotEmpty() ->
                        stringResource(R.string.campos_erro_sem_campo, estado.semCampo.joinToString(", "))
                    estado.erroGeral != null -> stringResource(estado.erroGeral!!)
                    else -> null
                }
                Text(
                    text = erro ?: stringResource(R.string.campos_dica),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (erro != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    OutlinedButton(onClick = onVoltar) { Text(stringResource(R.string.acao_voltar)) }
                    Button(onClick = viewModel::continuar, enabled = !estado.salvando) {
                        Text(
                            stringResource(
                                if (estado.salvando) R.string.acao_salvando else R.string.acao_continuar,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeletorSignatario(estado: PosicionamentoUiState, onSelecionar: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        estado.signatarios.forEach { signatario ->
            val total = estado.campos.count { it.signatarioId == signatario.id }
            FilterChip(
                selected = signatario.id == estado.signatarioAtivoId,
                onClick = { onSelecionar(signatario.id) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(corDoSignatario(signatario.indiceCor), CircleShape),
                    )
                },
                label = {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(signatario.nome, maxLines = 1)
                        Text(
                            text = stringResource(signatario.papel.rotulo) + " · " +
                                pluralStringResource(R.plurals.campos_total, total, total),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun ControlesDaPagina(estado: PosicionamentoUiState, viewModel: PosicionamentoCamposViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { viewModel.irParaPagina(estado.paginaAtual - 1) },
            enabled = estado.paginaAtual > 0,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.campos_pagina_anterior),
            )
        }
        Text(
            text = stringResource(R.string.campos_pagina, estado.paginaAtual + 1, estado.totalPaginas),
            style = MaterialTheme.typography.bodyMedium,
        )
        IconButton(
            onClick = { viewModel.irParaPagina(estado.paginaAtual + 1) },
            enabled = estado.paginaAtual < estado.totalPaginas - 1,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.campos_proxima_pagina),
            )
        }
        Box(Modifier.weight(1f))
        if (estado.campoSelecionado != null) {
            IconButton(onClick = viewModel::excluirCampoSelecionado) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.campos_excluir),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        FilledTonalButton(onClick = viewModel::adicionarCampo) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.campos_adicionar),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun BoxScope.CamposDaPagina(
    estado: PosicionamentoUiState,
    tamanhoPagina: IntSize,
    viewModel: PosicionamentoCamposViewModel,
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .pointerInput(Unit) { detectTapGestures { viewModel.selecionarCampo(null) } },
    )

    val signatarios = estado.signatarios.associateBy { it.id }
    estado.camposDaPagina.forEach { campo ->
        val signatario = signatarios[campo.signatarioId] ?: return@forEach
        key(campo.chave) {
            CampoArrastavel(
                campo = campo,
                nome = signatario.nome,
                cor = corDoSignatario(signatario.indiceCor),
                selecionado = campo.chave == estado.campoSelecionado,
                tamanhoPagina = tamanhoPagina,
                onSelecionar = { viewModel.selecionarCampo(campo.chave) },
                onMudarArea = { viewModel.atualizarArea(campo.chave, it) },
                onFimDoGesto = viewModel::aoTerminarGesto,
            )
        }
    }

    // A alça é irmã do campo, e não filha, para continuar tocável mesmo com o campo no tamanho mínimo.
    estado.camposDaPagina.firstOrNull { it.chave == estado.campoSelecionado }?.let { campo ->
        val signatario = signatarios[campo.signatarioId] ?: return@let
        key(campo.chave) {
            AlcaDeRedimensionar(
                campo = campo,
                cor = corDoSignatario(signatario.indiceCor),
                tamanhoPagina = tamanhoPagina,
                onMudarArea = { viewModel.atualizarArea(campo.chave, it) },
                onFimDoGesto = viewModel::aoTerminarGesto,
            )
        }
    }
}

@Composable
private fun CampoArrastavel(
    campo: CampoEditavel,
    nome: String,
    cor: Color,
    selecionado: Boolean,
    tamanhoPagina: IntSize,
    onSelecionar: () -> Unit,
    onMudarArea: (AreaNormalizada) -> Unit,
    onFimDoGesto: () -> Unit,
) {
    val pixels = GeometriaCampo.paraPixels(campo.area, tamanhoPagina.width.toFloat(), tamanhoPagina.height.toFloat())
    val areaAtual by rememberUpdatedState(campo.area)
    val pagina by rememberUpdatedState(tamanhoPagina)
    val descricao = stringResource(R.string.campos_descricao_campo, nome)

    // O arraste parte da área do início do gesto mais o deslocamento acumulado, para o campo
    // continuar colado ao dedo depois de bater na borda e voltar.
    var origem by remember { mutableStateOf(campo.area) }
    var acumulado by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .offset { IntOffset(pixels.x.roundToInt(), pixels.y.roundToInt()) }
            .size(
                width = with(LocalDensity.current) { pixels.largura.toDp() },
                height = with(LocalDensity.current) { pixels.altura.toDp() },
            )
            .background(cor.copy(alpha = if (selecionado) 0.30f else 0.18f), RoundedCornerShape(4.dp))
            .border(if (selecionado) 2.dp else 1.dp, cor, RoundedCornerShape(4.dp))
            .semantics { contentDescription = descricao }
            .pointerInput(Unit) { detectTapGestures { onSelecionar() } }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        origem = areaAtual
                        acumulado = Offset.Zero
                        onSelecionar()
                    },
                    onDragEnd = onFimDoGesto,
                    onDragCancel = onFimDoGesto,
                ) { mudanca, arraste ->
                    mudanca.consume()
                    acumulado += arraste
                    onMudarArea(
                        GeometriaCampo.mover(origem, acumulado.x / pagina.width, acumulado.y / pagina.height),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = nome,
            style = MaterialTheme.typography.labelSmall,
            color = cor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun AlcaDeRedimensionar(
    campo: CampoEditavel,
    cor: Color,
    tamanhoPagina: IntSize,
    onMudarArea: (AreaNormalizada) -> Unit,
    onFimDoGesto: () -> Unit,
) {
    val pixels = GeometriaCampo.paraPixels(campo.area, tamanhoPagina.width.toFloat(), tamanhoPagina.height.toFloat())
    val areaAtual by rememberUpdatedState(campo.area)
    val pagina by rememberUpdatedState(tamanhoPagina)
    val metade = with(LocalDensity.current) { TAMANHO_ALCA.toPx() / 2f }
    val descricao = stringResource(R.string.campos_redimensionar)

    var origem by remember { mutableStateOf(campo.area) }
    var acumulado by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (pixels.x + pixels.largura - metade).roundToInt(),
                    (pixels.y + pixels.altura - metade).roundToInt(),
                )
            }
            .size(TAMANHO_ALCA)
            .semantics { contentDescription = descricao }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        origem = areaAtual
                        acumulado = Offset.Zero
                    },
                    onDragEnd = onFimDoGesto,
                    onDragCancel = onFimDoGesto,
                ) { mudanca, arraste ->
                    mudanca.consume()
                    acumulado += arraste
                    onMudarArea(
                        GeometriaCampo.redimensionar(
                            origem,
                            acumulado.x / pagina.width,
                            acumulado.y / pagina.height,
                        ),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(14.dp)
                .background(Color.White, CircleShape)
                .border(3.dp, cor, CircleShape),
        )
    }
}
