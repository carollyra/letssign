package br.com.letssign.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.letssign.data.local.dao.CampoAssinaturaDao
import br.com.letssign.data.local.dao.DocumentoDao
import br.com.letssign.data.local.dao.DocumentoSignatarioDao
import br.com.letssign.data.local.dao.EventoDocumentoDao
import br.com.letssign.data.local.dao.UsuarioDao
import br.com.letssign.data.local.entity.CampoAssinaturaEntity
import br.com.letssign.data.local.entity.DocumentoEntity
import br.com.letssign.data.local.entity.DocumentoSignatarioEntity
import br.com.letssign.data.local.entity.EventoDocumentoEntity
import br.com.letssign.data.local.entity.UsuarioEntity

@Database(
    entities = [
        UsuarioEntity::class,
        DocumentoEntity::class,
        DocumentoSignatarioEntity::class,
        CampoAssinaturaEntity::class,
        EventoDocumentoEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class LetsSignDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun documentoDao(): DocumentoDao
    abstract fun documentoSignatarioDao(): DocumentoSignatarioDao
    abstract fun campoAssinaturaDao(): CampoAssinaturaDao
    abstract fun eventoDocumentoDao(): EventoDocumentoDao

    companion object {
        const val NOME = "letssign.db"
    }
}
