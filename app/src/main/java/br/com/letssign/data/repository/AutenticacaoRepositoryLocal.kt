package br.com.letssign.data.repository

import br.com.letssign.data.di.DispatcherPadrao
import br.com.letssign.data.local.dao.UsuarioDao
import br.com.letssign.data.local.paraDominio
import br.com.letssign.data.seguranca.HashSenha
import br.com.letssign.data.seguranca.SenhaProtegida
import br.com.letssign.data.sessao.ArmazenamentoSessao
import br.com.letssign.domain.model.Usuario
import br.com.letssign.domain.repository.AutenticacaoRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AutenticacaoRepositoryLocal @Inject constructor(
    private val usuarioDao: UsuarioDao,
    private val sessao: ArmazenamentoSessao,
    private val hashSenha: HashSenha,
    private val adminPadrao: AdminPadrao,
    @DispatcherPadrao private val dispatcher: CoroutineDispatcher,
) : AutenticacaoRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val usuarioLogado: Flow<Usuario?> = sessao.usuarioId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else usuarioDao.observarPorId(id).map { it?.paraDominio() }
        }
        .distinctUntilChanged()

    override suspend fun entrar(email: String, senha: String): Usuario? {
        adminPadrao.garantir()
        val usuario = usuarioDao.buscarPorEmail(email) ?: return null
        val confere = withContext(dispatcher) {
            hashSenha.confere(senha, SenhaProtegida(usuario.senhaHash, usuario.salt))
        }
        if (!confere) return null
        sessao.salvar(usuario.id)
        return usuario.paraDominio()
    }

    override suspend fun sair() = sessao.limpar()
}
