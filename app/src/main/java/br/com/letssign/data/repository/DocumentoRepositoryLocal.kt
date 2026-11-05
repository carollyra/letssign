package br.com.letssign.data.repository

import androidx.room.withTransaction
import br.com.letssign.data.local.LetsSignDatabase
import br.com.letssign.data.local.entity.CampoAssinaturaEntity
import br.com.letssign.data.local.entity.DocumentoEntity
import br.com.letssign.data.local.entity.DocumentoSignatarioEntity
import br.com.letssign.data.local.entity.EventoDocumentoEntity
import br.com.letssign.data.local.entity.UsuarioEntity
import br.com.letssign.data.local.paraDominio
import br.com.letssign.domain.model.ArquivoPdf
import br.com.letssign.domain.model.Documento
import br.com.letssign.domain.model.DocumentoDetalhado
import br.com.letssign.domain.model.DocumentoDoSignatario
import br.com.letssign.domain.model.EnvioInvalidoException
import br.com.letssign.domain.model.EventoDetalhado
import br.com.letssign.domain.model.DocumentoNaoEditavelException
import br.com.letssign.domain.model.DocumentoNaoEncontradoException
import br.com.letssign.domain.model.Perfil
import br.com.letssign.domain.model.SignatarioDoDocumento
import br.com.letssign.domain.model.StatusDocumento
import br.com.letssign.domain.model.TipoEvento
import br.com.letssign.domain.regra.GeometriaCampo
import br.com.letssign.domain.regra.ValidadorEnvio
import br.com.letssign.domain.repository.DocumentoRepository
import br.com.letssign.domain.repository.NovoCampo
import br.com.letssign.domain.repository.ResultadoRegistroEnvio
import br.com.letssign.domain.repository.SelecaoSignatario
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DocumentoRepositoryLocal @Inject constructor(
    private val banco: LetsSignDatabase,
) : DocumentoRepository {

    private val documentoDao = banco.documentoDao()
    private val vinculoDao = banco.documentoSignatarioDao()
    private val campoDao = banco.campoAssinaturaDao()
    private val eventoDao = banco.eventoDocumentoDao()
    private val usuarioDao = banco.usuarioDao()

    override fun observarDoAdmin(adminId: Long): Flow<List<Documento>> =
        documentoDao.observarTodosDoAdmin(adminId).map { lista -> lista.map { it.paraDominio() } }

    override fun observarDetalheDoAdmin(documentoId: Long, adminId: Long): Flow<DocumentoDetalhado?> =
        combine(
            documentoDao.observarDoAdmin(documentoId, adminId),
            vinculoDao.observarDoDocumento(documentoId),
            campoDao.observarDoDocumento(documentoId),
            usuarioDao.observarPorPerfil(Perfil.SIGNATARIO),
        ) { documento, vinculos, campos, usuarios ->
            documento?.let { montarDetalhe(it, vinculos, campos, usuarios) }
        }

    override suspend fun buscarDetalheDoAdmin(documentoId: Long, adminId: Long): DocumentoDetalhado? =
        banco.withTransaction {
            val documento = documentoDao.buscarDoAdmin(documentoId, adminId)
                ?: return@withTransaction null
            val vinculos = vinculoDao.listarDoDocumento(documentoId)
            montarDetalhe(
                documento = documento,
                vinculos = vinculos,
                campos = campoDao.listarDoDocumento(documentoId),
                usuarios = usuarioDao.buscarPorIds(vinculos.map { it.signatarioId }),
            )
        }

    override suspend fun criarRascunho(
        adminId: Long,
        titulo: String,
        arquivo: ArquivoPdf,
        agora: Long,
    ): Long = banco.withTransaction {
        val id = documentoDao.inserir(
            DocumentoEntity(
                titulo = titulo,
                nomeArquivo = arquivo.nomeArquivo,
                caminhoOriginal = arquivo.caminho,
                caminhoAtual = arquivo.caminho,
                totalPaginas = arquivo.totalPaginas,
                status = StatusDocumento.RASCUNHO,
                versao = 1,
                documentoOrigemId = null,
                adminId = adminId,
                mensagemConvite = "",
                criadoEm = agora,
                enviadoEm = null,
                concluidoEm = null,
            ),
        )
        registrarEvento(id, TipoEvento.CRIADO, adminId, agora)
        id
    }

    override suspend fun atualizarRascunho(
        documentoId: Long,
        adminId: Long,
        titulo: String,
        novoArquivo: ArquivoPdf?,
    ) = banco.withTransaction {
        val rascunho = rascunhoDoAdmin(documentoId, adminId)
        if (novoArquivo == null) {
            documentoDao.atualizar(rascunho.copy(titulo = titulo))
        } else {
            campoDao.excluirDoDocumento(documentoId)
            documentoDao.atualizar(
                rascunho.copy(
                    titulo = titulo,
                    nomeArquivo = novoArquivo.nomeArquivo,
                    caminhoOriginal = novoArquivo.caminho,
                    caminhoAtual = novoArquivo.caminho,
                    totalPaginas = novoArquivo.totalPaginas,
                ),
            )
        }
    }

    override suspend fun definirSignatarios(
        documentoId: Long,
        adminId: Long,
        selecao: List<SelecaoSignatario>,
    ) = banco.withTransaction {
        rascunhoDoAdmin(documentoId, adminId)
        val ids = selecao.map { it.signatarioId }
        val cadastrados = usuarioDao.buscarPorIds(ids).filter { it.perfil == Perfil.SIGNATARIO }
        require(cadastrados.size == ids.toSet().size) { "Seleção contém quem não é signatário" }

        val atuais = vinculoDao.listarDoDocumento(documentoId).associateBy { it.signatarioId }
        vinculoDao.excluir(atuais.values.filter { it.signatarioId !in ids }.map { it.id })
        selecao.forEach { escolhido ->
            val atual = atuais[escolhido.signatarioId]
            if (atual == null) {
                vinculoDao.inserir(
                    DocumentoSignatarioEntity(
                        documentoId = documentoId,
                        signatarioId = escolhido.signatarioId,
                        papel = escolhido.papel,
                        obrigatorio = escolhido.obrigatorio,
                    ),
                )
            } else {
                vinculoDao.atualizar(
                    atual.copy(papel = escolhido.papel, obrigatorio = escolhido.obrigatorio),
                )
            }
        }
    }

    override suspend fun salvarCampos(
        documentoId: Long,
        adminId: Long,
        campos: List<NovoCampo>,
    ) = banco.withTransaction {
        val rascunho = rascunhoDoAdmin(documentoId, adminId)
        val vinculoPorSignatario = vinculoDao.listarDoDocumento(documentoId)
            .associate { it.signatarioId to it.id }
        val novos = campos.map { campo ->
            val vinculoId = requireNotNull(vinculoPorSignatario[campo.signatarioId]) {
                "Campo de quem não é signatário do documento"
            }
            require(campo.pagina in 0 until rascunho.totalPaginas) { "Página inexistente" }
            require(GeometriaCampo.dentroDaPagina(campo.area)) { "Campo fora dos limites da página" }
            CampoAssinaturaEntity(
                documentoSignatarioId = vinculoId,
                pagina = campo.pagina,
                x = campo.area.x,
                y = campo.area.y,
                largura = campo.area.largura,
                altura = campo.area.altura,
            )
        }
        campoDao.excluirDoDocumento(documentoId)
        campoDao.inserir(novos)
    }

    override suspend fun salvarMensagemConvite(documentoId: Long, adminId: Long, mensagem: String) =
        banco.withTransaction {
            val rascunho = rascunhoDoAdmin(documentoId, adminId)
            documentoDao.atualizar(rascunho.copy(mensagemConvite = mensagem))
        }

    override suspend fun registrarEnvio(
        documentoId: Long,
        adminId: Long,
        mensagemConvite: String,
        agora: Long,
    ): ResultadoRegistroEnvio = banco.withTransaction {
        val documento = documentoDao.buscarDoAdmin(documentoId, adminId)
            ?: throw DocumentoNaoEncontradoException()
        if (documento.status != StatusDocumento.RASCUNHO) {
            return@withTransaction ResultadoRegistroEnvio.JA_ESTAVA_REGISTRADO
        }

        val vinculos = vinculoDao.listarDoDocumento(documentoId)
        val detalhe = montarDetalhe(
            documento = documento,
            vinculos = vinculos,
            campos = campoDao.listarDoDocumento(documentoId),
            usuarios = usuarioDao.buscarPorIds(vinculos.map { it.signatarioId }),
        )
        val problemas = ValidadorEnvio.validar(detalhe, mensagemConvite)
        if (problemas.isNotEmpty()) throw EnvioInvalidoException(problemas)

        documentoDao.atualizar(
            documento.copy(
                status = StatusDocumento.ENVIADO,
                mensagemConvite = mensagemConvite,
                enviadoEm = agora,
            ),
        )
        registrarEvento(documentoId, TipoEvento.ENVIADO, adminId, agora)
        ResultadoRegistroEnvio.REGISTRADO
    }

    override fun observarEventos(documentoId: Long, adminId: Long): Flow<List<EventoDetalhado>> =
        eventoDao.observarDoDocumento(documentoId, adminId).map { lista -> lista.map { it.paraDominio() } }

    override fun observarDoSignatario(signatarioId: Long): Flow<List<DocumentoDoSignatario>> =
        documentoDao.observarEnviadosAoSignatario(signatarioId).map { lista ->
            lista.map { DocumentoDoSignatario(it.documento.paraDominio(), it.vinculo.paraDominio()) }
        }

    private suspend fun rascunhoDoAdmin(documentoId: Long, adminId: Long): DocumentoEntity {
        val documento = documentoDao.buscarDoAdmin(documentoId, adminId)
            ?: throw DocumentoNaoEncontradoException()
        if (documento.status != StatusDocumento.RASCUNHO) throw DocumentoNaoEditavelException()
        return documento
    }

    private suspend fun registrarEvento(documentoId: Long, tipo: TipoEvento, usuarioId: Long, agora: Long) {
        eventoDao.inserir(
            EventoDocumentoEntity(
                documentoId = documentoId,
                tipo = tipo,
                usuarioId = usuarioId,
                dataHora = agora,
            ),
        )
    }

    private fun montarDetalhe(
        documento: DocumentoEntity,
        vinculos: List<DocumentoSignatarioEntity>,
        campos: List<CampoAssinaturaEntity>,
        usuarios: List<UsuarioEntity>,
    ): DocumentoDetalhado {
        val usuariosPorId = usuarios.associateBy { it.id }
        val camposPorVinculo = campos.groupBy { it.documentoSignatarioId }
        return DocumentoDetalhado(
            documento = documento.paraDominio(),
            signatarios = vinculos.mapNotNull { vinculo ->
                usuariosPorId[vinculo.signatarioId]?.let { usuario ->
                    SignatarioDoDocumento(
                        vinculo = vinculo.paraDominio(),
                        usuario = usuario.paraDominio(),
                        campos = camposPorVinculo[vinculo.id].orEmpty().map { it.paraDominio() },
                    )
                }
            },
        )
    }
}
