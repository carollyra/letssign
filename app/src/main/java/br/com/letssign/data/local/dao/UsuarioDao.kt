package br.com.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.letssign.data.local.entity.UsuarioEntity
import br.com.letssign.domain.model.Perfil
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert
    suspend fun inserir(usuario: UsuarioEntity): Long

    @Update
    suspend fun atualizar(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun buscarPorId(id: Long): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE id = :id")
    fun observarPorId(id: Long): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuarios WHERE email = :email")
    suspend fun buscarPorEmail(email: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE perfil = :perfil ORDER BY nome COLLATE NOCASE")
    fun observarPorPerfil(perfil: Perfil): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuarios WHERE id IN (:ids)")
    suspend fun buscarPorIds(ids: List<Long>): List<UsuarioEntity>

    @Query("SELECT COUNT(*) FROM usuarios WHERE perfil = :perfil")
    suspend fun contarPorPerfil(perfil: Perfil): Int
}
