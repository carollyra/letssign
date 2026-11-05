package br.com.letssign.domain.model

/** O documento não existe ou não pertence a quem está consultando. */
class DocumentoNaoEncontradoException : Exception()

/** Tentativa de alterar um documento que já foi enviado. */
class DocumentoNaoEditavelException : Exception()

class SessaoEncerradaException : Exception()

class SemPermissaoException : Exception()
