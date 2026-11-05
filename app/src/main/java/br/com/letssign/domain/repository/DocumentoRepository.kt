package br.com.letssign.domain.repository

import br.com.letssign.domain.model.ArquivoPdf
import br.com.letssign.domain.model.Documento
import br.com.letssign.domain.model.DocumentoDetalhado
import br.com.letssign.domain.model.DocumentoDoSignatario
import br.com.letssign.domain.model.DocumentoParaAssinar
import br.com.letssign.domain.model.EventoDetalhado
import br.com.letssign.domain.model.PapelSignatario
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.domain.regra.AreaNormalizada
import kotlinx.coroutines.flow.Flow

data class SelecaoSignatario(
    val signatarioId: Long,
    val papel: PapelSignatario,
    val obrigatorio: Boolean = true,
)

data class NovoCampo(
    val signatarioId: Long,
    val pagina: Int,
    val area: AreaNormalizada,
)

enum class ResultadoRegistroEnvio { REGISTRADO, JA_ESTAVA_REGISTRADO }

sealed interface ResultadoRegistroAssinatura {
    /** [caminhoSubstituido] é o arquivo que deixou de ser a versão atual do documento. */
    data class Registrada(val status: StatusDocumento, val caminhoSubstituido: String) : ResultadoRegistroAssinatura
    data object JaAssinado : ResultadoRegistroAssinatura
    data object DocumentoConcluido : ResultadoRegistroAssinatura

    /** O documento mudou depois que a assinatura foi aplicada; é preciso aplicar de novo. */
    data object ArquivoDesatualizado : ResultadoRegistroAssinatura
}

/**
 * Todas as operações recebem quem está consultando. Um documento de outro Admin se comporta
 * como inexistente: leituras devolvem `null` e escritas lançam
 * [br.com.letssign.domain.model.DocumentoNaoEncontradoException].
 *
 * Escritas em documento já enviado lançam
 * [br.com.letssign.domain.model.DocumentoNaoEditavelException].
 */
interface DocumentoRepository {

    fun observarDoAdmin(adminId: Long): Flow<List<Documento>>

    fun observarDetalheDoAdmin(documentoId: Long, adminId: Long): Flow<DocumentoDetalhado?>

    suspend fun buscarDetalheDoAdmin(documentoId: Long, adminId: Long): DocumentoDetalhado?

    suspend fun criarRascunho(adminId: Long, titulo: String, arquivo: ArquivoPdf, agora: Long): Long

    /**
     * Com [novoArquivo] o PDF do rascunho é trocado e os campos já posicionados são
     * descartados, porque pertenciam às páginas do arquivo anterior.
     */
    suspend fun atualizarRascunho(
        documentoId: Long,
        adminId: Long,
        titulo: String,
        novoArquivo: ArquivoPdf?,
    )

    /** Substitui a seleção inteira. Quem continua selecionado mantém os campos que já tinha. */
    suspend fun definirSignatarios(documentoId: Long, adminId: Long, selecao: List<SelecaoSignatario>)

    /**
     * Substitui todos os campos do rascunho. Lança [IllegalArgumentException] se algum campo
     * for de quem não é signatário do documento, apontar para uma página inexistente ou
     * ultrapassar os limites da página.
     */
    suspend fun salvarCampos(documentoId: Long, adminId: Long, campos: List<NovoCampo>)

    suspend fun salvarMensagemConvite(documentoId: Long, adminId: Long, mensagem: String)

    /**
     * Passa o rascunho para enviado numa única transação. Chamar de novo para um documento
     * que já saiu não altera nada e devolve [ResultadoRegistroEnvio.JA_ESTAVA_REGISTRADO].
     *
     * As regras de envio são conferidas de novo dentro da transação; se alguma falhar, lança
     * [br.com.letssign.domain.model.EnvioInvalidoException].
     */
    suspend fun registrarEnvio(
        documentoId: Long,
        adminId: Long,
        mensagemConvite: String,
        agora: Long,
    ): ResultadoRegistroEnvio

    /** Histórico do documento, do evento mais antigo para o mais recente. */
    fun observarEventos(documentoId: Long, adminId: Long): Flow<List<EventoDetalhado>>

    /** Documentos enviados ao signatário. Rascunhos nunca aparecem. */
    fun observarDoSignatario(signatarioId: Long): Flow<List<DocumentoDoSignatario>>

    /** Documento enviado ao signatário, só com os campos dele. `null` para rascunho ou documento alheio. */
    suspend fun buscarParaAssinar(documentoId: Long, signatarioId: Long): DocumentoParaAssinar?

    /**
     * Marca o vínculo como assinado, troca o arquivo atual do documento por [caminhoAssinado]
     * e recalcula a situação, tudo numa transação. [caminhoBase] é o arquivo sobre o qual a
     * assinatura foi aplicada: se ele não for mais o atual, nada é gravado.
     */
    suspend fun registrarAssinatura(
        documentoId: Long,
        signatarioId: Long,
        caminhoBase: String,
        caminhoAssinado: String,
        agora: Long,
    ): ResultadoRegistroAssinatura
}
