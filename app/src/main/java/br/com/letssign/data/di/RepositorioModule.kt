package br.com.letssign.data.di

import br.com.letssign.data.pdf.ArmazenamentoPdfLocal
import br.com.letssign.data.repository.AutenticacaoRepositoryLocal
import br.com.letssign.data.repository.DocumentoRepositoryLocal
import br.com.letssign.data.repository.SignatarioRepositoryLocal
import br.com.letssign.domain.Relogio
import br.com.letssign.domain.repository.ArmazenamentoPdf
import br.com.letssign.domain.repository.AutenticacaoRepository
import br.com.letssign.domain.repository.DocumentoRepository
import br.com.letssign.domain.repository.SignatarioRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
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

    @Binds
    @Singleton
    abstract fun documentos(impl: DocumentoRepositoryLocal): DocumentoRepository

    @Binds
    @Singleton
    abstract fun armazenamentoPdf(impl: ArmazenamentoPdfLocal): ArmazenamentoPdf

    companion object {
        @Provides
        fun relogio(): Relogio = Relogio { System.currentTimeMillis() }
    }
}
