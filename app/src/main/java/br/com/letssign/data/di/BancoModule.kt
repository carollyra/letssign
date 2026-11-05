package br.com.letssign.data.di

import android.content.Context
import androidx.room.Room
import br.com.letssign.data.local.LetsSignDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BancoModule {

    @Provides
    @Singleton
    fun banco(@ApplicationContext contexto: Context): LetsSignDatabase =
        Room.databaseBuilder(contexto, LetsSignDatabase::class.java, LetsSignDatabase.NOME).build()

    @Provides
    fun usuarioDao(banco: LetsSignDatabase) = banco.usuarioDao()

    @Provides
    fun documentoDao(banco: LetsSignDatabase) = banco.documentoDao()

    @Provides
    fun documentoSignatarioDao(banco: LetsSignDatabase) = banco.documentoSignatarioDao()

    @Provides
    fun campoAssinaturaDao(banco: LetsSignDatabase) = banco.campoAssinaturaDao()

    @Provides
    fun eventoDocumentoDao(banco: LetsSignDatabase) = banco.eventoDocumentoDao()
}
