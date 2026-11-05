package br.com.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.letssign.data.local.entity.DocumentoEntity
import br.com.letssign.data.local.entity.DocumentoSignatarioEntity
import kotlinx.coroutines.flow.Flow

data class DocumentoComVinculo(
    @Embedded val documento: DocumentoEntity,
    @Embedded(prefix = "v_") val vinculo: DocumentoSignatarioEntity,
)

/**
 * Toda leitura recebe o dono da consulta (adminId ou signatarioId), para que o isolamento
 * entre usuários valha no banco e não dependa de filtro na interface.
 */
@Dao
interface DocumentoDao {

    @Insert
    suspend fun inserir(documento: DocumentoEntity): Long

    @Update
    suspend fun atualizar(documento: DocumentoEntity)

    @Query("SELECT * FROM documentos WHERE id = :id AND adminId = :adminId")
    suspend fun buscarDoAdmin(id: Long, adminId: Long): DocumentoEntity?

    @Query("SELECT * FROM documentos WHERE id = :id AND adminId = :adminId")
    fun observarDoAdmin(id: Long, adminId: Long): Flow<DocumentoEntity?>

    @Query("SELECT * FROM documentos WHERE adminId = :adminId ORDER BY criadoEm DESC, id DESC")
    fun observarTodosDoAdmin(adminId: Long): Flow<List<DocumentoEntity>>

    @Query(
        """
        SELECT d.*, $COLUNAS_VINCULO
        FROM documentos d
        INNER JOIN documento_signatarios v ON v.documentoId = d.id
        WHERE v.signatarioId = :signatarioId AND d.status != 'RASCUNHO'
        ORDER BY d.enviadoEm DESC, d.id DESC
        """,
    )
    fun observarEnviadosAoSignatario(signatarioId: Long): Flow<List<DocumentoComVinculo>>

    @Query(
        """
        SELECT d.*, $COLUNAS_VINCULO
        FROM documentos d
        INNER JOIN documento_signatarios v ON v.documentoId = d.id
        WHERE d.id = :id AND v.signatarioId = :signatarioId AND d.status != 'RASCUNHO'
        """,
    )
    suspend fun buscarEnviadoAoSignatario(id: Long, signatarioId: Long): DocumentoComVinculo?

    @Query("DELETE FROM documentos WHERE id = :id AND adminId = :adminId AND status = 'RASCUNHO'")
    suspend fun excluirRascunho(id: Long, adminId: Long): Int

    private companion object {
        const val COLUNAS_VINCULO =
            "v.id AS v_id, v.documentoId AS v_documentoId, v.signatarioId AS v_signatarioId, " +
                "v.papel AS v_papel, v.obrigatorio AS v_obrigatorio, v.status AS v_status, " +
                "v.assinadoEm AS v_assinadoEm"
    }
}
