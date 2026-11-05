package br.com.letssign.domain.model

enum class TipoEvento { CRIADO, ENVIADO, ASSINADO, CONCLUIDO, NOVA_VERSAO }

data class EventoDocumento(
    val id: Long,
    val documentoId: Long,
    val tipo: TipoEvento,
    val usuarioId: Long,
    val dataHora: Long,
)

data class EventoDetalhado(
    val evento: EventoDocumento,
    val nomeUsuario: String,
)
