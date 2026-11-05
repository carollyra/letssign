package br.com.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.letssign.domain.model.Perfil

@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["email"], unique = true)],
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val email: String,
    val telefone: String,
    val senhaHash: String,
    val salt: String,
    val perfil: Perfil,
)
