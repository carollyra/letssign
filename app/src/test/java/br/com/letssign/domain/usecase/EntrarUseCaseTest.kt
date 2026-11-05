package br.com.letssign.domain.usecase

import br.com.letssign.domain.model.Perfil
import br.com.letssign.domain.model.Usuario
import br.com.letssign.domain.repository.AutenticacaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntrarUseCaseTest {

    private val repositorio = AutenticacaoFalsa()
    private val entrar = EntrarUseCase(repositorio)

    @Test
    fun `email malformado e senha vazia nem chegam ao repositorio`() = runTest {
        val resultado = entrar("sem-arroba", "")

        assertEquals(ResultadoLogin.CamposInvalidos(emailInvalido = true, senhaVazia = true), resultado)
        assertTrue(repositorio.tentativas.isEmpty())
    }

    @Test
    fun `email e normalizado antes da consulta`() = runTest {
        entrar("  Ana@Teste.COM ", "123456")

        assertEquals(listOf("ana@teste.com"), repositorio.tentativas)
    }

    @Test
    fun `credenciais recusadas viram erro de credenciais`() = runTest {
        assertEquals(ResultadoLogin.CredenciaisInvalidas, entrar("ana@teste.com", "errada"))
    }

    @Test
    fun `credenciais aceitas devolvem o usuario`() = runTest {
        val resultado = entrar("ana@teste.com", "123456")

        assertEquals(ResultadoLogin.Sucesso(repositorio.usuario), resultado)
    }

    private class AutenticacaoFalsa : AutenticacaoRepository {
        val usuario = Usuario(1, "Ana Souza", "ana@teste.com", "11999990000", Perfil.SIGNATARIO)
        val tentativas = mutableListOf<String>()
        override val usuarioLogado: Flow<Usuario?> = MutableStateFlow(null)

        override suspend fun entrar(email: String, senha: String): Usuario? {
            tentativas += email
            return usuario.takeIf { email == it.email && senha == "123456" }
        }

        override suspend fun sair() = Unit
    }
}
