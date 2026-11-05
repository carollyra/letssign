package br.com.letssign.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DispatcherIo

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DispatcherPadrao

/** Escopo que sobrevive às telas, para trabalho que precisa terminar depois que elas fecham. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EscopoAplicacao

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @DispatcherIo
    fun io(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DispatcherPadrao
    fun padrao(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @EscopoAplicacao
    fun escopoAplicacao(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
