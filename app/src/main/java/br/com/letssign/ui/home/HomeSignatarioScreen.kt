package br.com.letssign.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import br.com.letssign.R
import br.com.letssign.domain.model.Usuario
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.EstadoVazio

@Composable
fun HomeSignatarioScreen(usuario: Usuario, onSair: () -> Unit) {
    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = stringResource(R.string.home_signatario_titulo),
                acoes = {
                    TextButton(onClick = onSair) { Text(stringResource(R.string.acao_sair)) }
                },
            )
        },
    ) { margens ->
        EstadoVazio(
            titulo = stringResource(R.string.home_saudacao, usuario.nome),
            descricao = stringResource(R.string.home_signatario_vazio),
            modifier = Modifier.padding(margens),
        )
    }
}
