package br.com.letssign.ui.acompanhamento

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.letssign.domain.model.DocumentoDetalhado
import br.com.letssign.domain.model.EventoDetalhado
import br.com.letssign.domain.repository.AutenticacaoRepository
import br.com.letssign.domain.repository.DocumentoRepository
import br.com.letssign.domain.usecase.CriarNovaVersaoUseCase
import br.com.letssign.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface EstadoAcompanhamento {
    data object Carregando : EstadoAcompanhamento
    data object NaoEncontrado : EstadoAcompanhamento
    data object Erro : EstadoAcompanhamento
    data class Dados(
        val detalhe: DocumentoDetalhado,
        val eventos: List<EventoDetalhado>,
    ) : EstadoAcompanhamento
}

data class AcaoNovaVersao(val emAndamento: Boolean = false, val falhou: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AcompanhamentoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    autenticacao: AutenticacaoRepository,
    private val documentos: DocumentoRepository,
    private val criarNovaVersao: CriarNovaVersaoUseCase,
) : ViewModel() {

    private val documentoId: Long = checkNotNull(savedStateHandle[Rotas.ARG_DOCUMENTO_ID])
    private val tentativa = MutableStateFlow(0)

    val estado: StateFlow<EstadoAcompanhamento> = tentativa
        .flatMapLatest {
            autenticacao.usuarioLogado.flatMapLatest { usuario ->
                if (usuario == null) flowOf(EstadoAcompanhamento.NaoEncontrado) else observar(usuario.id)
            }.catch { emit(EstadoAcompanhamento.Erro) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoAcompanhamento.Carregando)

    private val _novaVersao = MutableStateFlow(AcaoNovaVersao())
    val novaVersao: StateFlow<AcaoNovaVersao> = _novaVersao.asStateFlow()

    private val _rascunhoCriado = Channel<Long>(Channel.BUFFERED)
    val rascunhoCriado = _rascunhoCriado.receiveAsFlow()

    fun tentarNovamente() = tentativa.update { it + 1 }

    fun criarVersao() {
        if (_novaVersao.value.emAndamento) return
        _novaVersao.value = AcaoNovaVersao(emAndamento = true)
        viewModelScope.launch {
            val novoId = try {
                criarNovaVersao(documentoId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _novaVersao.value = AcaoNovaVersao(falhou = novoId == null)
            if (novoId != null) _rascunhoCriado.send(novoId)
        }
    }

    private fun observar(adminId: Long): Flow<EstadoAcompanhamento> = combine(
        documentos.observarDetalheDoAdmin(documentoId, adminId),
        documentos.observarEventos(documentoId, adminId),
    ) { detalhe, eventos ->
        if (detalhe == null) EstadoAcompanhamento.NaoEncontrado else EstadoAcompanhamento.Dados(detalhe, eventos)
    }
}
