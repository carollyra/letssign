package br.com.letssign.ui.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

sealed interface EstadoLista<out T> {
    data object Carregando : EstadoLista<Nothing>
    data object Erro : EstadoLista<Nothing>
    data class Dados<T>(val itens: List<T>) : EstadoLista<T>
}

fun <T> Flow<List<T>>.comoEstadoLista(): Flow<EstadoLista<T>> =
    map<List<T>, EstadoLista<T>> { EstadoLista.Dados(it) }
        .catch { emit(EstadoLista.Erro) }
