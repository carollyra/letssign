package br.com.letssign.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.domain.model.Perfil
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.login.LoginScreen

@Composable
fun LetsSignRaiz(viewModel: SessaoViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    when (val sessao = estado) {
        EstadoSessao.Carregando -> Carregando()
        EstadoSessao.Deslogado -> LoginScreen()
        is EstadoSessao.Logado ->
            // Cada login monta um grafo novo, para a pilha de telas de um perfil não vazar para o outro.
            key(sessao.usuario.id) {
                when (sessao.usuario.perfil) {
                    Perfil.ADMIN -> AdminNavHost(usuario = sessao.usuario, onSair = viewModel::sair)
                    Perfil.SIGNATARIO -> SignatarioNavHost(usuario = sessao.usuario, onSair = viewModel::sair)
                }
            }
    }
}
