package br.com.letssign.ui.campos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.regra.AreaNormalizada
import br.com.letssign.domain.regra.GeometriaCampo
import br.com.letssign.ui.common.BotaoIcone
import br.com.letssign.ui.common.BotaoSecundario
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.ChipDeFiltro
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.FileiraDeChips
import br.com.letssign.ui.common.Icones
import br.com.letssign.ui.common.corDoSignatario
import br.com.letssign.ui.common.rotulo
import br.com.letssign.ui.common.sombraExterna
import br.com.letssign.ui.documento.AcoesDaEtapa
import br.com.letssign.ui.pdf.EstadoPdf
import br.com.letssign.ui.pdf.FORMA_DO_CAMPO
import br.com.letssign.ui.pdf.PaginaPdf
import br.com.letssign.ui.pdf.RotuloDoCampo
import br.com.letssign.ui.pdf.rememberDocumentoPdf
import br.com.letssign.ui.theme.Espacos
import br.com.letssign.ui.theme.Movimento
import kotlin.math.roundToInt

private val TAMANHO_ALCA = 32.dp
private val RAIO_DO_CAMPO = 6.dp

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

    when {
        estado.carregando -> Carregando()
        estado.erroCarregar -> EstadoErro(
            mensagem = stringResource(R.string.campos_erro_carregar),
            onTentarNovamente = viewModel::carregar,
        )
        else -> Column(modifier = Modifier.fillMaxSize()) {
            SeletorSignatario(estado = estado, onSelecionar = viewModel::selecionarSignatario)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Espacos.tela, vertical = Espacos.s)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
                    .padding(Espacos.s),
                contentAlignment = Alignment.Center,
            ) {
                when (val pdf = rememberDocumentoPdf(estado.caminhoPdf)) {
                    EstadoPdf.Carregando -> Carregando()
                    EstadoPdf.Erro -> EstadoErro(mensagem = stringResource(R.string.pdf_erro_abrir))
                    is EstadoPdf.Pronto -> AnimatedContent(
                        targetState = estado.paginaAtual,
                        transitionSpec = {
                            val sentido = if (targetState > initialState) 1 else -1
                            (slideInHorizontally(Movimento.medio()) { sentido * it / 5 } + fadeIn(Movimento.medio()))
                                .togetherWith(
                                    slideOutHorizontally(Movimento.medio()) { -sentido * it / 5 } +
                                        fadeOut(Movimento.rapido()),
                                )
                        },
                        contentAlignment = Alignment.Center,
                        label = "pagina",
                    ) { pagina ->
                        PaginaPdf(documento = pdf.documento, indice = pagina) { tamanho ->
                            CamposDaPagina(
                                estado = estado,
                                pagina = pagina,
                                tamanhoPagina = tamanho,
                                viewModel = viewModel,
                            )
                        }
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
                modifier = Modifier.padding(horizontal = Espacos.tela, vertical = Espacos.xxs),
            )
            AcoesDaEtapa(
                textoPrincipal = stringResource(
                    if (estado.salvando) R.string.acao_salvando else R.string.acao_continuar,
                ),
                onPrincipal = viewModel::continuar,
                carregando = estado.salvando,
                onVoltar = onVoltar,
            )
        }
    }
}

@Composable
private fun SeletorSignatario(estado: PosicionamentoUiState, onSelecionar: (Long) -> Unit) {
    FileiraDeChips(
        quantidade = estado.signatarios.size,
        indiceSelecionado = estado.signatarios.indexOfFirst { it.id == estado.signatarioAtivoId },
    ) { indice ->
        val signatario = estado.signatarios[indice]
        val total = estado.campos.count { it.signatarioId == signatario.id }
        ChipDeFiltro(
            texto = signatario.nome,
            legenda = stringResource(signatario.papel.rotulo) + " · " +
                pluralStringResource(R.plurals.campos_total, total, total),
            marcador = corDoSignatario(signatario.indiceCor),
            selecionado = signatario.id == estado.signatarioAtivoId,
            onClick = { onSelecionar(signatario.id) },
        )
    }
}

@Composable
private fun ControlesDaPagina(estado: PosicionamentoUiState, viewModel: PosicionamentoCamposViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Espacos.s, end = Espacos.tela),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotaoIcone(
            icone = Icones.Voltar,
            descricao = stringResource(R.string.campos_pagina_anterior),
            onClick = { viewModel.irParaPagina(estado.paginaAtual - 1) },
            habilitado = estado.paginaAtual > 0,
        )
        Text(
            text = stringResource(R.string.campos_pagina_curta, estado.paginaAtual + 1, estado.totalPaginas),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            softWrap = false,
        )
        BotaoIcone(
            icone = Icones.Avancar,
            descricao = stringResource(R.string.campos_proxima_pagina),
            onClick = { viewModel.irParaPagina(estado.paginaAtual + 1) },
            habilitado = estado.paginaAtual < estado.totalPaginas - 1,
        )
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(
            visible = estado.campoSelecionado != null,
            enter = scaleIn(Movimento.rapido()) + fadeIn(Movimento.rapido()),
            exit = scaleOut(Movimento.rapido()) + fadeOut(Movimento.rapido()),
        ) {
            BotaoIcone(
                icone = Icones.Lixeira,
                descricao = stringResource(R.string.campos_excluir),
                onClick = viewModel::excluirCampoSelecionado,
                cor = MaterialTheme.colorScheme.error,
            )
        }
        BotaoSecundario(
            texto = stringResource(R.string.campos_adicionar),
            onClick = viewModel::adicionarCampo,
            icone = Icones.Mais,
            compacto = true,
            modifier = Modifier.padding(start = Espacos.xxs),
        )
    }
}

@Composable
private fun BoxScope.CamposDaPagina(
    estado: PosicionamentoUiState,
    pagina: Int,
    tamanhoPagina: IntSize,
    viewModel: PosicionamentoCamposViewModel,
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .pointerInput(Unit) { detectTapGestures { viewModel.selecionarCampo(null) } },
    )

    val signatarios = estado.signatarios.associateBy { it.id }
    val campos = estado.campos.filter { it.pagina == pagina }
    campos.forEach { campo ->
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
    campos.firstOrNull { it.chave == estado.campoSelecionado }?.let { campo ->
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
    val haptico = LocalHapticFeedback.current

    // O arraste parte da área do início do gesto mais o deslocamento acumulado, para o campo
    // continuar colado ao dedo depois de bater na borda e voltar.
    var origem by remember { mutableStateOf(campo.area) }
    var acumulado by remember { mutableStateOf(Offset.Zero) }

    var arrastando by remember { mutableStateOf(false) }
    val elevacao by animateDpAsState(
        targetValue = if (arrastando) 10.dp else 0.dp,
        animationSpec = Movimento.rapido(),
        label = "elevacaoDoCampo",
    )
    val escala by animateFloatAsState(
        targetValue = if (arrastando) 1.04f else 1f,
        animationSpec = Movimento.rapido(),
        label = "escalaDoCampo",
    )
    val preenchimento by animateFloatAsState(
        targetValue = when {
            arrastando -> 0.34f
            selecionado -> 0.26f
            else -> 0.14f
        },
        animationSpec = Movimento.rapido(),
        label = "preenchimentoDoCampo",
    )

    Box(
        modifier = Modifier
            .zIndex(if (arrastando) 1f else 0f)
            .offset { IntOffset(pixels.x.roundToInt(), pixels.y.roundToInt()) }
            .size(
                width = with(LocalDensity.current) { pixels.largura.toDp() },
                height = with(LocalDensity.current) { pixels.altura.toDp() },
            )
            .semantics { contentDescription = descricao }
            .pointerInput(Unit) { detectTapGestures { onSelecionar() } }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        origem = areaAtual
                        acumulado = Offset.Zero
                        arrastando = true
                        onSelecionar()
                    },
                    onDragEnd = {
                        arrastando = false
                        haptico.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        onFimDoGesto()
                    },
                    onDragCancel = {
                        arrastando = false
                        onFimDoGesto()
                    },
                ) { mudanca, arraste ->
                    mudanca.consume()
                    acumulado += arraste
                    onMudarArea(
                        GeometriaCampo.mover(origem, acumulado.x / pagina.width, acumulado.y / pagina.height),
                    )
                }
            },
    ) {
        // A escala fica numa camada interna: no próprio nó que recebe o toque ela mudaria as
        // coordenadas do gesto no meio do arraste e o campo deixaria de acompanhar o dedo.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = escala
                    scaleY = escala
                }
                .sombraExterna(elevacao = { elevacao }, raioDoCanto = RAIO_DO_CAMPO)
                .background(cor.copy(alpha = preenchimento), FORMA_DO_CAMPO)
                .border(if (selecionado) 2.dp else 1.5.dp, cor, FORMA_DO_CAMPO),
            contentAlignment = Alignment.Center,
        ) {
            RotuloDoCampo(texto = nome, cor = cor)
        }
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
    val haptico = LocalHapticFeedback.current

    var origem by remember { mutableStateOf(campo.area) }
    var acumulado by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .zIndex(2f)
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
                    onDragEnd = {
                        haptico.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        onFimDoGesto()
                    },
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
                .size(16.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(3.dp, cor, CircleShape),
        )
    }
}
