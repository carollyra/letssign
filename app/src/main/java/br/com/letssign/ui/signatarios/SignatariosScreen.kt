package br.com.letssign.ui.signatarios

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import br.com.letssign.ui.common.AvatarDoSignatario
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.BotaoFlutuante
import br.com.letssign.ui.common.BotaoPrincipal
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.Cartao
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.common.EstadoLista
import br.com.letssign.ui.common.EstadoVazio
import br.com.letssign.ui.common.Icones
import br.com.letssign.ui.common.corDoSignatario
import br.com.letssign.ui.common.entradaSuave
import br.com.letssign.ui.common.formatarTelefone
import br.com.letssign.ui.common.progressoDoItem
import br.com.letssign.ui.common.rememberEntradaDeLista
import br.com.letssign.ui.theme.Espacos
import br.com.letssign.ui.theme.Movimento

@Composable
fun SignatariosScreen(
    onVoltar: () -> Unit,
    onNovo: () -> Unit,
    onEditar: (Long) -> Unit,
    viewModel: SignatariosViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val temSignatarios = (estado as? EstadoLista.Dados)?.itens?.isNotEmpty() == true

    Scaffold(
        topBar = {
            BarraSuperior(titulo = stringResource(R.string.signatarios_titulo), onVoltar = onVoltar)
        },
        floatingActionButton = {
            // Sem signatários, a ação fica no próprio estado vazio.
            AnimatedVisibility(
                visible = temSignatarios,
                enter = scaleIn(Movimento.medio()) + fadeIn(Movimento.medio()),
                exit = scaleOut(Movimento.rapido()) + fadeOut(Movimento.rapido()),
            ) {
                BotaoFlutuante(
                    texto = stringResource(R.string.signatarios_novo),
                    icone = Icones.Mais,
                    onClick = onNovo,
                )
            }
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
                    icone = Icones.Pessoas,
                    titulo = stringResource(R.string.signatarios_vazio_titulo),
                    descricao = stringResource(R.string.signatarios_vazio_descricao),
                    modifier = Modifier.padding(margens),
                    acao = {
                        BotaoPrincipal(
                            texto = stringResource(R.string.signatarios_novo),
                            onClick = onNovo,
                            icone = Icones.Mais,
                        )
                    },
                )
            } else {
                val entrada = rememberEntradaDeLista()
                LazyColumn(
                    modifier = Modifier.padding(margens),
                    contentPadding = PaddingValues(
                        start = Espacos.tela,
                        end = Espacos.tela,
                        top = Espacos.xs,
                        bottom = 104.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Espacos.s),
                ) {
                    itemsIndexed(atual.itens, key = { _, signatario -> signatario.id }) { indice, signatario ->
                        Cartao(
                            onClick = { onEditar(signatario.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .entradaSuave(entrada.progressoDoItem(indice)),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AvatarDoSignatario(
                                    nome = signatario.nome,
                                    cor = corDoSignatario(indice),
                                    tamanho = 44.dp,
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = Espacos.s),
                                ) {
                                    Text(
                                        text = signatario.nome,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = signatario.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = formatarTelefone(signatario.telefone),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = Icones.Editar,
                                    contentDescription = stringResource(R.string.acao_editar),
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
