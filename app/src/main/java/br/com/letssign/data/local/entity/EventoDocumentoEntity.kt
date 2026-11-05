package br.com.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.letssign.domain.model.TipoEvento

@Entity(
    tableName = "eventos_documento",
    foreignKeys = [
        ForeignKey(
            entity = DocumentoEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentoId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("documentoId"), Index("usuarioId")],
)
data class EventoDocumentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentoId: Long,
    val tipo: TipoEvento,
    val usuarioId: Long,
    val dataHora: Long,
)
