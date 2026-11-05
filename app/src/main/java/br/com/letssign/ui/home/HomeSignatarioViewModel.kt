package br.com.letssign.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.letssign.domain.model.DocumentoDoSignatario
import br.com.letssign.domain.repository.AutenticacaoRepository
import br.com.letssign.domain.repository.DocumentoRepository
import br.com.letssign.ui.common.EstadoLista
import br.com.letssign.ui.common.comoEstadoLista
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeSignatarioViewModel @Inject constructor(
    autenticacao: AutenticacaoRepository,
    private val documentos: DocumentoRepository,
) : ViewModel() {

    private val tentativa = MutableStateFlow(0)

    val estado: StateFlow<EstadoLista<DocumentoDoSignatario>> = tentativa
        .flatMapLatest {
            autenticacao.usuarioLogado
                .flatMapLatest { usuario ->
                    if (usuario == null) flowOf(emptyList()) else documentos.observarDoSignatario(usuario.id)
                }
                .comoEstadoLista()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoLista.Carregando)

    fun tentarNovamente() = tentativa.update { it + 1 }
}
