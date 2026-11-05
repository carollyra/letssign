package br.com.letssign.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.letssign.R
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.domain.model.StatusAssinatura
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.domain.repository.MotivoFalhaPdf

@get:StringRes
val StatusDocumento.rotulo: Int
    get() = when (this) {
        StatusDocumento.RASCUNHO -> R.string.status_rascunho
        StatusDocumento.ENVIADO -> R.string.status_enviado
        StatusDocumento.PARCIALMENTE_ASSINADO -> R.string.status_parcialmente_assinado
        StatusDocumento.CONCLUIDO -> R.string.status_concluido
    }

@get:StringRes
val PapelSignatario.rotulo: Int
    get() = when (this) {
        PapelSignatario.CONTRATANTE -> R.string.papel_contratante
        PapelSignatario.CONTRATADO -> R.string.papel_contratado
        PapelSignatario.TESTEMUNHA -> R.string.papel_testemunha
    }

@get:StringRes
val StatusAssinatura.rotulo: Int
    get() = when (this) {
        StatusAssinatura.PENDENTE -> R.string.assinatura_pendente
        StatusAssinatura.ASSINADO -> R.string.assinatura_assinado
    }

@get:StringRes
val MotivoFalhaPdf.mensagem: Int
    get() = when (this) {
        MotivoFalhaPdf.NAO_E_PDF -> R.string.pdf_erro_nao_e_pdf
        MotivoFalhaPdf.CORROMPIDO -> R.string.pdf_erro_corrompido
        MotivoFalhaPdf.PROTEGIDO_POR_SENHA -> R.string.pdf_erro_protegido
        MotivoFalhaPdf.SEM_PAGINAS -> R.string.pdf_erro_sem_paginas
        MotivoFalhaPdf.MUITO_GRANDE -> R.string.pdf_erro_muito_grande
        MotivoFalhaPdf.NAO_FOI_POSSIVEL_LER -> R.string.pdf_erro_leitura
    }

@Composable
fun EtiquetaStatus(status: StatusDocumento, modifier: Modifier = Modifier) {
    val escuro = isSystemInDarkTheme()
    val (fundo, texto) = when (status) {
        StatusDocumento.RASCUNHO ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        StatusDocumento.ENVIADO ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        StatusDocumento.PARCIALMENTE_ASSINADO ->
            if (escuro) Color(0xFF5C4200) to Color(0xFFFFDEA6) else Color(0xFFFFDEA6) to Color(0xFF3F2E00)
        StatusDocumento.CONCLUIDO ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Etiqueta(texto = stringResource(status.rotulo), fundo = fundo, corTexto = texto, modifier = modifier)
}

@Composable
fun EtiquetaAssinatura(status: StatusAssinatura, modifier: Modifier = Modifier) {
    val (fundo, texto) = when (status) {
        StatusAssinatura.PENDENTE ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        StatusAssinatura.ASSINADO ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Etiqueta(texto = stringResource(status.rotulo), fundo = fundo, corTexto = texto, modifier = modifier)
}

@Composable
private fun Etiqueta(texto: String, fundo: Color, corTexto: Color, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = corTexto,
        maxLines = 1,
        modifier = modifier
            .background(fundo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
