package br.com.letssign.domain.regra

import br.com.letssign.domain.model.DocumentoDetalhado
import br.com.letssign.domain.model.SignatarioDoDocumento

enum class ErroEnvio {
    TITULO_VAZIO,
    SEM_ARQUIVO,
    MENSAGEM_VAZIA,
    SEM_SIGNATARIOS,
    SIGNATARIO_SEM_CAMPO,
    CAMPO_SEM_SIGNATARIO,
    CAMPO_FORA_DA_PAGINA,
    CAMPO_EM_PAGINA_INEXISTENTE,
}

/** [signatarios] traz os nomes envolvidos quando o problema é de pessoas específicas. */
data class ProblemaEnvio(val erro: ErroEnvio, val signatarios: List<String> = emptyList())

object ValidadorEnvio {

    /** Lista vazia significa que o documento pode ser enviado. */
    fun validar(detalhe: DocumentoDetalhado, mensagem: String): List<ProblemaEnvio> = buildList {
        val documento = detalhe.documento
        if (documento.titulo.isBlank()) add(ProblemaEnvio(ErroEnvio.TITULO_VAZIO))
        if (documento.caminhoOriginal.isBlank() || documento.totalPaginas <= 0) {
            add(ProblemaEnvio(ErroEnvio.SEM_ARQUIVO))
        }
        if (mensagem.isBlank()) add(ProblemaEnvio(ErroEnvio.MENSAGEM_VAZIA))

        if (detalhe.signatarios.isEmpty()) {
            add(ProblemaEnvio(ErroEnvio.SEM_SIGNATARIOS))
            return@buildList
        }

        nomesDe(detalhe) { it.campos.isEmpty() }
            ?.let { add(ProblemaEnvio(ErroEnvio.SIGNATARIO_SEM_CAMPO, it)) }
        nomesDe(detalhe) { signatario ->
            signatario.campos.any { it.documentoSignatarioId != signatario.vinculo.id }
        }?.let { add(ProblemaEnvio(ErroEnvio.CAMPO_SEM_SIGNATARIO, it)) }
        nomesDe(detalhe) { signatario ->
            signatario.campos.any { !GeometriaCampo.dentroDaPagina(it.area) }
        }?.let { add(ProblemaEnvio(ErroEnvio.CAMPO_FORA_DA_PAGINA, it)) }
        nomesDe(detalhe) { signatario ->
            signatario.campos.any { it.pagina !in 0 until documento.totalPaginas }
        }?.let { add(ProblemaEnvio(ErroEnvio.CAMPO_EM_PAGINA_INEXISTENTE, it)) }
    }

    private fun nomesDe(
        detalhe: DocumentoDetalhado,
        criterio: (SignatarioDoDocumento) -> Boolean,
    ): List<String>? = detalhe.signatarios.filter(criterio).map { it.usuario.nome }.ifEmpty { null }
}
