package br.com.letssign.data.seguranca

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HashSenhaTest {

    private val hashSenha = HashSenha()

    @Test
    fun `senha correta confere`() {
        val protegida = hashSenha.proteger("Segredo#1")

        assertTrue(hashSenha.confere("Segredo#1", protegida))
    }

    @Test
    fun `senha errada nao confere`() {
        val protegida = hashSenha.proteger("Segredo#1")

        assertFalse(hashSenha.confere("segredo#1", protegida))
    }

    @Test
    fun `mesma senha gera hash diferente a cada vez por causa do salt`() {
        val primeira = hashSenha.proteger("Segredo#1")
        val segunda = hashSenha.proteger("Segredo#1")

        assertNotEquals(primeira.salt, segunda.salt)
        assertNotEquals(primeira.hash, segunda.hash)
    }

    @Test
    fun `hash nao contem a senha`() {
        val protegida = hashSenha.proteger("Segredo#1")

        assertFalse(protegida.hash.contains("Segredo#1"))
    }
}
