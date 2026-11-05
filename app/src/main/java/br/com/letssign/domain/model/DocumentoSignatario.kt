package br.com.letssign.domain.model

enum class PapelSignatario { CONTRATANTE, CONTRATADO, TESTEMUNHA }

enum class StatusAssinatura { PENDENTE, ASSINADO }

data class DocumentoSignatario(
    val id: Long,
    val documentoId: Long,
    val signatarioId: Long,
    val papel: PapelSignatario,
    val obrigatorio: Boolean,
    val status: StatusAssinatura,
    val assinadoEm: Long?,
)
