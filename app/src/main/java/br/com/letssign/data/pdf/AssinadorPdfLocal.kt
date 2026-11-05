package br.com.letssign.data.pdf

import android.content.Context
import android.graphics.BitmapFactory
import br.com.letssign.di.DispatcherIo
import br.com.letssign.domain.model.CampoAssinatura
import br.com.letssign.domain.regra.CaixaPagina
import br.com.letssign.domain.regra.ConversaoPdf
import br.com.letssign.domain.repository.AssinadorPdf
import br.com.letssign.domain.repository.FalhaAoAssinarPdfException
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.util.Matrix
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class AssinadorPdfLocal @Inject constructor(
    @ApplicationContext private val contexto: Context,
    private val pastaDocumentos: PastaDocumentos,
    @DispatcherIo private val dispatcher: CoroutineDispatcher,
) : AssinadorPdf {

    override suspend fun aplicar(
        caminhoOrigem: String,
        assinaturaPng: ByteArray,
        campos: List<CampoAssinatura>,
    ): String = withContext(dispatcher) {
        PdfBox.garantirIniciado(contexto)
        val assinatura = BitmapFactory.decodeByteArray(assinaturaPng, 0, assinaturaPng.size)
            ?: throw FalhaAoAssinarPdfException()
        val proporcao = assinatura.width / assinatura.height.toFloat()
        val destino = pastaDocumentos.novoArquivo()
        try {
            PDDocument.load(File(caminhoOrigem), MemoryUsageSetting.setupTempFileOnly()).use { documento ->
                val imagem = LosslessFactory.createFromImage(documento, assinatura)
                campos.groupBy { it.pagina }.forEach { (indice, camposDaPagina) ->
                    val pagina = documento.getPage(indice)
                    val recorte = pagina.cropBox
                    val caixa = CaixaPagina(
                        x = recorte.lowerLeftX,
                        y = recorte.lowerLeftY,
                        largura = recorte.width,
                        altura = recorte.height,
                        rotacao = pagina.rotation,
                    )
                    // O último `true` isola o conteúdo original, para uma transformação deixada
                    // aberta por ele não deslocar a assinatura.
                    PDPageContentStream(
                        documento,
                        pagina,
                        PDPageContentStream.AppendMode.APPEND,
                        true,
                        true,
                    ).use { conteudo ->
                        camposDaPagina.forEach { campo ->
                            val area = ConversaoPdf.encaixar(campo.area, proporcao, caixa)
                            val matriz = ConversaoPdf.matrizDoCampo(area, caixa)
                            conteudo.saveGraphicsState()
                            conteudo.transform(
                                Matrix(matriz.a, matriz.b, matriz.c, matriz.d, matriz.e, matriz.f),
                            )
                            conteudo.drawImage(imagem, 0f, 0f, 1f, 1f)
                            conteudo.restoreGraphicsState()
                        }
                    }
                }
                documento.save(destino)
            }
            destino.absolutePath
        } catch (e: IOException) {
            destino.delete()
            throw FalhaAoAssinarPdfException(e)
        } catch (e: RuntimeException) {
            destino.delete()
            throw FalhaAoAssinarPdfException(e)
        } finally {
            assinatura.recycle()
        }
    }
}
