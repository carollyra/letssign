package br.com.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import br.com.letssign.data.local.entity.CampoAssinaturaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CampoAssinaturaDao {

    @Insert
    suspend fun inserir(campos: List<CampoAssinaturaEntity>)

    @Query(
        """
        SELECT c.* FROM campos_assinatura c
        INNER JOIN documento_signatarios v ON v.id = c.documentoSignatarioId
        WHERE v.documentoId = :documentoId
        ORDER BY c.pagina, c.id
        """,
    )
    suspend fun listarDoDocumento(documentoId: Long): List<CampoAssinaturaEntity>

    @Query(
        """
        SELECT c.* FROM campos_assinatura c
        INNER JOIN documento_signatarios v ON v.id = c.documentoSignatarioId
        WHERE v.documentoId = :documentoId
        ORDER BY c.pagina, c.id
        """,
    )
    fun observarDoDocumento(documentoId: Long): Flow<List<CampoAssinaturaEntity>>

    @Query("SELECT * FROM campos_assinatura WHERE documentoSignatarioId = :vinculoId ORDER BY pagina, id")
    suspend fun listarDoVinculo(vinculoId: Long): List<CampoAssinaturaEntity>

    @Query(
        """
        DELETE FROM campos_assinatura WHERE documentoSignatarioId IN
            (SELECT id FROM documento_signatarios WHERE documentoId = :documentoId)
        """,
    )
    suspend fun excluirDoDocumento(documentoId: Long)
}
