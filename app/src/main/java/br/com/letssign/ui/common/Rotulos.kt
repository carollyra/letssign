package br.com.letssign.ui.common

import androidx.annotation.StringRes
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
