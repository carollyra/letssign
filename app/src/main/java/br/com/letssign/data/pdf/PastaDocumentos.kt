package br.com.letssign.data.pdf

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/** Pasta do armazenamento interno onde ficam os PDFs importados e os assinados. */
class PastaDocumentos @Inject constructor(
    @ApplicationContext private val contexto: Context,
) {
    val pasta: File
        get() = File(contexto.filesDir, NOME).apply { mkdirs() }

    fun novoArquivo(): File = File(pasta, "${UUID.randomUUID()}.pdf")

    fun contem(arquivo: File): Boolean = arquivo.parentFile == pasta

    private companion object {
        const val NOME = "documentos"
    }
}
