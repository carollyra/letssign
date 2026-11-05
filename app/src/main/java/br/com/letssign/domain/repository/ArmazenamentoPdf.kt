package br.com.letssign.domain.repository

import br.com.letssign.domain.model.ArquivoPdf

enum class MotivoFalhaPdf {
    NAO_E_PDF,
    CORROMPIDO,
    PROTEGIDO_POR_SENHA,
    SEM_PAGINAS,
    MUITO_GRANDE,
    NAO_FOI_POSSIVEL_LER,
}

sealed interface ResultadoImportacao {
    data class Sucesso(val arquivo: ArquivoPdf) : ResultadoImportacao
    data class Falha(val motivo: MotivoFalhaPdf) : ResultadoImportacao
}

interface ArmazenamentoPdf {

    /**
     * Copia o PDF apontado por [origem] (URI devolvida pelo seletor de arquivos) para o
     * armazenamento do app e confere se ele pode ser lido e alterado.
     */
    suspend fun importar(origem: String): ResultadoImportacao

    /** Copia um arquivo já importado e devolve o caminho da cópia. */
    suspend fun duplicar(caminho: String): String

    suspend fun remover(caminho: String)
}
