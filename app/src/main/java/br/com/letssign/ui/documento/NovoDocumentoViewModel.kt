package br.com.letssign.ui.documento

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.letssign.R
import br.com.letssign.di.EscopoAplicacao
import br.com.letssign.domain.model.ArquivoPdf
import br.com.letssign.domain.repository.ArmazenamentoPdf
import br.com.letssign.domain.repository.DocumentoRepository
import br.com.letssign.domain.repository.ResultadoImportacao
import br.com.letssign.domain.usecase.ObterUsuarioLogadoUseCase
import br.com.letssign.domain.usecase.ResultadoSalvarRascunho
import br.com.letssign.domain.usecase.SalvarRascunhoUseCase
import br.com.letssign.ui.common.mensagem
import br.com.letssign.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NovoDocumentoUiState(
    val carregando: Boolean = false,
    @StringRes val erroCarregar: Int? = null,
    val titulo: String = "",
    val arquivo: ArquivoPdf? = null,
    val importando: Boolean = false,
    @StringRes val erroPdf: Int? = null,
    @StringRes val erroTitulo: Int? = null,
    val erroSemArquivo: Boolean = false,
    val salvando: Boolean = false,
    @StringRes val erroGeral: Int? = null,
)

@HiltViewModel
class NovoDocumentoViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val usuarioLogado: ObterUsuarioLogadoUseCase,
    private val documentos: DocumentoRepository,
    private val armazenamento: ArmazenamentoPdf,
    private val salvarRascunho: SalvarRascunhoUseCase,
    @EscopoAplicacao private val escopoAplicacao: CoroutineScope,
) : ViewModel() {

    // Fica no SavedStateHandle para o rascunho recém-criado não ser duplicado se o processo morrer.
    private var documentoId: Long?
        get() = savedStateHandle.get<Long>(Rotas.ARG_DOCUMENTO_ID)?.takeIf { it > 0 }
        set(valor) {
            savedStateHandle[Rotas.ARG_DOCUMENTO_ID] = valor ?: 0L
        }

    /** PDF importado nesta tela que ainda não pertence a nenhum rascunho. */
    private var importacaoPendente: ArquivoPdf? = null

    private val _estado = MutableStateFlow(NovoDocumentoUiState(carregando = documentoId != null))
    val estado: StateFlow<NovoDocumentoUiState> = _estado.asStateFlow()

    private val _proximaEtapa = Channel<Long>(Channel.BUFFERED)
    val proximaEtapa = _proximaEtapa.receiveAsFlow()

    init {
        carregar()
    }

    fun carregar() {
        val id = documentoId ?: return
        _estado.update { it.copy(carregando = true, erroCarregar = null) }
        viewModelScope.launch {
            val documento = try {
                documentos.buscarDetalheDoAdmin(id, usuarioLogado.admin().id)?.documento
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _estado.update {
                when {
                    documento == null ->
                        it.copy(carregando = false, erroCarregar = R.string.documento_erro_nao_encontrado)
                    !documento.editavel ->
                        it.copy(carregando = false, erroCarregar = R.string.documento_erro_ja_enviado)
                    else -> it.copy(
                        carregando = false,
                        titulo = documento.titulo,
                        arquivo = ArquivoPdf(
                            nomeArquivo = documento.nomeArquivo,
                            caminho = documento.caminhoOriginal,
                            totalPaginas = documento.totalPaginas,
                        ),
                    )
                }
            }
        }
    }

    fun aoMudarTitulo(valor: String) {
        _estado.update { it.copy(titulo = valor, erroTitulo = null, erroGeral = null) }
    }

    fun aoEscolherPdf(uri: String) {
        if (_estado.value.importando) return
        _estado.update { it.copy(importando = true, erroPdf = null, erroSemArquivo = false) }
        viewModelScope.launch {
            val resultado = try {
                armazenamento.importar(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            when (resultado) {
                is ResultadoImportacao.Sucesso -> {
                    descartarImportacaoPendente()
                    importacaoPendente = resultado.arquivo
                    _estado.update { it.copy(importando = false, arquivo = resultado.arquivo) }
                }
                is ResultadoImportacao.Falha ->
                    _estado.update { it.copy(importando = false, erroPdf = resultado.motivo.mensagem) }
                null ->
                    _estado.update { it.copy(importando = false, erroPdf = R.string.pdf_erro_leitura) }
            }
        }
    }

    fun continuar() {
        val atual = _estado.value
        if (atual.salvando || atual.importando) return
        _estado.update { it.copy(salvando = true, erroGeral = null) }
        viewModelScope.launch {
            val resultado = try {
                salvarRascunho(documentoId, atual.titulo, importacaoPendente)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            when (resultado) {
                is ResultadoSalvarRascunho.Sucesso -> {
                    documentoId = resultado.documentoId
                    importacaoPendente = null
                    _estado.update { it.copy(salvando = false) }
                    _proximaEtapa.send(resultado.documentoId)
                }
                is ResultadoSalvarRascunho.Invalido -> _estado.update {
                    it.copy(
                        salvando = false,
                        erroTitulo = R.string.documento_erro_titulo.takeIf { resultado.tituloInvalido },
                        erroSemArquivo = resultado.semArquivo,
                    )
                }
                null -> _estado.update { it.copy(salvando = false, erroGeral = R.string.erro_inesperado) }
            }
        }
    }

    override fun onCleared() {
        descartarImportacaoPendente()
    }

    private fun descartarImportacaoPendente() {
        val pendente = importacaoPendente ?: return
        importacaoPendente = null
        escopoAplicacao.launch { armazenamento.remover(pendente.caminho) }
    }
}
