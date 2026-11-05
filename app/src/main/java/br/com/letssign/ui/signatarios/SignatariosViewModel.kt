package br.com.letssign.ui.signatarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.letssign.domain.model.Usuario
import br.com.letssign.domain.repository.SignatarioRepository
import br.com.letssign.ui.common.EstadoLista
import br.com.letssign.ui.common.comoEstadoLista
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class SignatariosViewModel @Inject constructor(
    private val signatarios: SignatarioRepository,
) : ViewModel() {

    private val tentativa = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<EstadoLista<Usuario>> = tentativa
        .flatMapLatest { signatarios.observarTodos().comoEstadoLista() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoLista.Carregando)

    fun tentarNovamente() = tentativa.update { it + 1 }
}
