package br.com.letssign.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.letssign.domain.model.Usuario
import br.com.letssign.domain.repository.AutenticacaoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EstadoSessao {
    data object Carregando : EstadoSessao
    data object Deslogado : EstadoSessao
    data class Logado(val usuario: Usuario) : EstadoSessao
}

@HiltViewModel
class SessaoViewModel @Inject constructor(
    private val autenticacao: AutenticacaoRepository,
) : ViewModel() {

    val estado: StateFlow<EstadoSessao> = autenticacao.usuarioLogado
        .map { if (it == null) EstadoSessao.Deslogado else EstadoSessao.Logado(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoSessao.Carregando)

    fun sair() {
        viewModelScope.launch { autenticacao.sair() }
    }
}
