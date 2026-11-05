package br.com.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import br.com.letssign.data.local.entity.EventoDocumentoEntity
import kotlinx.coroutines.flow.Flow

data class EventoComUsuario(
    @Embedded val evento: EventoDocumentoEntity,
    val nomeUsuario: String,
)

@Dao
interface EventoDocumentoDao {

    @Insert
    suspend fun inserir(evento: EventoDocumentoEntity): Long

    @Query(
        """
        SELECT e.*, u.nome AS nomeUsuario
        FROM eventos_documento e
        INNER JOIN documentos d ON d.id = e.documentoId
        INNER JOIN usuarios u ON u.id = e.usuarioId
        WHERE e.documentoId = :documentoId AND d.adminId = :adminId
        ORDER BY e.dataHora, e.id
        """,
    )
    fun observarDoDocumento(documentoId: Long, adminId: Long): Flow<List<EventoComUsuario>>
}
