package br.com.letssign.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DispatcherIo

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DispatcherPadrao

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @DispatcherIo
    fun io(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DispatcherPadrao
    fun padrao(): CoroutineDispatcher = Dispatchers.Default
}
