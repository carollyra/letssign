package br.com.letssign.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.letssign.domain.model.Usuario
import br.com.letssign.ui.home.HomeAdminScreen
import br.com.letssign.ui.signatarios.SignatarioFormScreen
import br.com.letssign.ui.signatarios.SignatariosScreen

@Composable
fun AdminNavHost(usuario: Usuario, onSair: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Rotas.HOME) {
        composable(Rotas.HOME) {
            HomeAdminScreen(
                usuario = usuario,
                onSair = onSair,
                onSignatarios = { navController.navigate(Rotas.SIGNATARIOS) },
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
    }
}
