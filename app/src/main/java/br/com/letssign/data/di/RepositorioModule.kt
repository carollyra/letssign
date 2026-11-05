package br.com.letssign.data.di

import br.com.letssign.data.repository.AutenticacaoRepositoryLocal
import br.com.letssign.data.repository.SignatarioRepositoryLocal
import br.com.letssign.domain.repository.AutenticacaoRepository
import br.com.letssign.domain.repository.SignatarioRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositorioModule {

    @Binds
    @Singleton
    abstract fun autenticacao(impl: AutenticacaoRepositoryLocal): AutenticacaoRepository

    @Binds
    @Singleton
    abstract fun signatarios(impl: SignatarioRepositoryLocal): SignatarioRepository
}
