package br.com.letssign.ui.revisao

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.letssign.domain.model.DocumentoDetalhado
import br.com.letssign.domain.regra.ProblemaEnvio
import br.com.letssign.domain.repository.DocumentoRepository
import br.com.letssign.domain.usecase.EnviarDocumentoUseCase
import br.com.letssign.domain.usecase.ObterUsuarioLogadoUseCase
import br.com.letssign.domain.usecase.ResultadoEnvio
import br.com.letssign.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FalhaRevisao { ENVIO, INESPERADA }

data class RevisaoUiState(
    val carregando: Boolean = true,
    val erroCarregar: Boolean = false,
    val detalhe: DocumentoDetalhado? = null,
    val mensagem: String = "",
    val problemas: List<ProblemaEnvio> = emptyList(),
    val enviando: Boolean = false,
    val falha: FalhaRevisao? = null,
    val enviado: Boolean = false,
)

@HiltViewModel
class RevisaoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usuarioLogado: ObterUsuarioLogadoUseCase,
    private val documentos: DocumentoRepository,
    private val enviarDocumento: EnviarDocumentoUseCase,
) : ViewModel() {

    private val documentoId: Long = checkNotNull(savedStateHandle[Rotas.ARG_DOCUMENTO_ID])

    private val _estado = MutableStateFlow(RevisaoUiState())
    val estado: StateFlow<RevisaoUiState> = _estado.asStateFlow()

    private var mensagemPadrao = ""
    private var mensagemEditada = false

    init {
        carregar()
    }

    fun carregar() {
        _estado.update { it.copy(carregando = true, erroCarregar = false) }
        viewModelScope.launch {
            val detalhe = try {
                documentos.buscarDetalheDoAdmin(documentoId, usuarioLogado.admin().id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _estado.update {
                if (detalhe == null) {
                    it.copy(carregando = false, erroCarregar = true)
                } else {
                    it.copy(
                        carregando = false,
                        detalhe = detalhe,
                        mensagem = if (mensagemEditada) {
                            it.mensagem
                        } else {
                            detalhe.documento.mensagemConvite.ifBlank { mensagemPadrao }
                        },
                        enviado = !detalhe.documento.editavel,
                    )
                }
            }
        }
    }

    /** Texto sugerido para o convite enquanto o Admin não escrever o dele. */
    fun definirMensagemPadrao(texto: String) {
        mensagemPadrao = texto
        if (!mensagemEditada) {
            _estado.update { if (it.mensagem.isBlank()) it.copy(mensagem = texto) else it }
        }
    }

    fun aoMudarMensagem(valor: String) {
        mensagemEditada = true
        _estado.update { it.copy(mensagem = valor, problemas = emptyList(), falha = null) }
    }

    fun enviar() {
        val atual = _estado.value
        if (atual.enviando || atual.enviado) return
        _estado.update { it.copy(enviando = true, falha = null, problemas = emptyList()) }
        viewModelScope.launch {
            val resultado = try {
                enviarDocumento(documentoId, atual.mensagem)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _estado.update {
                when (resultado) {
                    is ResultadoEnvio.Enviado -> it.copy(enviando = false, enviado = true)
                    is ResultadoEnvio.Invalido -> it.copy(enviando = false, problemas = resultado.problemas)
                    ResultadoEnvio.FalhaNoEnvio -> it.copy(enviando = false, falha = FalhaRevisao.ENVIO)
                    null -> it.copy(enviando = false, falha = FalhaRevisao.INESPERADA)
                }
            }
        }
    }
}
