package br.com.letssign.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import br.com.letssign.ui.theme.Movimento

private typealias Entrada = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
private typealias Saida = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

/**
 * Transições entre telas: deslize curto com fade. Quem avança entra pela direita e, ao voltar,
 * sai por onde entrou.
 */
internal object Transicoes {
    val entrar: Entrada = {
        slideInHorizontally(Movimento.lento()) { it / 5 } + fadeIn(tween(Movimento.MEDIO))
    }
    val sair: Saida = {
        slideOutHorizontally(Movimento.lento()) { -it / 8 } + fadeOut(tween(Movimento.RAPIDO))
    }
    val voltarEntrar: Entrada = {
        slideInHorizontally(Movimento.lento()) { -it / 8 } + fadeIn(tween(Movimento.MEDIO))
    }
    val voltarSair: Saida = {
        slideOutHorizontally(Movimento.lento()) { it / 5 } + fadeOut(tween(Movimento.RAPIDO))
    }

    /** Entre as etapas de um mesmo fluxo o conteúdo atravessa a tela inteira. */
    val etapaEntrar: Entrada = {
        slideInHorizontally(Movimento.lento()) { it } + fadeIn(tween(Movimento.MEDIO))
    }
    val etapaSair: Saida = {
        slideOutHorizontally(Movimento.lento()) { -it } + fadeOut(tween(Movimento.MEDIO))
    }
    val etapaVoltarEntrar: Entrada = {
        slideInHorizontally(Movimento.lento()) { -it } + fadeIn(tween(Movimento.MEDIO))
    }
    val etapaVoltarSair: Saida = {
        slideOutHorizontally(Movimento.lento()) { it } + fadeOut(tween(Movimento.MEDIO))
    }
}
