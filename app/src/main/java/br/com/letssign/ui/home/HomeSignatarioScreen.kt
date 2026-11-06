package br.com.letssign.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.model.DocumentoDoSignatario
import br.com.letssign.domain.model.StatusAssinatura
import br.com.letssign.domain.model.Usuario
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.BotaoDeTexto
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.Cartao
import br.com.letssign.ui.common.ChipAssinatura
import br.com.letssign.ui.common.ControleSegmentado
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.EstadoLista
import br.com.letssign.ui.common.EstadoVazio
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
fun HomeSignatarioScreen(
    usuario: Usuario,
    onSair: () -> Unit,
    onAbrirDocumento: (DocumentoDoSignatario) -> Unit,
    viewModel: HomeSignatarioViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var aba by rememberSaveable { mutableStateOf(StatusAssinatura.PENDENTE) }
    val lista = rememberLazyListState()
    val tituloSaiuDaTela by remember { derivedStateOf { lista.firstVisibleItemIndex > 0 } }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = stringResource(R.string.home_signatario_titulo),
                tituloVisivel = estado !is EstadoLista.Dados || tituloSaiuDaTela,
                acoes = { MenuDaConta(usuario = usuario, onSair = onSair) },
            )
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
                val visiveis = atual.itens.filter { it.vinculo.status == aba }
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
                    contentPadding = PaddingValues(bottom = margens.calculateBottomPadding() + Espacos.xl),
                ) {
                    item(key = "titulo") {
                        TituloGrande(
                            titulo = stringResource(R.string.home_signatario_titulo),
                            sobretitulo = stringResource(R.string.home_saudacao, usuario.nome),
                            modifier = Modifier.padding(
                                start = Espacos.tela,
                                end = Espacos.tela,
                                top = Espacos.xs,
                                bottom = Espacos.l,
                            ),
                        )
                    }
                    stickyHeader(key = "abas") {
                        Abas(itens = atual.itens, selecionada = aba, onSelecionar = { aba = it })
                    }
                    if (visiveis.isEmpty()) {
                        item(key = "vazio") {
                            SemDocumentos(
                                aba = aba,
                                haNaOutraAba = atual.itens.isNotEmpty(),
                                onTrocarAba = {
                                    aba = if (aba == StatusAssinatura.PENDENTE) {
                                        StatusAssinatura.ASSINADO
                                    } else {
                                        StatusAssinatura.PENDENTE
                                    }
                                },
                                modifier = Modifier
                                    .fillParentMaxHeight(0.7f)
                                    .animateItem(),
                            )
                        }
                    }
                    itemsIndexed(visiveis, key = { _, item -> item.documento.id }) { indice, item ->
                        ItemDoSignatario(
                            item = item,
                            onClick = { onAbrirDocumento(item) },
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
private fun Abas(
    itens: List<DocumentoDoSignatario>,
    selecionada: StatusAssinatura,
    onSelecionar: (StatusAssinatura) -> Unit,
) {
    val abas = StatusAssinatura.entries
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = Espacos.tela, end = Espacos.tela, bottom = Espacos.xs),
    ) {
        ControleSegmentado(
            opcoes = abas.map { status ->
                stringResource(
                    R.string.filtro_com_total,
                    stringResource(
                        if (status == StatusAssinatura.PENDENTE) {
                            R.string.home_signatario_pendentes
                        } else {
                            R.string.home_signatario_assinados
                        },
                    ),
                    itens.count { it.vinculo.status == status },
                )
            },
            indiceSelecionado = abas.indexOf(selecionada),
            onSelecionar = { onSelecionar(abas[it]) },
        )
    }
}

@Composable
private fun SemDocumentos(
    aba: StatusAssinatura,
    haNaOutraAba: Boolean,
    onTrocarAba: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pendentes = aba == StatusAssinatura.PENDENTE
    EstadoVazio(
        icone = if (pendentes) Icones.CheckCirculo else Icones.Assinar,
        titulo = stringResource(
            if (pendentes) R.string.home_signatario_vazio_pendentes else R.string.home_signatario_vazio_assinados,
        ),
        descricao = stringResource(R.string.home_signatario_vazio_pendentes_descricao).takeIf { pendentes },
        modifier = modifier,
        acao = if (haNaOutraAba) {
            {
                BotaoDeTexto(
                    texto = stringResource(
                        if (pendentes) {
                            R.string.home_signatario_ver_assinados
                        } else {
                            R.string.home_signatario_ver_pendentes
                        },
                    ),
                    onClick = onTrocarAba,
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun ItemDoSignatario(item: DocumentoDoSignatario, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Cartao(onClick = onClick, margemInterna = PaddingValues(18.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChipAssinatura(item.vinculo.status)
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
                text = item.documento.titulo,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Espacos.xxs))
            Text(
                text = stringResource(R.string.home_signatario_papel, stringResource(item.vinculo.papel.rotulo)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val data = item.vinculo.assinadoEm?.let {
                stringResource(R.string.documento_assinado_em, formatarDataHora(it))
            } ?: item.documento.enviadoEm?.let {
                stringResource(R.string.documento_recebido_em, formatarDataHora(it))
            }
            if (data != null) {
                Text(
                    text = data,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
