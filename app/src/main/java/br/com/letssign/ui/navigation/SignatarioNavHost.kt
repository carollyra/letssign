package br.com.letssign.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.letssign.domain.model.Usuario
import br.com.letssign.ui.assinatura.AssinaturaScreen
import br.com.letssign.ui.home.HomeSignatarioScreen

@Composable
fun SignatarioNavHost(usuario: Usuario, onSair: () -> Unit) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Rotas.HOME,
        enterTransition = Transicoes.entrar,
        exitTransition = Transicoes.sair,
        popEnterTransition = Transicoes.voltarEntrar,
        popExitTransition = Transicoes.voltarSair,
    ) {
        composable(Rotas.HOME) {
            HomeSignatarioScreen(
                usuario = usuario,
                onSair = onSair,
                onAbrirDocumento = { navController.navigate(Rotas.documentoAssinatura(it.documento.id)) },
            )
        }
        composable(
            route = Rotas.DOCUMENTO_ASSINATURA,
            arguments = listOf(navArgument(Rotas.ARG_DOCUMENTO_ID) { type = NavType.LongType }),
        ) {
            AssinaturaScreen(onVoltar = navController::popBackStack)
        }
    }
}
