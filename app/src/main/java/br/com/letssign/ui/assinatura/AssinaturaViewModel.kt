package br.com.letssign.ui.assinatura

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.letssign.R
import br.com.letssign.domain.model.DocumentoParaAssinar
import br.com.letssign.domain.model.StatusAssinatura
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.domain.repository.DocumentoRepository
import br.com.letssign.domain.usecase.AssinarDocumentoUseCase
import br.com.letssign.domain.usecase.ObterUsuarioLogadoUseCase
import br.com.letssign.domain.usecase.ResultadoAssinatura
import br.com.letssign.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssinaturaUiState(
    val carregando: Boolean = true,
    val erroCarregar: Boolean = false,
    val dados: DocumentoParaAssinar? = null,
    val capturando: Boolean = false,
    val assinando: Boolean = false,
    val acabouDeAssinar: Boolean = false,
    @StringRes val erro: Int? = null,
) {
    val podeAssinar: Boolean
        get() = dados != null &&
            dados.vinculo.status == StatusAssinatura.PENDENTE &&
            dados.documento.status != StatusDocumento.CONCLUIDO
}

@HiltViewModel
class AssinaturaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usuarioLogado: ObterUsuarioLogadoUseCase,
    private val documentos: DocumentoRepository,
    private val assinarDocumento: AssinarDocumentoUseCase,
) : ViewModel() {

    private val documentoId: Long = checkNotNull(savedStateHandle[Rotas.ARG_DOCUMENTO_ID])

    private val _estado = MutableStateFlow(AssinaturaUiState())
    val estado: StateFlow<AssinaturaUiState> = _estado.asStateFlow()

    init {
        carregar()
    }

    fun carregar() {
        _estado.update { it.copy(carregando = it.dados == null, erroCarregar = false) }
        viewModelScope.launch {
            val dados = try {
                documentos.buscarParaAssinar(documentoId, usuarioLogado.signatario().id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _estado.update {
                if (dados == null) {
                    it.copy(carregando = false, erroCarregar = true)
                } else {
                    it.copy(carregando = false, dados = dados)
                }
            }
        }
    }

    fun abrirCaptura() {
        if (_estado.value.podeAssinar) _estado.update { it.copy(capturando = true, erro = null) }
    }

    fun cancelarCaptura() {
        _estado.update { it.copy(capturando = false) }
    }

    fun confirmarAssinatura(assinaturaPng: ByteArray) {
        if (_estado.value.assinando) return
        _estado.update { it.copy(capturando = false, assinando = true, erro = null) }
        viewModelScope.launch {
            val resultado = try {
                assinarDocumento(documentoId, assinaturaPng)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _estado.update {
                when (resultado) {
                    is ResultadoAssinatura.Assinado -> it.copy(assinando = false, acabouDeAssinar = true)
                    ResultadoAssinatura.JaAssinado -> it.copy(assinando = false)
                    ResultadoAssinatura.DocumentoConcluido ->
                        it.copy(assinando = false, erro = R.string.assinatura_erro_concluido)
                    ResultadoAssinatura.AssinaturaVazia ->
                        it.copy(assinando = false, erro = R.string.assinatura_erro_vazia)
                    ResultadoAssinatura.FalhaAoGravar ->
                        it.copy(assinando = false, erro = R.string.assinatura_erro_gravar)
                    null -> it.copy(assinando = false, erro = R.string.erro_inesperado)
                }
            }
            carregar()
        }
    }
}
