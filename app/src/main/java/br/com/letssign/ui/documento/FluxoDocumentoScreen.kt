package br.com.letssign.ui.documento

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.letssign.R
import br.com.letssign.ui.campos.PosicionamentoCamposScreen
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.navigation.Rotas
import br.com.letssign.ui.navigation.Transicoes
import br.com.letssign.ui.revisao.RevisaoScreen
import br.com.letssign.ui.theme.Espacos
import br.com.letssign.ui.theme.Movimento

/**
 * Casca das quatro etapas do documento. A barra e o indicador ficam parados enquanto só o
 * conteúdo da etapa desliza, e por isso as etapas têm um grafo de navegação próprio aqui dentro.
 */
@Composable
fun FluxoDocumentoScreen(
    documentoId: Long,
    onFechar: () -> Unit,
    onCadastrarSignatario: () -> Unit,
    onConcluir: () -> Unit,
    onAcompanhar: (Long) -> Unit,
) {
    val navController = rememberNavController()
    val entradaAtual by navController.currentBackStackEntryAsState()
    val etapa = etapaDaRota(entradaAtual?.destination?.route)
    var enviado by rememberSaveable { mutableStateOf(false) }
    val argumentoDocumento = navArgument(Rotas.ARG_DOCUMENTO_ID) { type = NavType.LongType }

    val voltar: () -> Unit = {
        if (navController.previousBackStackEntry != null) navController.popBackStack() else onFechar()
    }

    Scaffold(
        topBar = {
            // Depois do envio não há mais etapas: a confirmação ocupa a tela inteira.
            AnimatedVisibility(
                visible = !enviado,
                enter = expandVertically(Movimento.medio()) + fadeIn(Movimento.medio()),
                exit = shrinkVertically(Movimento.medio()) + fadeOut(Movimento.rapido()),
            ) {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    BarraSuperior(titulo = stringResource(R.string.documento_novo_titulo), onVoltar = voltar)
                    IndicadorEtapas(etapa = etapa, modifier = Modifier.padding(bottom = Espacos.s))
                }
            }
        },
    ) { margens ->
        NavHost(
            navController = navController,
            startDestination = Rotas.DOCUMENTO_ARQUIVO,
            modifier = Modifier
                .fillMaxSize()
                .padding(margens)
                .consumeWindowInsets(margens),
            enterTransition = Transicoes.etapaEntrar,
            exitTransition = Transicoes.etapaSair,
            popEnterTransition = Transicoes.etapaVoltarEntrar,
            popExitTransition = Transicoes.etapaVoltarSair,
        ) {
            composable(
                route = Rotas.DOCUMENTO_ARQUIVO,
                arguments = listOf(
                    navArgument(Rotas.ARG_DOCUMENTO_ID) {
                        type = NavType.LongType
                        defaultValue = documentoId
                    },
                ),
            ) {
                NovoDocumentoScreen(
                    onProximaEtapa = { navController.navigate(Rotas.documentoSignatarios(it)) },
                )
            }
            composable(route = Rotas.DOCUMENTO_SIGNATARIOS, arguments = listOf(argumentoDocumento)) {
                SelecaoSignatariosScreen(
                    onVoltar = voltar,
                    onCadastrarSignatario = onCadastrarSignatario,
                    onProximaEtapa = { navController.navigate(Rotas.documentoCampos(it)) },
                )
            }
            composable(route = Rotas.DOCUMENTO_CAMPOS, arguments = listOf(argumentoDocumento)) {
                PosicionamentoCamposScreen(
                    onVoltar = voltar,
                    onProximaEtapa = { navController.navigate(Rotas.documentoRevisao(it)) },
                )
            }
            composable(route = Rotas.DOCUMENTO_REVISAO, arguments = listOf(argumentoDocumento)) {
                RevisaoScreen(
                    onVoltar = voltar,
                    onConcluir = onConcluir,
                    onAcompanhar = onAcompanhar,
                    onEnviado = { enviado = it },
                )
            }
        }
    }
}

private fun etapaDaRota(rota: String?): EtapaDocumento = when (rota) {
    Rotas.DOCUMENTO_SIGNATARIOS -> EtapaDocumento.SIGNATARIOS
    Rotas.DOCUMENTO_CAMPOS -> EtapaDocumento.CAMPOS
    Rotas.DOCUMENTO_REVISAO -> EtapaDocumento.REVISAO
    else -> EtapaDocumento.ARQUIVO
}
