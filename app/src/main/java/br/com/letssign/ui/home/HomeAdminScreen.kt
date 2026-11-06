package br.com.letssign.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.EstadoLista
import br.com.letssign.ui.common.EstadoVazio
import br.com.letssign.ui.common.EtiquetaStatus
import br.com.letssign.ui.common.FileiraDeChips
import br.com.letssign.ui.common.formatarDataHora
import br.com.letssign.ui.common.rotulo

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

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = stringResource(R.string.home_admin_titulo),
                acoes = {
                    TextButton(onClick = onSignatarios) {
                        Text(stringResource(R.string.signatarios_titulo))
                    }
                    TextButton(onClick = onSair) { Text(stringResource(R.string.acao_sair)) }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovoDocumento,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.home_admin_novo_documento)) },
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
            is EstadoLista.Dados -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(margens),
            ) {
                Text(
                    text = stringResource(R.string.home_saudacao, usuario.nome),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                FiltroSituacao(documentos = atual.itens, selecionado = filtro, onSelecionar = viewModel::filtrar)

                val visiveis = atual.itens.filter { filtro == null || it.status == filtro }
                if (visiveis.isEmpty()) {
                    EstadoVazio(
                        titulo = stringResource(
                            if (atual.itens.isEmpty()) R.string.home_admin_vazio else R.string.home_admin_vazio_filtro,
                        ),
                        descricao = stringResource(R.string.home_admin_vazio_dica)
                            .takeIf { atual.itens.isEmpty() },
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(visiveis, key = { it.id }) { documento ->
                            ItemDocumento(documento = documento, onClick = { onAbrirDocumento(documento) })
                        }
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
    val filtros = listOf<StatusDocumento?>(null) + StatusDocumento.entries
    FileiraDeChips(
        quantidade = filtros.size,
        indiceSelecionado = filtros.indexOf(selecionado),
        modifier = Modifier.padding(vertical = 4.dp),
    ) { indice ->
        val status = filtros[indice]
        FilterChip(
            selected = selecionado == status,
            onClick = { onSelecionar(status) },
            label = {
                Text(
                    text = if (status == null) {
                        stringResource(R.string.filtro_todos, documentos.size)
                    } else {
                        stringResource(
                            R.string.filtro_com_total,
                            stringResource(status.rotulo),
                            documentos.count { it.status == status },
                        )
                    },
                    maxLines = 1,
                    softWrap = false,
                )
            },
        )
    }
}

@Composable
private fun ItemDocumento(documento: Documento, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = documento.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                )
                EtiquetaStatus(documento.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.documento_arquivo_versao, documento.nomeArquivo, documento.versao),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = documento.enviadoEm?.let {
                    stringResource(R.string.documento_enviado_em, formatarDataHora(it))
                } ?: stringResource(R.string.documento_criado_em, formatarDataHora(documento.criadoEm)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
