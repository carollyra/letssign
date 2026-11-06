package br.com.letssign.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.model.Documento
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.domain.model.Usuario
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.BotaoDeTexto
import br.com.letssign.ui.common.BotaoFlutuante
import br.com.letssign.ui.common.BotaoIcone
import br.com.letssign.ui.common.BotaoPrincipal
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.Cartao
import br.com.letssign.ui.common.ChipDeFiltro
import br.com.letssign.ui.common.ChipStatus
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.EstadoLista
import br.com.letssign.ui.common.EstadoVazio
import br.com.letssign.ui.common.FileiraDeChips
import br.com.letssign.ui.common.Icones
import br.com.letssign.ui.common.MenuDaConta
import br.com.letssign.ui.common.TituloGrande
import br.com.letssign.ui.common.entradaSuave
import br.com.letssign.ui.common.formatarDataHora
import br.com.letssign.ui.common.progressoDoItem
import br.com.letssign.ui.common.rememberEntradaDeLista
import br.com.letssign.ui.common.rotulo
import br.com.letssign.ui.theme.Espacos
import br.com.letssign.ui.theme.Movimento

@Composable
fun HomeAdminScreen(
    usuario: Usuario,
    onSair: () -> Unit,
    onSignatarios: () -> Unit,
    onNovoDocumento: () -> Unit,
    onAbrirDocumento: (Documento) -> Unit,
    viewModel: HomeAdminViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val filtro by viewModel.filtro.collectAsStateWithLifecycle()
    val lista = rememberLazyListState()
    val tituloSaiuDaTela by remember { derivedStateOf { lista.firstVisibleItemIndex > 0 } }
    val documentos = (estado as? EstadoLista.Dados)?.itens

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = stringResource(R.string.home_admin_titulo),
                tituloVisivel = documentos == null || tituloSaiuDaTela,
                acoes = {
                    BotaoIcone(
                        icone = Icones.Pessoas,
                        descricao = stringResource(R.string.signatarios_titulo),
                        onClick = onSignatarios,
                    )
                    MenuDaConta(usuario = usuario, onSair = onSair)
                },
            )
        },
        floatingActionButton = {
            // Sem documentos, a ação fica no próprio estado vazio.
            AnimatedVisibility(
                visible = !documentos.isNullOrEmpty(),
                enter = scaleIn(Movimento.medio()) + fadeIn(Movimento.medio()),
                exit = scaleOut(Movimento.rapido()) + fadeOut(Movimento.rapido()),
            ) {
                BotaoFlutuante(
                    texto = stringResource(R.string.home_admin_novo_documento),
                    icone = Icones.Mais,
                    onClick = onNovoDocumento,
                )
            }
        },
    ) { margens ->
        when (val atual = estado) {
            EstadoLista.Carregando -> Carregando(Modifier.padding(margens))
            EstadoLista.Erro -> EstadoErro(
                mensagem = stringResource(R.string.documentos_erro_carregar),
                modifier = Modifier.padding(margens),
                onTentarNovamente = viewModel::tentarNovamente,
            )
            is EstadoLista.Dados -> {
                val entrada = rememberEntradaDeLista()
                val visiveis = atual.itens.filter { filtro == null || it.status == filtro }
                val direcao = LocalLayoutDirection.current

                LazyColumn(
                    state = lista,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = margens.calculateStartPadding(direcao),
                            end = margens.calculateEndPadding(direcao),
                            top = margens.calculateTopPadding(),
                        ),
                    contentPadding = PaddingValues(bottom = margens.calculateBottomPadding() + 104.dp),
                ) {
                    item(key = "titulo") {
                        TituloGrande(
                            titulo = stringResource(R.string.home_admin_titulo),
                            sobretitulo = stringResource(R.string.home_saudacao, usuario.nome),
                            modifier = Modifier.padding(
                                start = Espacos.tela,
                                end = Espacos.tela,
                                top = Espacos.xs,
                                bottom = Espacos.l,
                            ),
                        )
                    }
                    if (atual.itens.isNotEmpty()) {
                        stickyHeader(key = "filtros") {
                            FiltroSituacao(
                                documentos = atual.itens,
                                selecionado = filtro,
                                onSelecionar = viewModel::filtrar,
                            )
                        }
                    }
                    if (visiveis.isEmpty()) {
                        item(key = "vazio") {
                            SemDocumentos(
                                semNenhum = atual.itens.isEmpty(),
                                onNovoDocumento = onNovoDocumento,
                                onVerTodos = { viewModel.filtrar(null) },
                                modifier = Modifier
                                    .fillParentMaxHeight(0.7f)
                                    .animateItem(),
                            )
                        }
                    }
                    itemsIndexed(visiveis, key = { _, documento -> documento.id }) { indice, documento ->
                        ItemDocumento(
                            documento = documento,
                            onClick = { onAbrirDocumento(documento) },
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = Movimento.medio(),
                                    placementSpec = Movimento.lento(),
                                    fadeOutSpec = Movimento.rapido(),
                                )
                                .entradaSuave(entrada.progressoDoItem(indice))
                                .padding(horizontal = Espacos.tela, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FiltroSituacao(
    documentos: List<Documento>,
    selecionado: StatusDocumento?,
    onSelecionar: (StatusDocumento?) -> Unit,
) {
    val filtros = remember { listOf<StatusDocumento?>(null) + StatusDocumento.entries }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = Espacos.xs),
    ) {
        FileiraDeChips(quantidade = filtros.size, indiceSelecionado = filtros.indexOf(selecionado)) { indice ->
            val status = filtros[indice]
            val total = if (status == null) documentos.size else documentos.count { it.status == status }
            ChipDeFiltro(
                texto = stringResource(status?.rotulo ?: R.string.filtro_todos),
                detalhe = total.toString(),
                selecionado = selecionado == status,
                onClick = { onSelecionar(status) },
            )
        }
    }
}

@Composable
private fun SemDocumentos(
    semNenhum: Boolean,
    onNovoDocumento: () -> Unit,
    onVerTodos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (semNenhum) {
        EstadoVazio(
            icone = Icones.DocumentoNovo,
            titulo = stringResource(R.string.home_admin_vazio),
            descricao = stringResource(R.string.home_admin_vazio_dica),
            modifier = modifier,
            acao = {
                BotaoPrincipal(
                    texto = stringResource(R.string.home_admin_novo_documento),
                    onClick = onNovoDocumento,
                    icone = Icones.Mais,
                )
            },
        )
    } else {
        EstadoVazio(
            icone = Icones.Lupa,
            titulo = stringResource(R.string.home_admin_vazio_filtro),
            modifier = modifier,
            acao = { BotaoDeTexto(texto = stringResource(R.string.home_admin_ver_todos), onClick = onVerTodos) },
        )
    }
}

@Composable
private fun ItemDocumento(documento: Documento, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Cartao(onClick = onClick, margemInterna = PaddingValues(18.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChipStatus(documento.status)
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icones.Avancar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(Espacos.s))
            Text(
                text = documento.titulo,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Espacos.xxs))
            Text(
                text = documento.enviadoEm?.let {
                    stringResource(R.string.documento_enviado_em, formatarDataHora(it))
                } ?: stringResource(R.string.documento_criado_em, formatarDataHora(documento.criadoEm)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.documento_arquivo_versao, documento.nomeArquivo, documento.versao),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
