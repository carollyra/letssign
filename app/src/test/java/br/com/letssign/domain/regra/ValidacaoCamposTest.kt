package br.com.letssign.domain.regra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoCamposTest {

    @Test
    fun `aceita emails comuns`() {
        assertTrue(ValidacaoCampos.emailValido("ana.souza@empresa.com.br"))
        assertTrue(ValidacaoCampos.emailValido(" ana+contratos@empresa.com "))
    }

    @Test
    fun `recusa emails malformados`() {
        listOf("", "ana", "ana@", "@empresa.com", "ana@empresa", "ana souza@empresa.com").forEach {
            assertFalse("deveria recusar '$it'", ValidacaoCampos.emailValido(it))
        }
    }

    @Test
    fun `normaliza email para comparar sem diferenca de caixa`() {
        assertEquals("ana@empresa.com", ValidacaoCampos.normalizarEmail("  Ana@Empresa.COM "))
    }

    @Test
    fun `nome completo exige nome e sobrenome`() {
        assertTrue(ValidacaoCampos.nomeCompletoValido("Ana Souza"))
        assertTrue(ValidacaoCampos.nomeCompletoValido("  João   da Silva "))
        assertFalse(ValidacaoCampos.nomeCompletoValido("Ana"))
        assertFalse(ValidacaoCampos.nomeCompletoValido("Ana S"))
        assertFalse(ValidacaoCampos.nomeCompletoValido("   "))
    }

    @Test
    fun `telefone aceita fixo e celular com DDD, com ou sem mascara`() {
        assertTrue(ValidacaoCampos.telefoneValido("(11) 99999-0000"))
        assertTrue(ValidacaoCampos.telefoneValido("1133334444"))
        assertFalse(ValidacaoCampos.telefoneValido("99999-0000"))
        assertFalse(ValidacaoCampos.telefoneValido("011999990000"))
        assertFalse(ValidacaoCampos.telefoneValido(""))
    }

    @Test
    fun `senha exige tamanho minimo e nao aceita espacos`() {
        assertTrue(ValidacaoCampos.senhaValida("abc123"))
        assertFalse(ValidacaoCampos.senhaValida("abc12"))
        assertFalse(ValidacaoCampos.senhaValida("abc 123"))
    }
}
