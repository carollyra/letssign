package br.com.letssign.ui.documento

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.letssign.R
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.domain.model.Usuario
import br.com.letssign.domain.repository.DocumentoRepository
import br.com.letssign.domain.repository.SelecaoSignatario
import br.com.letssign.domain.repository.SignatarioRepository
import br.com.letssign.domain.usecase.DefinirSignatariosUseCase
import br.com.letssign.domain.usecase.ObterUsuarioLogadoUseCase
import br.com.letssign.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [papel] nulo significa que o signatário não está selecionado para o documento. */
data class ItemSelecao(val usuario: Usuario, val papel: PapelSignatario?)

data class SelecaoSignatariosUiState(
    val carregando: Boolean = true,
    val erroCarregar: Boolean = false,
    val itens: List<ItemSelecao> = emptyList(),
    val erroMinimo: Boolean = false,
    val salvando: Boolean = false,
    @StringRes val erroGeral: Int? = null,
) {
    val totalSelecionados: Int get() = itens.count { it.papel != null }
}

@HiltViewModel
class SelecaoSignatariosViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usuarioLogado: ObterUsuarioLogadoUseCase,
    private val documentos: DocumentoRepository,
    private val signatarios: SignatarioRepository,
    private val definirSignatarios: DefinirSignatariosUseCase,
) : ViewModel() {

    private val documentoId: Long = checkNotNull(savedStateHandle[Rotas.ARG_DOCUMENTO_ID])

    private val selecao = MutableStateFlow<Map<Long, PapelSignatario>>(emptyMap())
    private var selecaoCarregada = false
    private var observacao: Job? = null

    private val _estado = MutableStateFlow(SelecaoSignatariosUiState())
    val estado: StateFlow<SelecaoSignatariosUiState> = _estado.asStateFlow()

    private val _proximaEtapa = Channel<Long>(Channel.BUFFERED)
    val proximaEtapa = _proximaEtapa.receiveAsFlow()

    init {
        carregar()
    }

    fun carregar() {
        observacao?.cancel()
        _estado.update { it.copy(carregando = true, erroCarregar = false) }
        observacao = viewModelScope.launch {
            try {
                if (!selecaoCarregada) {
                    val detalhe = documentos.buscarDetalheDoAdmin(documentoId, usuarioLogado.admin().id)
                    if (detalhe == null || !detalhe.documento.editavel) {
                        _estado.update { it.copy(carregando = false, erroCarregar = true) }
                        return@launch
                    }
                    selecao.value = detalhe.signatarios.associate { it.usuario.id to it.vinculo.papel }
                    selecaoCarregada = true
                }
                combine(signatarios.observarTodos(), selecao) { todos, escolhidos ->
                    todos.map { ItemSelecao(it, escolhidos[it.id]) }
                }.collect { itens ->
                    _estado.update { it.copy(carregando = false, itens = itens) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(carregando = false, erroCarregar = true) }
            }
        }
    }

    fun alternar(signatarioId: Long) {
        selecao.update { atual ->
            if (signatarioId in atual) atual - signatarioId else atual + (signatarioId to papelSugerido(atual))
        }
        _estado.update { it.copy(erroMinimo = false, erroGeral = null) }
    }

    fun definirPapel(signatarioId: Long, papel: PapelSignatario) {
        selecao.update { atual -> if (signatarioId in atual) atual + (signatarioId to papel) else atual }
    }

    fun continuar() {
        if (_estado.value.salvando) return
        val escolhidos = selecao.value.map { (id, papel) -> SelecaoSignatario(id, papel) }
        if (escolhidos.isEmpty()) {
            _estado.update { it.copy(erroMinimo = true) }
            return
        }
        _estado.update { it.copy(salvando = true, erroGeral = null) }
        viewModelScope.launch {
            val salvo = try {
                definirSignatarios(documentoId, escolhidos)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _estado.update {
                it.copy(
                    salvando = false,
                    erroMinimo = salvo == false,
                    erroGeral = R.string.erro_inesperado.takeIf { salvo == null },
                )
            }
            if (salvo == true) _proximaEtapa.send(documentoId)
        }
    }

    /** Sugere o primeiro papel ainda sem ninguém, na ordem contratante, contratado, testemunha. */
    private fun papelSugerido(atual: Map<Long, PapelSignatario>): PapelSignatario =
        PapelSignatario.entries.firstOrNull { it !in atual.values } ?: PapelSignatario.TESTEMUNHA
}
