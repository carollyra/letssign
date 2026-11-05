package br.com.letssign.apoio

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.letssign.data.local.LetsSignDatabase
import br.com.letssign.data.local.entity.UsuarioEntity
import br.com.letssign.data.repository.AdminPadrao
import br.com.letssign.data.repository.AutenticacaoRepositoryLocal
import br.com.letssign.data.repository.DocumentoRepositoryLocal
import br.com.letssign.data.repository.SignatarioRepositoryLocal
import br.com.letssign.data.seguranca.HashSenha
import br.com.letssign.data.sessao.ArmazenamentoSessao
import br.com.letssign.domain.Relogio
import br.com.letssign.domain.model.ArquivoPdf
import br.com.letssign.domain.model.Perfil
import br.com.letssign.domain.model.Usuario
import br.com.letssign.domain.repository.ArmazenamentoPdf
import br.com.letssign.domain.repository.ResultadoImportacao
import br.com.letssign.domain.usecase.ObterUsuarioLogadoUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** Banco em memória com os repositórios locais reais, para testar as regras de ponta a ponta. */
class AmbienteDeTeste {

    val banco: LetsSignDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        LetsSignDatabase::class.java,
    ).allowMainThreadQueries().build()

    val sessao = SessaoEmMemoria()
    val relogio = RelogioFalso()
    val armazenamento = ArmazenamentoFalso()

    private val dispatcher = UnconfinedTestDispatcher()
    private val hash = HashSenha()

    val autenticacao = AutenticacaoRepositoryLocal(
        usuarioDao = banco.usuarioDao(),
        sessao = sessao,
        hashSenha = hash,
        adminPadrao = AdminPadrao(banco.usuarioDao(), hash, dispatcher),
        dispatcher = dispatcher,
    )
    val signatarios = SignatarioRepositoryLocal(banco.usuarioDao(), hash, dispatcher)
    val documentos = DocumentoRepositoryLocal(banco)
    val usuarioLogado = ObterUsuarioLogadoUseCase(autenticacao)

    private var sequencia = 0

    /** Os usuários de teste são gravados sem senha real: os testes trocam de sessão direto. */
    suspend fun criarUsuario(nome: String, perfil: Perfil): Usuario {
        val email = "usuario${++sequencia}@teste.com"
        val id = banco.usuarioDao().inserir(
            UsuarioEntity(
                nome = nome,
                email = email,
                telefone = "11999990000",
                senhaHash = "-",
                salt = "-",
                perfil = perfil,
            ),
        )
        return Usuario(id, nome, email, "11999990000", perfil)
    }

    suspend fun criarAdmin(nome: String = "Admin Teste") = criarUsuario(nome, Perfil.ADMIN)

    suspend fun criarSignatario(nome: String) = criarUsuario(nome, Perfil.SIGNATARIO)

    suspend fun entrarComo(usuario: Usuario) = sessao.salvar(usuario.id)

    fun arquivo(nome: String = "contrato.pdf", paginas: Int = 2) =
        ArquivoPdf(nomeArquivo = nome, caminho = "/documentos/$nome", totalPaginas = paginas)

    fun fechar() = banco.close()
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

class RelogioFalso(var instante: Long = 1_000L) : Relogio {
    override fun agora(): Long = instante
}

class ArmazenamentoFalso : ArmazenamentoPdf {
    val removidos = mutableListOf<String>()
    private var copias = 0

    override suspend fun importar(origem: String): ResultadoImportacao =
        ResultadoImportacao.Sucesso(ArquivoPdf("importado.pdf", origem, 1))

    override suspend fun duplicar(caminho: String): String = "$caminho.copia${++copias}"

    override suspend fun remover(caminho: String) {
        removidos += caminho
    }
}
