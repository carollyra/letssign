package br.com.letssign.domain.repository

import br.com.letssign.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

data class DadosSignatario(
    val nome: String,
    val email: String,
    val telefone: String,
)

sealed interface ResultadoCadastro {
    data class Salvo(val id: Long) : ResultadoCadastro
    data object EmailJaCadastrado : ResultadoCadastro
    data object NaoEncontrado : ResultadoCadastro
}

interface SignatarioRepository {

    fun observarTodos(): Flow<List<Usuario>>

    suspend fun buscar(id: Long): Usuario?

    suspend fun criar(dados: DadosSignatario, senha: String): ResultadoCadastro

    /** `novaSenha` nula mantém a senha atual. */
    suspend fun atualizar(id: Long, dados: DadosSignatario, novaSenha: String?): ResultadoCadastro
}
