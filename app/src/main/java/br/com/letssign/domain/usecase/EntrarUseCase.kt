package br.com.letssign.domain.usecase

import br.com.letssign.domain.model.Usuario
import br.com.letssign.domain.regra.ValidacaoCampos
import br.com.letssign.domain.repository.AutenticacaoRepository
import javax.inject.Inject

sealed interface ResultadoLogin {
    data class Sucesso(val usuario: Usuario) : ResultadoLogin
    data class CamposInvalidos(val emailInvalido: Boolean, val senhaVazia: Boolean) : ResultadoLogin
    data object CredenciaisInvalidas : ResultadoLogin
}

class EntrarUseCase @Inject constructor(
    private val autenticacao: AutenticacaoRepository,
) {
    suspend operator fun invoke(email: String, senha: String): ResultadoLogin {
        val emailInvalido = !ValidacaoCampos.emailValido(email)
        val senhaVazia = senha.isEmpty()
        if (emailInvalido || senhaVazia) {
            return ResultadoLogin.CamposInvalidos(emailInvalido, senhaVazia)
        }
        val usuario = autenticacao.entrar(ValidacaoCampos.normalizarEmail(email), senha)
        return if (usuario != null) ResultadoLogin.Sucesso(usuario) else ResultadoLogin.CredenciaisInvalidas
    }
}
