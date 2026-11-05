package br.com.letssign.domain.model

import br.com.letssign.domain.regra.ProblemaEnvio

/** O documento não existe ou não pertence a quem está consultando. */
class DocumentoNaoEncontradoException : Exception()

/** Tentativa de alterar um documento que já foi enviado. */
class DocumentoNaoEditavelException : Exception()

/** Tentativa de registrar o envio de um documento que não cumpre as regras de envio. */
class EnvioInvalidoException(val problemas: List<ProblemaEnvio>) : Exception()

class SessaoEncerradaException : Exception()

class SemPermissaoException : Exception()
