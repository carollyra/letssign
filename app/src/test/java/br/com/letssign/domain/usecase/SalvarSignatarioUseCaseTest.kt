package br.com.letssign.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.letssign.data.local.LetsSignDatabase
import br.com.letssign.data.repository.AdminPadrao
import br.com.letssign.data.repository.AutenticacaoRepositoryLocal
import br.com.letssign.data.repository.SessaoEmMemoria
import br.com.letssign.data.repository.SignatarioRepositoryLocal
import br.com.letssign.data.seguranca.HashSenha
import br.com.letssign.domain.model.Perfil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SalvarSignatarioUseCaseTest {

    private lateinit var banco: LetsSignDatabase
    private lateinit var autenticacao: AutenticacaoRepositoryLocal
    private lateinit var signatarios: SignatarioRepositoryLocal
    private lateinit var salvar: SalvarSignatarioUseCase

    @Before
    fun preparar() {
        banco = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LetsSignDatabase::class.java,
        ).allowMainThreadQueries().build()
        val dispatcher = UnconfinedTestDispatcher()
        val hash = HashSenha()
        autenticacao = AutenticacaoRepositoryLocal(
            usuarioDao = banco.usuarioDao(),
            sessao = SessaoEmMemoria(),
            hashSenha = hash,
            adminPadrao = AdminPadrao(banco.usuarioDao(), hash, dispatcher),
            dispatcher = dispatcher,
        )
        signatarios = SignatarioRepositoryLocal(banco.usuarioDao(), hash, dispatcher)
        salvar = SalvarSignatarioUseCase(autenticacao, signatarios)
    }

    @After
    fun fechar() = banco.close()

    private suspend fun entrarComoAdmin() {
        autenticacao.entrar(AdminPadrao.EMAIL, AdminPadrao.SENHA)
    }

    @Test
    fun `cria signatario que consegue entrar com a senha cadastrada`() = runTest {
        entrarComoAdmin()

        val resultado = salvar(null, "Ana  Souza", " Ana@Teste.com ", "(11) 99999-0000", "abc123")

        assertTrue(resultado is ResultadoSalvarSignatario.Sucesso)
        autenticacao.sair()
        val logado = autenticacao.entrar("ana@teste.com", "abc123")
        assertNotNull(logado)
        assertEquals(Perfil.SIGNATARIO, logado!!.perfil)
        assertEquals("Ana Souza", logado.nome)
        assertEquals("11999990000", logado.telefone)
    }

    @Test
    fun `aponta todos os campos invalidos de uma vez`() = runTest {
        entrarComoAdmin()

        val resultado = salvar(null, "Ana", "ana@", "9999", "123")

        assertEquals(
            ResultadoSalvarSignatario.Invalido(
                setOf(
                    ErroSignatario.NOME,
                    ErroSignatario.EMAIL,
                    ErroSignatario.TELEFONE,
                    ErroSignatario.SENHA,
                ),
            ),
            resultado,
        )
        assertTrue(signatarios.observarTodos().first().isEmpty())
    }

    @Test
    fun `email repetido e recusado, mesmo com outra caixa`() = runTest {
        entrarComoAdmin()
        salvar(null, "Ana Souza", "ana@teste.com", "11999990000", "abc123")

        val resultado = salvar(null, "Outra Ana", "ANA@teste.com", "11988880000", "abc123")

        assertEquals(
            ResultadoSalvarSignatario.Invalido(setOf(ErroSignatario.EMAIL_JA_CADASTRADO)),
            resultado,
        )
        assertEquals(1, signatarios.observarTodos().first().size)
    }

    @Test
    fun `email do admin tambem conta como ja cadastrado`() = runTest {
        entrarComoAdmin()

        val resultado = salvar(null, "Ana Souza", AdminPadrao.EMAIL, "11999990000", "abc123")

        assertEquals(
            ResultadoSalvarSignatario.Invalido(setOf(ErroSignatario.EMAIL_JA_CADASTRADO)),
            resultado,
        )
    }

    @Test
    fun `edicao sem senha mantem a senha atual`() = runTest {
        entrarComoAdmin()
        val id = (salvar(null, "Ana Souza", "ana@teste.com", "11999990000", "abc123")
            as ResultadoSalvarSignatario.Sucesso).id

        val resultado = salvar(id, "Ana Souza Lima", "ana.lima@teste.com", "1133334444", "")

        assertEquals(ResultadoSalvarSignatario.Sucesso(id), resultado)
        autenticacao.sair()
        assertNull(autenticacao.entrar("ana@teste.com", "abc123"))
        assertEquals("Ana Souza Lima", autenticacao.entrar("ana.lima@teste.com", "abc123")?.nome)
    }

    @Test
    fun `edicao com senha nova troca a senha`() = runTest {
        entrarComoAdmin()
        val id = (salvar(null, "Ana Souza", "ana@teste.com", "11999990000", "abc123")
            as ResultadoSalvarSignatario.Sucesso).id

        salvar(id, "Ana Souza", "ana@teste.com", "11999990000", "nova456")

        autenticacao.sair()
        assertNull(autenticacao.entrar("ana@teste.com", "abc123"))
        assertNotNull(autenticacao.entrar("ana@teste.com", "nova456"))
    }

    @Test
    fun `signatario nao pode cadastrar outros signatarios`() = runTest {
        entrarComoAdmin()
        salvar(null, "Ana Souza", "ana@teste.com", "11999990000", "abc123")
        autenticacao.sair()
        autenticacao.entrar("ana@teste.com", "abc123")

        val resultado = salvar(null, "Bia Lima", "bia@teste.com", "11988880000", "abc123")

        assertEquals(ResultadoSalvarSignatario.SemPermissao, resultado)
    }

    @Test
    fun `sem sessao nao cadastra`() = runTest {
        val resultado = salvar(null, "Bia Lima", "bia@teste.com", "11988880000", "abc123")

        assertEquals(ResultadoSalvarSignatario.SemPermissao, resultado)
    }
}
