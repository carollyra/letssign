package br.com.letssign.data.sessao

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface ArmazenamentoSessao {
    val usuarioId: Flow<Long?>
    suspend fun salvar(usuarioId: Long)
    suspend fun limpar()
}

class ArmazenamentoSessaoDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ArmazenamentoSessao {

    override val usuarioId: Flow<Long?> =
        dataStore.data.map { it[CHAVE_USUARIO] }.distinctUntilChanged()

    override suspend fun salvar(usuarioId: Long) {
        dataStore.edit { it[CHAVE_USUARIO] = usuarioId }
    }

    override suspend fun limpar() {
        dataStore.edit { it.remove(CHAVE_USUARIO) }
    }

    private companion object {
        val CHAVE_USUARIO = longPreferencesKey("usuario_id")
    }
}
