package br.com.letssign.domain.regra

object ValidacaoCampos {

    const val TAMANHO_MINIMO_SENHA = 6

    private val FORMATO_EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$")

    fun normalizarEmail(email: String): String = email.trim().lowercase()

    fun emailValido(email: String): Boolean = FORMATO_EMAIL.matches(email.trim())

    /** Nome e sobrenome, cada um com pelo menos duas letras. */
    fun nomeCompletoValido(nome: String): Boolean {
        val partes = nome.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return partes.size >= 2 && partes.all { parte -> parte.length >= 2 && parte.any(Char::isLetter) }
    }

    fun apenasDigitos(telefone: String): String = telefone.filter(Char::isDigit)

    /** DDD + número: 10 dígitos para fixo, 11 para celular. */
    fun telefoneValido(telefone: String): Boolean {
        val digitos = apenasDigitos(telefone)
        return digitos.length in 10..11 && digitos.first() != '0'
    }

    fun senhaValida(senha: String): Boolean =
        senha.length >= TAMANHO_MINIMO_SENHA && senha.none(Char::isWhitespace)
}
