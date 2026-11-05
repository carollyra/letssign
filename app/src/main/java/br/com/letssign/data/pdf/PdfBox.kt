package br.com.letssign.data.pdf

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

internal object PdfBox {

    @Volatile
    private var iniciado = false

    /** O PDFBox precisa carregar seus recursos uma vez antes de abrir qualquer documento. */
    fun garantirIniciado(contexto: Context) {
        if (iniciado) return
        synchronized(this) {
            if (!iniciado) {
                PDFBoxResourceLoader.init(contexto.applicationContext)
                iniciado = true
            }
        }
    }
}
