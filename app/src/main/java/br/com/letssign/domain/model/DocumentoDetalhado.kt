package br.com.letssign.domain.model

data class SignatarioDoDocumento(
    val vinculo: DocumentoSignatario,
    val usuario: Usuario,
    val campos: List<CampoAssinatura>,
)

data class DocumentoDetalhado(
    val documento: Documento,
    val signatarios: List<SignatarioDoDocumento>,
)

/** Visão que o signatário tem de um documento: só o próprio vínculo e os próprios campos. */
data class DocumentoDoSignatario(
    val documento: Documento,
    val vinculo: DocumentoSignatario,
    val campos: List<CampoAssinatura>,
)
