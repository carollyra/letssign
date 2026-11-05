package br.com.letssign.ui.signatarios

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.EstadoLista
import br.com.letssign.ui.common.EstadoVazio
import br.com.letssign.ui.common.formatarTelefone

@Composable
fun SignatariosScreen(
    onVoltar: () -> Unit,
    onNovo: () -> Unit,
    onEditar: (Long) -> Unit,
    viewModel: SignatariosViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BarraSuperior(titulo = stringResource(R.string.signatarios_titulo), onVoltar = onVoltar)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovo,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.signatarios_novo)) },
            )
        },
    ) { margens ->
        when (val atual = estado) {
            EstadoLista.Carregando -> Carregando(Modifier.padding(margens))
            EstadoLista.Erro -> EstadoErro(
                mensagem = stringResource(R.string.signatarios_erro_carregar),
                modifier = Modifier.padding(margens),
                onTentarNovamente = viewModel::tentarNovamente,
            )
            is EstadoLista.Dados -> if (atual.itens.isEmpty()) {
                EstadoVazio(
                    titulo = stringResource(R.string.signatarios_vazio_titulo),
                    descricao = stringResource(R.string.signatarios_vazio_descricao),
                    modifier = Modifier.padding(margens),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(margens),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(atual.itens, key = { it.id }) { signatario ->
                        ListItem(
                            headlineContent = { Text(signatario.nome) },
                            supportingContent = {
                                Text("${signatario.email}\n${formatarTelefone(signatario.telefone)}")
                            },
                            trailingContent = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.acao_editar),
                                )
                            },
                            modifier = Modifier.clickable { onEditar(signatario.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
