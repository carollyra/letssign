package br.com.letssign.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE_BR = Locale.forLanguageTag("pt-BR")
private val FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", LOCALE_BR)

fun formatarDataHora(instante: Long, fuso: ZoneId = ZoneId.systemDefault()): String =
    FORMATO_DATA_HORA.format(Instant.ofEpochMilli(instante).atZone(fuso))

fun formatarTelefone(digitos: String): String = when (digitos.length) {
    11 -> "(${digitos.substring(0, 2)}) ${digitos.substring(2, 7)}-${digitos.substring(7)}"
    10 -> "(${digitos.substring(0, 2)}) ${digitos.substring(2, 6)}-${digitos.substring(6)}"
    else -> digitos
}
