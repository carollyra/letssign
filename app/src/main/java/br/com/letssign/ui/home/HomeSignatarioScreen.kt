package br.com.letssign.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.EstadoLista
import br.com.letssign.ui.common.EstadoVazio
import br.com.letssign.ui.common.ChipAssinatura
import br.com.letssign.ui.common.formatarDataHora
import br.com.letssign.ui.common.rotulo

@Composable
fun HomeSignatarioScreen(
    usuario: Usuario,
    onSair: () -> Unit,
    onAbrirDocumento: (DocumentoDoSignatario) -> Unit,
    viewModel: HomeSignatarioViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var aba by rememberSaveable { mutableStateOf(StatusAssinatura.PENDENTE) }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = stringResource(R.string.home_signatario_titulo),
                acoes = {
                    TextButton(onClick = onSair) { Text(stringResource(R.string.acao_sair)) }
                },
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                PrimaryTabRow(selectedTabIndex = aba.ordinal) {
                    StatusAssinatura.entries.forEach { status ->
                        Tab(
                            selected = aba == status,
                            onClick = { aba = status },
                            text = {
                                Text(
                                    stringResource(
                                        R.string.filtro_com_total,
                                        stringResource(
                                            if (status == StatusAssinatura.PENDENTE) {
                                                R.string.home_signatario_pendentes
                                            } else {
                                                R.string.home_signatario_assinados
                                            },
                                        ),
                                        atual.itens.count { it.vinculo.status == status },
                                    ),
                                )
                            },
                        )
                    }
                }

                val visiveis = atual.itens.filter { it.vinculo.status == aba }
                if (visiveis.isEmpty()) {
                    EstadoVazio(
                        titulo = stringResource(
                            if (aba == StatusAssinatura.PENDENTE) {
                                R.string.home_signatario_vazio_pendentes
                            } else {
                                R.string.home_signatario_vazio_assinados
                            },
                        ),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(visiveis, key = { it.documento.id }) { item ->
                            ItemDoSignatario(item = item, onClick = { onAbrirDocumento(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemDoSignatario(item: DocumentoDoSignatario, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.documento.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                )
                ChipAssinatura(item.vinculo.status)
            }
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
