package br.com.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.letssign.domain.model.StatusDocumento

@Entity(
    tableName = "documentos",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["adminId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = DocumentoEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentoOrigemId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("adminId"), Index("documentoOrigemId")],
)
data class DocumentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val nomeArquivo: String,
    val caminhoOriginal: String,
    val caminhoAtual: String,
    val totalPaginas: Int,
    val status: StatusDocumento,
    val versao: Int,
    val documentoOrigemId: Long?,
    val adminId: Long,
    val mensagemConvite: String,
    val criadoEm: Long,
    val enviadoEm: Long?,
    val concluidoEm: Long?,
)
