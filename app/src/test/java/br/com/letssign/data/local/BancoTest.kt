package br.com.letssign.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.letssign.data.local.entity.CampoAssinaturaEntity
import br.com.letssign.data.local.entity.DocumentoEntity
import br.com.letssign.data.local.entity.DocumentoSignatarioEntity
import br.com.letssign.data.local.entity.UsuarioEntity
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.domain.model.Perfil
import br.com.letssign.domain.model.StatusDocumento
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BancoTest {

    private lateinit var banco: LetsSignDatabase

    @Before
    fun abrir() {
        banco = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LetsSignDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun fechar() = banco.close()

    @Test
    fun `email de usuario e unico`() = runTest {
        banco.usuarioDao().inserir(usuario("ana@teste.com"))

        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking { banco.usuarioDao().inserir(usuario("ana@teste.com")) }
        }
    }

    @Test
    fun `signatario nao pode ser vinculado duas vezes ao mesmo documento`() = runTest {
        val adminId = banco.usuarioDao().inserir(usuario("admin@teste.com", Perfil.ADMIN))
        val signatarioId = banco.usuarioDao().inserir(usuario("ana@teste.com"))
        val documentoId = banco.documentoDao().inserir(documento(adminId))
        val vinculo = DocumentoSignatarioEntity(
            documentoId = documentoId,
            signatarioId = signatarioId,
            papel = PapelSignatario.CONTRATANTE,
        )
        banco.documentoSignatarioDao().inserir(vinculo)

        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking { banco.documentoSignatarioDao().inserir(vinculo) }
        }
    }

    @Test
    fun `vinculo nasce obrigatorio e pendente`() = runTest {
        val adminId = banco.usuarioDao().inserir(usuario("admin@teste.com", Perfil.ADMIN))
        val signatarioId = banco.usuarioDao().inserir(usuario("ana@teste.com"))
        val documentoId = banco.documentoDao().inserir(documento(adminId))
        banco.documentoSignatarioDao().inserir(
            DocumentoSignatarioEntity(
                documentoId = documentoId,
                signatarioId = signatarioId,
                papel = PapelSignatario.TESTEMUNHA,
            ),
        )

        val salvo = banco.documentoSignatarioDao().listarDoDocumento(documentoId).single()

        assertTrue(salvo.obrigatorio)
        assertEquals(br.com.letssign.domain.model.StatusAssinatura.PENDENTE, salvo.status)
    }

    @Test
    fun `remover vinculo remove os campos dele`() = runTest {
        val adminId = banco.usuarioDao().inserir(usuario("admin@teste.com", Perfil.ADMIN))
        val signatarioId = banco.usuarioDao().inserir(usuario("ana@teste.com"))
        val documentoId = banco.documentoDao().inserir(documento(adminId))
        val vinculoId = banco.documentoSignatarioDao().inserir(
            DocumentoSignatarioEntity(
                documentoId = documentoId,
                signatarioId = signatarioId,
                papel = PapelSignatario.CONTRATADO,
            ),
        )
        banco.campoAssinaturaDao().inserir(
            listOf(CampoAssinaturaEntity(0, vinculoId, 0, 0.1f, 0.1f, 0.3f, 0.1f)),
        )

        banco.documentoSignatarioDao().excluir(listOf(vinculoId))

        assertTrue(banco.campoAssinaturaDao().listarDoDocumento(documentoId).isEmpty())
    }

    private fun usuario(email: String, perfil: Perfil = Perfil.SIGNATARIO) = UsuarioEntity(
        nome = "Pessoa",
        email = email,
        telefone = "11999990000",
        senhaHash = "hash",
        salt = "salt",
        perfil = perfil,
    )

    private fun documento(adminId: Long) = DocumentoEntity(
        titulo = "Contrato",
        nomeArquivo = "contrato.pdf",
        caminhoOriginal = "/tmp/contrato.pdf",
        caminhoAtual = "/tmp/contrato.pdf",
        totalPaginas = 1,
        status = StatusDocumento.RASCUNHO,
        versao = 1,
        documentoOrigemId = null,
        adminId = adminId,
        mensagemConvite = "",
        criadoEm = 1L,
        enviadoEm = null,
        concluidoEm = null,
    )
}
