package br.com.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "campos_assinatura",
    foreignKeys = [
        ForeignKey(
            entity = DocumentoSignatarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentoSignatarioId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("documentoSignatarioId")],
)
data class CampoAssinaturaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentoSignatarioId: Long,
    val pagina: Int,
    val x: Float,
    val y: Float,
    val largura: Float,
    val altura: Float,
)
