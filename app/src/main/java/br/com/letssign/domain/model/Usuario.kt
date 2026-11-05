package br.com.letssign.domain.model

enum class Perfil { ADMIN, SIGNATARIO }

data class Usuario(
    val id: Long,
    val nome: String,
    val email: String,
    val telefone: String,
    val perfil: Perfil,
)
