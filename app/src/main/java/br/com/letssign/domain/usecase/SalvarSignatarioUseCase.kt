package br.com.letssign.domain.usecase

import br.com.letssign.domain.model.Perfil
import br.com.letssign.domain.regra.ValidacaoCampos
import br.com.letssign.domain.repository.AutenticacaoRepository
import br.com.letssign.domain.repository.DadosSignatario
import br.com.letssign.domain.repository.ResultadoCadastro
import br.com.letssign.domain.repository.SignatarioRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

enum class ErroSignatario { NOME, EMAIL, EMAIL_JA_CADASTRADO, TELEFONE, SENHA }

sealed interface ResultadoSalvarSignatario {
    data class Sucesso(val id: Long) : ResultadoSalvarSignatario
    data class Invalido(val erros: Set<ErroSignatario>) : ResultadoSalvarSignatario
    data object NaoEncontrado : ResultadoSalvarSignatario
    data object SemPermissao : ResultadoSalvarSignatario
}

class SalvarSignatarioUseCase @Inject constructor(
    private val autenticacao: AutenticacaoRepository,
    private val signatarios: SignatarioRepository,
) {
    /**
     * Com [id] nulo cria um signatário e a senha é obrigatória. Na edição, senha em branco
     * mantém a atual.
     */
    suspend operator fun invoke(
        id: Long?,
        nome: String,
        email: String,
        telefone: String,
        senha: String,
    ): ResultadoSalvarSignatario {
        if (autenticacao.usuarioLogado.first()?.perfil != Perfil.ADMIN) {
            return ResultadoSalvarSignatario.SemPermissao
        }

        val erros = buildSet {
            if (!ValidacaoCampos.nomeCompletoValido(nome)) add(ErroSignatario.NOME)
            if (!ValidacaoCampos.emailValido(email)) add(ErroSignatario.EMAIL)
            if (!ValidacaoCampos.telefoneValido(telefone)) add(ErroSignatario.TELEFONE)
            val senhaObrigatoria = id == null || senha.isNotEmpty()
            if (senhaObrigatoria && !ValidacaoCampos.senhaValida(senha)) add(ErroSignatario.SENHA)
        }
        if (erros.isNotEmpty()) return ResultadoSalvarSignatario.Invalido(erros)

        val dados = DadosSignatario(
            nome = nome.trim().replace(Regex("\\s+"), " "),
            email = ValidacaoCampos.normalizarEmail(email),
            telefone = ValidacaoCampos.apenasDigitos(telefone),
        )
        val resultado = if (id == null) {
            signatarios.criar(dados, senha)
        } else {
            signatarios.atualizar(id, dados, senha.ifEmpty { null })
        }
        return when (resultado) {
            is ResultadoCadastro.Salvo -> ResultadoSalvarSignatario.Sucesso(resultado.id)
            ResultadoCadastro.EmailJaCadastrado ->
                ResultadoSalvarSignatario.Invalido(setOf(ErroSignatario.EMAIL_JA_CADASTRADO))
            ResultadoCadastro.NaoEncontrado -> ResultadoSalvarSignatario.NaoEncontrado
        }
    }
}
