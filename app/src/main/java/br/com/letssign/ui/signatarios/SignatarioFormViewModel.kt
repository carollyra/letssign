package br.com.letssign.ui.signatarios

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.letssign.R
import br.com.letssign.domain.repository.SignatarioRepository
import br.com.letssign.domain.usecase.ErroSignatario
import br.com.letssign.domain.usecase.ResultadoSalvarSignatario
import br.com.letssign.domain.usecase.SalvarSignatarioUseCase
import br.com.letssign.ui.common.formatarTelefone
import br.com.letssign.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignatarioFormUiState(
    val edicao: Boolean = false,
    val carregando: Boolean = false,
    val erroCarregar: Boolean = false,
    val nome: String = "",
    val email: String = "",
    val telefone: String = "",
    val senha: String = "",
    @StringRes val erroNome: Int? = null,
    @StringRes val erroEmail: Int? = null,
    @StringRes val erroTelefone: Int? = null,
    @StringRes val erroSenha: Int? = null,
    @StringRes val erroGeral: Int? = null,
    val salvando: Boolean = false,
    val salvo: Boolean = false,
)

@HiltViewModel
class SignatarioFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val signatarios: SignatarioRepository,
    private val salvarSignatario: SalvarSignatarioUseCase,
) : ViewModel() {

    private val signatarioId: Long? =
        savedStateHandle.get<Long>(Rotas.ARG_SIGNATARIO_ID)?.takeIf { it > 0 }

    private val _estado = MutableStateFlow(
        SignatarioFormUiState(edicao = signatarioId != null, carregando = signatarioId != null),
    )
    val estado: StateFlow<SignatarioFormUiState> = _estado.asStateFlow()

    init {
        carregar()
    }

    fun carregar() {
        val id = signatarioId ?: return
        _estado.update { it.copy(carregando = true, erroCarregar = false) }
        viewModelScope.launch {
            val signatario = try {
                signatarios.buscar(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _estado.update {
                if (signatario == null) {
                    it.copy(carregando = false, erroCarregar = true)
                } else {
                    it.copy(
                        carregando = false,
                        nome = signatario.nome,
                        email = signatario.email,
                        telefone = formatarTelefone(signatario.telefone),
                    )
                }
            }
        }
    }

    fun aoMudarNome(valor: String) = _estado.update { it.copy(nome = valor, erroNome = null) }

    fun aoMudarEmail(valor: String) = _estado.update { it.copy(email = valor, erroEmail = null) }

    fun aoMudarTelefone(valor: String) =
        _estado.update { it.copy(telefone = valor, erroTelefone = null) }

    fun aoMudarSenha(valor: String) = _estado.update { it.copy(senha = valor, erroSenha = null) }

    fun salvar() {
        val atual = _estado.value
        if (atual.salvando) return
        _estado.update { it.copy(salvando = true, erroGeral = null) }
        viewModelScope.launch {
            val resultado = try {
                salvarSignatario(signatarioId, atual.nome, atual.email, atual.telefone, atual.senha)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _estado.update { estado ->
                when (resultado) {
                    is ResultadoSalvarSignatario.Sucesso -> estado.copy(salvando = false, salvo = true)
                    is ResultadoSalvarSignatario.Invalido -> estado.comErros(resultado.erros)
                    ResultadoSalvarSignatario.NaoEncontrado ->
                        estado.copy(salvando = false, erroGeral = R.string.signatario_erro_nao_encontrado)
                    ResultadoSalvarSignatario.SemPermissao ->
                        estado.copy(salvando = false, erroGeral = R.string.erro_sem_permissao)
                    null -> estado.copy(salvando = false, erroGeral = R.string.erro_inesperado)
                }
            }
        }
    }

    private fun SignatarioFormUiState.comErros(erros: Set<ErroSignatario>) = copy(
        salvando = false,
        erroNome = R.string.signatario_erro_nome.takeIf { ErroSignatario.NOME in erros },
        erroEmail = when {
            ErroSignatario.EMAIL in erros -> R.string.login_erro_email
            ErroSignatario.EMAIL_JA_CADASTRADO in erros -> R.string.signatario_erro_email_duplicado
            else -> null
        },
        erroTelefone = R.string.signatario_erro_telefone.takeIf { ErroSignatario.TELEFONE in erros },
        erroSenha = R.string.signatario_erro_senha.takeIf { ErroSignatario.SENHA in erros },
    )
}
