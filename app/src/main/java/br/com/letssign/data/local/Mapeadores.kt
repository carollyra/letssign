package br.com.letssign.data.local

import br.com.letssign.data.local.dao.EventoComUsuario
import br.com.letssign.data.local.entity.CampoAssinaturaEntity
import br.com.letssign.data.local.entity.DocumentoEntity
import br.com.letssign.data.local.entity.DocumentoSignatarioEntity
import br.com.letssign.data.local.entity.EventoDocumentoEntity
import br.com.letssign.data.local.entity.UsuarioEntity
import br.com.letssign.domain.model.CampoAssinatura
import br.com.letssign.domain.model.Documento
import br.com.letssign.domain.model.DocumentoSignatario
import br.com.letssign.domain.model.EventoDetalhado
import br.com.letssign.domain.model.EventoDocumento
import br.com.letssign.domain.model.Usuario

fun UsuarioEntity.paraDominio() = Usuario(
    id = id,
    nome = nome,
    email = email,
    telefone = telefone,
    perfil = perfil,
)

fun DocumentoEntity.paraDominio() = Documento(
    id = id,
    titulo = titulo,
    nomeArquivo = nomeArquivo,
    caminhoOriginal = caminhoOriginal,
    caminhoAtual = caminhoAtual,
    totalPaginas = totalPaginas,
    status = status,
    versao = versao,
    documentoOrigemId = documentoOrigemId,
    adminId = adminId,
    mensagemConvite = mensagemConvite,
    criadoEm = criadoEm,
    enviadoEm = enviadoEm,
    concluidoEm = concluidoEm,
)

fun DocumentoSignatarioEntity.paraDominio() = DocumentoSignatario(
    id = id,
    documentoId = documentoId,
    signatarioId = signatarioId,
    papel = papel,
    obrigatorio = obrigatorio,
    status = status,
    assinadoEm = assinadoEm,
)

fun CampoAssinaturaEntity.paraDominio() = CampoAssinatura(
    id = id,
    documentoSignatarioId = documentoSignatarioId,
    pagina = pagina,
    x = x,
    y = y,
    largura = largura,
    altura = altura,
)

fun EventoDocumentoEntity.paraDominio() = EventoDocumento(
    id = id,
    documentoId = documentoId,
    tipo = tipo,
    usuarioId = usuarioId,
    dataHora = dataHora,
)

fun EventoComUsuario.paraDominio() = EventoDetalhado(
    evento = evento.paraDominio(),
    nomeUsuario = nomeUsuario,
)
