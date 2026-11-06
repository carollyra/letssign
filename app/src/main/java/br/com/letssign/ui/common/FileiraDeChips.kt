package br.com.letssign.ui.common

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Fileira de chips que rola na horizontal. O chip de [indiceSelecionado] é trazido para dentro
 * da área visível sempre que a seleção muda.
 */
@Composable
fun FileiraDeChips(
    quantidade: Int,
    indiceSelecionado: Int,
    modifier: Modifier = Modifier,
    margens: PaddingValues = PaddingValues(horizontal = 16.dp),
    chip: @Composable (indice: Int) -> Unit,
) {
    val estado = rememberLazyListState()

    LaunchedEffect(indiceSelecionado, quantidade) {
        if (indiceSelecionado in 0 until quantidade) estado.revelar(indiceSelecionado)
    }

    LazyRow(
        state = estado,
        modifier = modifier.fillMaxWidth(),
        contentPadding = margens,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(count = quantidade, key = { it }) { indice -> chip(indice) }
    }
}

private suspend fun LazyListState.revelar(indice: Int) {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == indice }
    if (item == null) {
        animateScrollToItem(indice)
        return
    }
    // Os deslocamentos dos itens são contados a partir do fim da margem inicial.
    val fimVisivel = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
    val sobra = item.offset + item.size - fimVisivel
    when {
        item.offset < 0 -> animateScrollBy(item.offset.toFloat())
        sobra > 0 -> animateScrollBy(sobra.toFloat())
    }
}
