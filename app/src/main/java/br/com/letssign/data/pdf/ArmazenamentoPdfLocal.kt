package br.com.letssign.data.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import br.com.letssign.di.DispatcherIo
import br.com.letssign.domain.model.ArquivoPdf
import br.com.letssign.domain.repository.ArmazenamentoPdf
import br.com.letssign.domain.repository.MotivoFalhaPdf
import br.com.letssign.domain.repository.ResultadoImportacao
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ArmazenamentoPdfLocal @Inject constructor(
    @ApplicationContext private val contexto: Context,
    @DispatcherIo private val dispatcher: CoroutineDispatcher,
) : ArmazenamentoPdf {

    private val pasta: File
        get() = File(contexto.filesDir, PASTA).apply { mkdirs() }

    override suspend fun importar(origem: String): ResultadoImportacao = withContext(dispatcher) {
        val uri = Uri.parse(origem)
        val destino = novoArquivo()
        try {
            if (!copiar(uri, destino)) {
                return@withContext falha(destino, MotivoFalhaPdf.NAO_FOI_POSSIVEL_LER)
            }
            if (destino.length() > TAMANHO_MAXIMO_BYTES) {
                return@withContext falha(destino, MotivoFalhaPdf.MUITO_GRANDE)
            }
            when (val conferencia = conferir(destino)) {
                is Conferencia.Recusado -> falha(destino, conferencia.motivo)
                is Conferencia.Aceito -> ResultadoImportacao.Sucesso(
                    ArquivoPdf(
                        nomeArquivo = nomeDe(uri),
                        caminho = destino.absolutePath,
                        totalPaginas = conferencia.totalPaginas,
                    ),
                )
            }
        } catch (e: OutOfMemoryError) {
            falha(destino, MotivoFalhaPdf.MUITO_GRANDE)
        }
    }

    override suspend fun duplicar(caminho: String): String = withContext(dispatcher) {
        val copia = novoArquivo()
        File(caminho).copyTo(copia, overwrite = true)
        copia.absolutePath
    }

    override suspend fun remover(caminho: String) {
        withContext(dispatcher) {
            val arquivo = File(caminho)
            // Só apaga o que está na pasta de documentos do app.
            if (arquivo.parentFile == pasta) arquivo.delete()
        }
    }

    private fun novoArquivo() = File(pasta, "${UUID.randomUUID()}.pdf")

    private fun falha(arquivo: File, motivo: MotivoFalhaPdf): ResultadoImportacao {
        arquivo.delete()
        return ResultadoImportacao.Falha(motivo)
    }

    private fun copiar(uri: Uri, destino: File): Boolean = try {
        val entrada = contexto.contentResolver.openInputStream(uri)
        if (entrada == null) {
            false
        } else {
            entrada.use { origem -> destino.outputStream().use { origem.copyTo(it) } }
            true
        }
    } catch (e: IOException) {
        false
    } catch (e: SecurityException) {
        false
    }

    private fun nomeDe(uri: Uri): String {
        val nome = try {
            contexto.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        } catch (e: Exception) {
            null
        }
        return nome?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment ?: NOME_PADRAO
    }

    private sealed interface Conferencia {
        data class Aceito(val totalPaginas: Int) : Conferencia
        data class Recusado(val motivo: MotivoFalhaPdf) : Conferencia
    }

    /**
     * O arquivo precisa abrir nas duas bibliotecas: o PdfRenderer exibe as páginas e o PDFBox
     * grava as assinaturas. Recusar aqui evita descobrir o problema só na hora de assinar.
     */
    private fun conferir(arquivo: File): Conferencia {
        if (!temCabecalhoPdf(arquivo)) return Conferencia.Recusado(MotivoFalhaPdf.NAO_E_PDF)

        PdfBox.garantirIniciado(contexto)
        try {
            PDDocument.load(arquivo, MemoryUsageSetting.setupTempFileOnly()).use { documento ->
                if (documento.isEncrypted) {
                    return Conferencia.Recusado(MotivoFalhaPdf.PROTEGIDO_POR_SENHA)
                }
                if (documento.numberOfPages == 0) {
                    return Conferencia.Recusado(MotivoFalhaPdf.SEM_PAGINAS)
                }
            }
        } catch (e: InvalidPasswordException) {
            return Conferencia.Recusado(MotivoFalhaPdf.PROTEGIDO_POR_SENHA)
        } catch (e: IOException) {
            return Conferencia.Recusado(MotivoFalhaPdf.CORROMPIDO)
        } catch (e: RuntimeException) {
            return Conferencia.Recusado(MotivoFalhaPdf.CORROMPIDO)
        }

        return try {
            ParcelFileDescriptor.open(arquivo, ParcelFileDescriptor.MODE_READ_ONLY).use { descritor ->
                PdfRenderer(descritor).use { renderizador ->
                    if (renderizador.pageCount == 0) {
                        Conferencia.Recusado(MotivoFalhaPdf.SEM_PAGINAS)
                    } else {
                        Conferencia.Aceito(renderizador.pageCount)
                    }
                }
            }
        } catch (e: SecurityException) {
            Conferencia.Recusado(MotivoFalhaPdf.PROTEGIDO_POR_SENHA)
        } catch (e: IOException) {
            Conferencia.Recusado(MotivoFalhaPdf.CORROMPIDO)
        } catch (e: RuntimeException) {
            Conferencia.Recusado(MotivoFalhaPdf.NAO_FOI_POSSIVEL_LER)
        }
    }

    private fun temCabecalhoPdf(arquivo: File): Boolean {
        val inicio = ByteArray(TAMANHO_BUSCA_CABECALHO)
        val lidos = arquivo.inputStream().use { it.read(inicio) }
        if (lidos <= 0) return false
        return String(inicio, 0, lidos, Charsets.ISO_8859_1).contains("%PDF-")
    }

    private companion object {
        const val PASTA = "documentos"
        const val NOME_PADRAO = "documento.pdf"
        const val TAMANHO_MAXIMO_BYTES = 50L * 1024 * 1024

        // A especificação tolera lixo antes do cabeçalho, desde que ele apareça no primeiro KB.
        const val TAMANHO_BUSCA_CABECALHO = 1024
    }
}
