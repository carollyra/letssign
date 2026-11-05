package br.com.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.domain.model.StatusAssinatura

@Entity(
    tableName = "documento_signatarios",
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
            childColumns = ["signatarioId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["documentoId", "signatarioId"], unique = true),
        Index("signatarioId"),
    ],
)
data class DocumentoSignatarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentoId: Long,
    val signatarioId: Long,
    val papel: PapelSignatario,
    val obrigatorio: Boolean = true,
    val status: StatusAssinatura = StatusAssinatura.PENDENTE,
    val assinadoEm: Long? = null,
)
