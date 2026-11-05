package br.com.letssign.domain.usecase

import br.com.letssign.domain.model.Perfil
import br.com.letssign.domain.model.SemPermissaoException
import br.com.letssign.domain.model.SessaoEncerradaException
import br.com.letssign.domain.model.Usuario
import br.com.letssign.domain.repository.AutenticacaoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class ObterUsuarioLogadoUseCase @Inject constructor(
    private val autenticacao: AutenticacaoRepository,
) {
    suspend operator fun invoke(): Usuario =
        autenticacao.usuarioLogado.first() ?: throw SessaoEncerradaException()

    suspend fun admin(): Usuario = comPerfil(Perfil.ADMIN)

    suspend fun signatario(): Usuario = comPerfil(Perfil.SIGNATARIO)

    private suspend fun comPerfil(perfil: Perfil): Usuario =
        invoke().also { if (it.perfil != perfil) throw SemPermissaoException() }
}
