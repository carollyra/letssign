package br.com.letssign.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import br.com.letssign.data.sessao.ArmazenamentoSessao
import br.com.letssign.data.sessao.ArmazenamentoSessaoDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessaoModule {

    @Binds
    abstract fun armazenamento(impl: ArmazenamentoSessaoDataStore): ArmazenamentoSessao

    companion object {
        @Provides
        @Singleton
        fun dataStore(@ApplicationContext contexto: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { contexto.preferencesDataStoreFile("sessao") }
    }
}
