package br.com.letssign.ui.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Leitura das páginas de um PDF local para exibição. */
class DocumentoPdf private constructor(
    private val descritor: ParcelFileDescriptor,
    private val renderizador: PdfRenderer,
) {
    val totalPaginas: Int = renderizador.pageCount

    // O PdfRenderer só aceita uma página aberta por vez.
    private val trava = Mutex()
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val proporcoes = arrayOfNulls<Float>(totalPaginas)
    private var fechado = false

    /** Largura dividida pela altura da página, já considerando a rotação. */
    suspend fun proporcao(indice: Int): Float {
        proporcoes[indice]?.let { return it }
        return withContext(Dispatchers.IO) {
            trava.withLock {
                check(!fechado)
                renderizador.openPage(indice).use { pagina ->
                    (pagina.width / pagina.height.toFloat()).also { proporcoes[indice] = it }
                }
            }
        }
    }

    suspend fun renderizar(indice: Int, larguraPx: Int): Bitmap = withContext(Dispatchers.IO) {
        trava.withLock {
            check(!fechado)
            renderizador.openPage(indice).use { pagina ->
                val largura = larguraPx.coerceIn(1, LARGURA_MAXIMA_PX)
                val altura = (largura * pagina.height / pagina.width.toFloat())
                    .roundToInt()
                    .coerceAtLeast(1)
                Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888).also { bitmap ->
                    bitmap.eraseColor(Color.WHITE)
                    pagina.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
        }
    }

    fun fechar() {
        escopo.launch {
            trava.withLock {
                if (!fechado) {
                    fechado = true
                    renderizador.close()
                    descritor.close()
                }
            }
        }
    }

    companion object {
        private const val LARGURA_MAXIMA_PX = 2000

        // Não cancelável para o descritor nunca ficar aberto sem dono se a tela fechar no meio.
        suspend fun abrir(caminho: String): DocumentoPdf =
            withContext(Dispatchers.IO + NonCancellable) {
                val descritor =
                    ParcelFileDescriptor.open(File(caminho), ParcelFileDescriptor.MODE_READ_ONLY)
                try {
                    DocumentoPdf(descritor, PdfRenderer(descritor))
                } catch (e: Exception) {
                    descritor.close()
                    throw e
                }
            }
    }
}
