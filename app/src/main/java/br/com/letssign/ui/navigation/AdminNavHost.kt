package br.com.letssign.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.letssign.domain.model.Usuario
import br.com.letssign.ui.acompanhamento.AcompanhamentoScreen
import br.com.letssign.ui.acompanhamento.VisualizadorPdfScreen
import br.com.letssign.ui.documento.FluxoDocumentoScreen
import br.com.letssign.ui.home.HomeAdminScreen
import br.com.letssign.ui.signatarios.SignatarioFormScreen
import br.com.letssign.ui.signatarios.SignatariosScreen

@Composable
fun AdminNavHost(usuario: Usuario, onSair: () -> Unit) {
    val navController = rememberNavController()
    val argumentoDocumento = navArgument(Rotas.ARG_DOCUMENTO_ID) { type = NavType.LongType }

    NavHost(
        navController = navController,
        startDestination = Rotas.HOME,
        enterTransition = Transicoes.entrar,
        exitTransition = Transicoes.sair,
        popEnterTransition = Transicoes.voltarEntrar,
        popExitTransition = Transicoes.voltarSair,
    ) {
        composable(Rotas.HOME) {
            HomeAdminScreen(
                usuario = usuario,
                onSair = onSair,
                onSignatarios = { navController.navigate(Rotas.SIGNATARIOS) },
                onNovoDocumento = { navController.navigate(Rotas.documentoFluxo()) },
                onAbrirDocumento = { documento ->
                    navController.navigate(
                        if (documento.editavel) {
                            Rotas.documentoFluxo(documento.id)
                        } else {
                            Rotas.documentoAcompanhamento(documento.id)
                        },
                    )
                },
            )
        }
        composable(Rotas.SIGNATARIOS) {
            SignatariosScreen(
                onVoltar = navController::popBackStack,
                onNovo = { navController.navigate(Rotas.signatarioForm()) },
                onEditar = { navController.navigate(Rotas.signatarioForm(it)) },
            )
        }
        composable(
            route = Rotas.SIGNATARIO_FORM,
            arguments = listOf(
                navArgument(Rotas.ARG_SIGNATARIO_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                },
            ),
        ) {
            SignatarioFormScreen(onVoltar = navController::popBackStack)
        }
        composable(
            route = Rotas.DOCUMENTO_FLUXO,
            arguments = listOf(
                navArgument(Rotas.ARG_DOCUMENTO_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                },
            ),
        ) { entrada ->
            FluxoDocumentoScreen(
                documentoId = entrada.arguments?.getLong(Rotas.ARG_DOCUMENTO_ID) ?: 0L,
                onFechar = navController::popBackStack,
                onCadastrarSignatario = { navController.navigate(Rotas.signatarioForm()) },
                onConcluir = { navController.popBackStack(Rotas.HOME, inclusive = false) },
                onAcompanhar = { id ->
                    navController.navigate(Rotas.documentoAcompanhamento(id)) {
                        popUpTo(Rotas.HOME)
                    }
                },
            )
        }
        composable(route = Rotas.DOCUMENTO_ACOMPANHAMENTO, arguments = listOf(argumentoDocumento)) {
            AcompanhamentoScreen(
                onVoltar = navController::popBackStack,
                onAbrirPdf = { navController.navigate(Rotas.documentoPdf(it)) },
                onEditarRascunho = { id ->
                    navController.navigate(Rotas.documentoFluxo(id)) {
                        popUpTo(Rotas.HOME)
                    }
                },
            )
        }
        composable(route = Rotas.DOCUMENTO_PDF, arguments = listOf(argumentoDocumento)) {
            VisualizadorPdfScreen(onVoltar = navController::popBackStack)
        }
    }
}
