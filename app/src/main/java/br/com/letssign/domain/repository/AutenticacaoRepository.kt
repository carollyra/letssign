package br.com.letssign.domain.repository

import br.com.letssign.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface AutenticacaoRepository {

    /** Emite `null` enquanto não houver sessão aberta. */
    val usuarioLogado: Flow<Usuario?>

    /** Devolve o usuário quando as credenciais conferem, ou `null` caso contrário. */
    suspend fun entrar(email: String, senha: String): Usuario?

    suspend fun sair()
}
