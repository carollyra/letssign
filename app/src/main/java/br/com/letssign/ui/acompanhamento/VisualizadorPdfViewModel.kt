package br.com.letssign.ui.acompanhamento

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.letssign.domain.model.Documento
import br.com.letssign.domain.repository.DocumentoRepository
import br.com.letssign.domain.usecase.ObterUsuarioLogadoUseCase
import br.com.letssign.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EstadoVisualizador {
    data object Carregando : EstadoVisualizador
    data object Erro : EstadoVisualizador
    data class Pronto(val documento: Documento) : EstadoVisualizador
}

@HiltViewModel
class VisualizadorPdfViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usuarioLogado: ObterUsuarioLogadoUseCase,
    private val documentos: DocumentoRepository,
) : ViewModel() {

    private val documentoId: Long = checkNotNull(savedStateHandle[Rotas.ARG_DOCUMENTO_ID])

    private val _estado = MutableStateFlow<EstadoVisualizador>(EstadoVisualizador.Carregando)
    val estado: StateFlow<EstadoVisualizador> = _estado.asStateFlow()

    init {
        carregar()
    }

    fun carregar() {
        _estado.value = EstadoVisualizador.Carregando
        viewModelScope.launch {
            val documento = try {
                documentos.buscarDetalheDoAdmin(documentoId, usuarioLogado.admin().id)?.documento
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _estado.value = documento?.let { EstadoVisualizador.Pronto(it) } ?: EstadoVisualizador.Erro
        }
    }
}
