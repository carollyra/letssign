package br.com.letssign.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.letssign.data.local.LetsSignDatabase
import br.com.letssign.data.seguranca.HashSenha
import br.com.letssign.data.sessao.ArmazenamentoSessao
import br.com.letssign.domain.model.Perfil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutenticacaoRepositoryLocalTest {

    private lateinit var banco: LetsSignDatabase
    private lateinit var sessao: SessaoEmMemoria
    private lateinit var repositorio: AutenticacaoRepositoryLocal

    @Before
    fun preparar() {
        banco = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LetsSignDatabase::class.java,
        ).allowMainThreadQueries().build()
        sessao = SessaoEmMemoria()
        val dispatcher = UnconfinedTestDispatcher()
        val hash = HashSenha()
        repositorio = AutenticacaoRepositoryLocal(
            usuarioDao = banco.usuarioDao(),
            sessao = sessao,
            hashSenha = hash,
            adminPadrao = AdminPadrao(banco.usuarioDao(), hash, dispatcher),
            dispatcher = dispatcher,
        )
    }

    @After
    fun fechar() = banco.close()

    @Test
    fun `admin padrao entra com as credenciais documentadas`() = runTest {
        val usuario = repositorio.entrar(AdminPadrao.EMAIL, AdminPadrao.SENHA)

        assertNotNull(usuario)
        assertEquals(Perfil.ADMIN, usuario!!.perfil)
        assertEquals(usuario, repositorio.usuarioLogado.first())
    }

    @Test
    fun `admin padrao e criado uma unica vez`() = runTest {
        repositorio.entrar(AdminPadrao.EMAIL, AdminPadrao.SENHA)
        repositorio.sair()
        repositorio.entrar(AdminPadrao.EMAIL, AdminPadrao.SENHA)

        assertEquals(1, banco.usuarioDao().contarPorPerfil(Perfil.ADMIN))
    }

    @Test
    fun `senha fica gravada apenas como hash`() = runTest {
        repositorio.entrar(AdminPadrao.EMAIL, AdminPadrao.SENHA)

        val salvo = banco.usuarioDao().buscarPorEmail(AdminPadrao.EMAIL)!!

        assertNotEquals(AdminPadrao.SENHA, salvo.senhaHash)
        assertNotEquals("", salvo.salt)
    }

    @Test
    fun `senha errada nao abre sessao`() = runTest {
        val usuario = repositorio.entrar(AdminPadrao.EMAIL, "outra-senha")

        assertNull(usuario)
        assertNull(repositorio.usuarioLogado.first())
    }

    @Test
    fun `email desconhecido nao abre sessao`() = runTest {
        assertNull(repositorio.entrar("ninguem@letssign.com.br", AdminPadrao.SENHA))
    }

    @Test
    fun `sair encerra a sessao`() = runTest {
        repositorio.entrar(AdminPadrao.EMAIL, AdminPadrao.SENHA)

        repositorio.sair()

        assertNull(repositorio.usuarioLogado.first())
    }
}

class SessaoEmMemoria : ArmazenamentoSessao {
    private val id = MutableStateFlow<Long?>(null)
    override val usuarioId: Flow<Long?> = id

    override suspend fun salvar(usuarioId: Long) {
        id.value = usuarioId
    }

    override suspend fun limpar() {
        id.value = null
    }
}
