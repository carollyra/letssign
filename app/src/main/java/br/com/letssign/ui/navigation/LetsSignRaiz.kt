package br.com.letssign.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.domain.model.Perfil
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.login.LoginScreen
import br.com.letssign.ui.theme.Movimento

@Composable
fun LetsSignRaiz(viewModel: SessaoViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = estado,
        // Cada login monta um grafo novo, para a pilha de telas de um perfil não vazar para o outro.
        contentKey = { sessao ->
            when (sessao) {
                EstadoSessao.Carregando -> "carregando"
                EstadoSessao.Deslogado -> "login"
                is EstadoSessao.Logado -> sessao.usuario.id
            }
        },
        transitionSpec = { fadeIn(tween(Movimento.LENTO)) togetherWith fadeOut(tween(Movimento.RAPIDO)) },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        label = "sessao",
    ) { sessao ->
        when (sessao) {
            EstadoSessao.Carregando -> Carregando()
            EstadoSessao.Deslogado -> LoginScreen()
            is EstadoSessao.Logado -> when (sessao.usuario.perfil) {
                Perfil.ADMIN -> AdminNavHost(usuario = sessao.usuario, onSair = viewModel::sair)
                Perfil.SIGNATARIO -> SignatarioNavHost(usuario = sessao.usuario, onSair = viewModel::sair)
            }
        }
    }
}
