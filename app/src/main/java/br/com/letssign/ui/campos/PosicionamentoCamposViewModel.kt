package br.com.letssign.ui.campos

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.letssign.R
import br.com.letssign.di.EscopoAplicacao
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.domain.regra.AreaNormalizada
import br.com.letssign.domain.regra.GeometriaCampo
import br.com.letssign.domain.repository.DocumentoRepository
import br.com.letssign.domain.repository.NovoCampo
import br.com.letssign.domain.usecase.ObterUsuarioLogadoUseCase
import br.com.letssign.domain.usecase.SalvarCamposUseCase
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SignatarioDosCampos(
    val id: Long,
    val nome: String,
    val papel: PapelSignatario,
    val indiceCor: Int,
)

data class CampoEditavel(
    val chave: Long,
    val signatarioId: Long,
    val pagina: Int,
    val area: AreaNormalizada,
)

data class PosicionamentoUiState(
    val carregando: Boolean = true,
    val erroCarregar: Boolean = false,
    val caminhoPdf: String? = null,
    val totalPaginas: Int = 0,
    val signatarios: List<SignatarioDosCampos> = emptyList(),
    val campos: List<CampoEditavel> = emptyList(),
    val paginaAtual: Int = 0,
    val signatarioAtivoId: Long? = null,
    val campoSelecionado: Long? = null,
    val semCampo: List<String> = emptyList(),
    val salvando: Boolean = false,
    @StringRes val erroGeral: Int? = null,
) {
    val camposDaPagina: List<CampoEditavel> get() = campos.filter { it.pagina == paginaAtual }
}

@HiltViewModel
class PosicionamentoCamposViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usuarioLogado: ObterUsuarioLogadoUseCase,
    private val documentos: DocumentoRepository,
    private val salvarCampos: SalvarCamposUseCase,
    @EscopoAplicacao private val escopoAplicacao: CoroutineScope,
) : ViewModel() {

    private val documentoId: Long = checkNotNull(savedStateHandle[Rotas.ARG_DOCUMENTO_ID])

    private val _estado = MutableStateFlow(PosicionamentoUiState())
    val estado: StateFlow<PosicionamentoUiState> = _estado.asStateFlow()

    private val _proximaEtapa = Channel<Long>(Channel.BUFFERED)
    val proximaEtapa = _proximaEtapa.receiveAsFlow()

    private var proximaChave = 1L

    // As gravações automáticas rodam fora do escopo da tela e podem terminar fora de ordem;
    // a numeração garante que uma versão antiga nunca sobrescreva uma mais nova.
    private val travaGravacao = Mutex()
    private var versaoAtual = 0L
    private var versaoGravada = 0L

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
            if (detalhe == null || !detalhe.documento.editavel || detalhe.signatarios.isEmpty()) {
                _estado.update { it.copy(carregando = false, erroCarregar = true) }
                return@launch
            }
            val signatarios = detalhe.signatarios.mapIndexed { indice, signatario ->
                SignatarioDosCampos(
                    id = signatario.usuario.id,
                    nome = signatario.usuario.nome,
                    papel = signatario.vinculo.papel,
                    indiceCor = indice,
                )
            }
            val campos = detalhe.signatarios.flatMap { signatario ->
                signatario.campos.map { campo ->
                    CampoEditavel(proximaChave++, signatario.usuario.id, campo.pagina, campo.area)
                }
            }
            _estado.update {
                it.copy(
                    carregando = false,
                    caminhoPdf = detalhe.documento.caminhoOriginal,
                    totalPaginas = detalhe.documento.totalPaginas,
                    signatarios = signatarios,
                    campos = campos,
                    signatarioAtivoId = it.signatarioAtivoId ?: signatarios.first().id,
                    paginaAtual = it.paginaAtual.coerceIn(0, detalhe.documento.totalPaginas - 1),
                )
            }
        }
    }

    fun selecionarSignatario(id: Long) {
        _estado.update { it.copy(signatarioAtivoId = id) }
    }

    fun irParaPagina(pagina: Int) {
        _estado.update {
            if (pagina in 0 until it.totalPaginas) it.copy(paginaAtual = pagina, campoSelecionado = null) else it
        }
    }

    fun adicionarCampo() {
        val atual = _estado.value
        val signatarioId = atual.signatarioAtivoId ?: return
        // Cada campo novo nasce um pouco abaixo do anterior para não ficar escondido atrás dele.
        val deslocamento = (atual.camposDaPagina.size % 6) * 0.04f
        val area = GeometriaCampo.limitar(
            AreaNormalizada(
                x = (1f - GeometriaCampo.LARGURA_PADRAO) / 2f + deslocamento,
                y = 0.42f + deslocamento,
                largura = GeometriaCampo.LARGURA_PADRAO,
                altura = GeometriaCampo.ALTURA_PADRAO,
            ),
        )
        val campo = CampoEditavel(proximaChave++, signatarioId, atual.paginaAtual, area)
        _estado.update {
            it.copy(campos = it.campos + campo, campoSelecionado = campo.chave, semCampo = emptyList())
        }
        gravarEmSegundoPlano()
    }

    fun selecionarCampo(chave: Long?) {
        _estado.update { estado ->
            val campo = estado.campos.firstOrNull { it.chave == chave }
            estado.copy(
                campoSelecionado = campo?.chave,
                signatarioAtivoId = campo?.signatarioId ?: estado.signatarioAtivoId,
            )
        }
    }

    fun atualizarArea(chave: Long, area: AreaNormalizada) {
        val limitada = GeometriaCampo.limitar(area)
        _estado.update { estado ->
            estado.copy(campos = estado.campos.map { if (it.chave == chave) it.copy(area = limitada) else it })
        }
    }

    fun aoTerminarGesto() = gravarEmSegundoPlano()

    fun excluirCampoSelecionado() {
        val chave = _estado.value.campoSelecionado ?: return
        _estado.update {
            it.copy(campos = it.campos.filterNot { campo -> campo.chave == chave }, campoSelecionado = null)
        }
        gravarEmSegundoPlano()
    }

    fun continuar() {
        val atual = _estado.value
        if (atual.salvando) return
        val comCampo = atual.campos.map { it.signatarioId }.toSet()
        val semCampo = atual.signatarios.filter { it.id !in comCampo }.map { it.nome }
        if (semCampo.isNotEmpty()) {
            _estado.update { it.copy(semCampo = semCampo) }
            return
        }
        _estado.update { it.copy(salvando = true, erroGeral = null) }
        val versao = ++versaoAtual
        viewModelScope.launch {
            val gravou = try {
                gravar(versao, atual.campos)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            _estado.update {
                it.copy(salvando = false, erroGeral = R.string.campos_erro_salvar.takeIf { !gravou })
            }
            if (gravou) _proximaEtapa.send(documentoId)
        }
    }

    private fun gravarEmSegundoPlano() {
        val versao = ++versaoAtual
        val campos = _estado.value.campos
        escopoAplicacao.launch {
            try {
                gravar(versao, campos)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(erroGeral = R.string.campos_erro_salvar) }
            }
        }
    }

    private suspend fun gravar(versao: Long, campos: List<CampoEditavel>) = travaGravacao.withLock {
        if (versao < versaoGravada) return@withLock
        salvarCampos(documentoId, campos.map { NovoCampo(it.signatarioId, it.pagina, it.area) })
        versaoGravada = versao
    }
}
