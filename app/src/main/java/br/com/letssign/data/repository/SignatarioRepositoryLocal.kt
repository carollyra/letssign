package br.com.letssign.data.repository

import android.database.sqlite.SQLiteConstraintException
import br.com.letssign.data.di.DispatcherPadrao
import br.com.letssign.data.local.dao.UsuarioDao
import br.com.letssign.data.local.entity.UsuarioEntity
import br.com.letssign.data.local.paraDominio
import br.com.letssign.data.seguranca.HashSenha
import br.com.letssign.domain.model.Perfil
import br.com.letssign.domain.model.Usuario
import br.com.letssign.domain.repository.DadosSignatario
import br.com.letssign.domain.repository.ResultadoCadastro
import br.com.letssign.domain.repository.SignatarioRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SignatarioRepositoryLocal @Inject constructor(
    private val usuarioDao: UsuarioDao,
    private val hashSenha: HashSenha,
    @DispatcherPadrao private val dispatcher: CoroutineDispatcher,
) : SignatarioRepository {

    override fun observarTodos(): Flow<List<Usuario>> =
        usuarioDao.observarPorPerfil(Perfil.SIGNATARIO).map { lista -> lista.map { it.paraDominio() } }

    override suspend fun buscar(id: Long): Usuario? =
        usuarioDao.buscarPorId(id)?.takeIf { it.perfil == Perfil.SIGNATARIO }?.paraDominio()

    override suspend fun criar(dados: DadosSignatario, senha: String): ResultadoCadastro {
        val protegida = withContext(dispatcher) { hashSenha.proteger(senha) }
        return salvandoComEmailUnico {
            usuarioDao.inserir(
                UsuarioEntity(
                    nome = dados.nome,
                    email = dados.email,
                    telefone = dados.telefone,
                    senhaHash = protegida.hash,
                    salt = protegida.salt,
                    perfil = Perfil.SIGNATARIO,
                ),
            )
        }
    }

    override suspend fun atualizar(
        id: Long,
        dados: DadosSignatario,
        novaSenha: String?,
    ): ResultadoCadastro {
        val atual = usuarioDao.buscarPorId(id)?.takeIf { it.perfil == Perfil.SIGNATARIO }
            ?: return ResultadoCadastro.NaoEncontrado
        val protegida = novaSenha?.let { withContext(dispatcher) { hashSenha.proteger(it) } }
        return salvandoComEmailUnico {
            usuarioDao.atualizar(
                atual.copy(
                    nome = dados.nome,
                    email = dados.email,
                    telefone = dados.telefone,
                    senhaHash = protegida?.hash ?: atual.senhaHash,
                    salt = protegida?.salt ?: atual.salt,
                ),
            )
            id
        }
    }

    // A unicidade é garantida pelo índice do banco; aqui a violação só vira um resultado de domínio.
    private suspend fun salvandoComEmailUnico(salvar: suspend () -> Long): ResultadoCadastro =
        try {
            ResultadoCadastro.Salvo(salvar())
        } catch (e: SQLiteConstraintException) {
            ResultadoCadastro.EmailJaCadastrado
        }
}
