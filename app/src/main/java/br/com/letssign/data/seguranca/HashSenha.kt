package br.com.letssign.data.seguranca

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject

data class SenhaProtegida(val hash: String, val salt: String)

class HashSenha @Inject constructor() {

    private val aleatorio = SecureRandom()

    fun proteger(senha: String): SenhaProtegida {
        val salt = ByteArray(TAMANHO_SALT).also(aleatorio::nextBytes)
        return SenhaProtegida(
            hash = Base64.getEncoder().encodeToString(derivar(senha, salt)),
            salt = Base64.getEncoder().encodeToString(salt),
        )
    }

    fun confere(senha: String, protegida: SenhaProtegida): Boolean {
        val esperado = Base64.getDecoder().decode(protegida.hash)
        val calculado = derivar(senha, Base64.getDecoder().decode(protegida.salt))
        // Comparação em tempo constante, para o tempo de resposta não revelar quantos bytes batem.
        return MessageDigest.isEqual(esperado, calculado)
    }

    private fun derivar(senha: String, salt: ByteArray): ByteArray {
        val especificacao = PBEKeySpec(senha.toCharArray(), salt, ITERACOES, TAMANHO_CHAVE_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacao).encoded
        } finally {
            especificacao.clearPassword()
        }
    }

    private companion object {
        const val ALGORITMO = "PBKDF2WithHmacSHA256"
        const val ITERACOES = 150_000
        const val TAMANHO_CHAVE_BITS = 256
        const val TAMANHO_SALT = 16
    }
}
