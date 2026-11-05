package br.com.letssign.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.letssign.domain.model.Usuario
import br.com.letssign.ui.home.HomeSignatarioScreen

@Composable
fun SignatarioNavHost(usuario: Usuario, onSair: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Rotas.HOME) {
        composable(Rotas.HOME) {
            HomeSignatarioScreen(usuario = usuario, onSair = onSair)
        }
    }
}
