package br.com.letssign.data.repository

import br.com.letssign.data.di.DispatcherPadrao
import br.com.letssign.data.local.dao.UsuarioDao
import br.com.letssign.data.local.entity.UsuarioEntity
import br.com.letssign.data.seguranca.HashSenha
import br.com.letssign.domain.model.Perfil
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Cria o Admin inicial enquanto o app não tem backend para provisionar contas. */
@Singleton
class AdminPadrao @Inject constructor(
    private val usuarioDao: UsuarioDao,
    private val hashSenha: HashSenha,
    @DispatcherPadrao private val dispatcher: CoroutineDispatcher,
) {
    private val trava = Mutex()

    suspend fun garantir() = trava.withLock {
        if (usuarioDao.contarPorPerfil(Perfil.ADMIN) > 0) return@withLock
        val senha = withContext(dispatcher) { hashSenha.proteger(SENHA) }
        usuarioDao.inserir(
            UsuarioEntity(
                nome = NOME,
                email = EMAIL,
                telefone = TELEFONE,
                senhaHash = senha.hash,
                salt = senha.salt,
                perfil = Perfil.ADMIN,
            ),
        )
    }

    companion object {
        const val NOME = "Administrador LetsSign"
        const val EMAIL = "admin@letssign.com.br"
        const val SENHA = "Admin@123"
        const val TELEFONE = "11900000000"
    }
}
