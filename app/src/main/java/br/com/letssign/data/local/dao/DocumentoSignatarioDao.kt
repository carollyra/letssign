package br.com.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.letssign.data.local.entity.DocumentoSignatarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentoSignatarioDao {

    @Insert
    suspend fun inserir(vinculo: DocumentoSignatarioEntity): Long

    @Update
    suspend fun atualizar(vinculo: DocumentoSignatarioEntity)

    @Query("SELECT * FROM documento_signatarios WHERE documentoId = :documentoId ORDER BY id")
    suspend fun listarDoDocumento(documentoId: Long): List<DocumentoSignatarioEntity>

    @Query("SELECT * FROM documento_signatarios WHERE documentoId = :documentoId ORDER BY id")
    fun observarDoDocumento(documentoId: Long): Flow<List<DocumentoSignatarioEntity>>

    @Query("DELETE FROM documento_signatarios WHERE id IN (:ids)")
    suspend fun excluir(ids: List<Long>)
}
