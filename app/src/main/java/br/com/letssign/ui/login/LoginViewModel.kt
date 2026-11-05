package br.com.letssign.ui.login

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.letssign.R
import br.com.letssign.domain.usecase.EntrarUseCase
import br.com.letssign.domain.usecase.ResultadoLogin
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val senha: String = "",
    @StringRes val erroEmail: Int? = null,
    @StringRes val erroSenha: Int? = null,
    @StringRes val erroGeral: Int? = null,
    val carregando: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val entrar: EntrarUseCase,
) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun aoMudarEmail(valor: String) =
        _estado.update { it.copy(email = valor, erroEmail = null, erroGeral = null) }

    fun aoMudarSenha(valor: String) =
        _estado.update { it.copy(senha = valor, erroSenha = null, erroGeral = null) }

    fun aoEntrar() {
        val atual = _estado.value
        if (atual.carregando) return
        _estado.update { it.copy(carregando = true, erroGeral = null) }
        viewModelScope.launch {
            val resultado = try {
                entrar(atual.email, atual.senha)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _estado.update { estado ->
                when (resultado) {
                    is ResultadoLogin.Sucesso -> estado.copy(carregando = false, senha = "")
                    is ResultadoLogin.CamposInvalidos -> estado.copy(
                        carregando = false,
                        erroEmail = R.string.login_erro_email.takeIf { resultado.emailInvalido },
                        erroSenha = R.string.login_erro_senha_vazia.takeIf { resultado.senhaVazia },
                    )
                    ResultadoLogin.CredenciaisInvalidas ->
                        estado.copy(carregando = false, erroGeral = R.string.login_erro_credenciais)
                    null -> estado.copy(carregando = false, erroGeral = R.string.erro_inesperado)
                }
            }
        }
    }
}
