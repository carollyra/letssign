package br.com.letssign.ui.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.ui.common.AvisoDeErro
import br.com.letssign.ui.common.BotaoPrincipal
import br.com.letssign.ui.common.CampoTexto
import br.com.letssign.ui.common.Icones
import br.com.letssign.ui.theme.Espacos
import br.com.letssign.ui.theme.Movimento

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold { margens ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(margens)
                .consumeWindowInsets(margens)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacos.xl, vertical = Espacos.xxl),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Espacos.xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icones.Assinar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.height(Espacos.m))
                Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
                Text(
                    text = stringResource(R.string.login_subtitulo),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Espacos.xl))

                CampoTexto(
                    valor = estado.email,
                    aoMudar = viewModel::aoMudarEmail,
                    rotulo = stringResource(R.string.campo_email),
                    erro = estado.erroEmail?.let { stringResource(it) },
                    opcoesDoTeclado = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )
                CampoTexto(
                    valor = estado.senha,
                    aoMudar = viewModel::aoMudarSenha,
                    rotulo = stringResource(R.string.campo_senha),
                    erro = estado.erroSenha?.let { stringResource(it) },
                    senha = true,
                    opcoesDoTeclado = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    acoesDoTeclado = KeyboardActions(onDone = { viewModel.aoEntrar() }),
                )

                AnimatedContent(
                    targetState = estado.erroGeral,
                    transitionSpec = { fadeIn(Movimento.medio()) togetherWith fadeOut(Movimento.rapido()) },
                    label = "erroDoLogin",
                ) { erro ->
                    if (erro != null) AvisoDeErro(texto = stringResource(erro))
                }

                Spacer(Modifier.height(Espacos.s))
                BotaoPrincipal(
                    texto = stringResource(R.string.login_entrar),
                    onClick = viewModel::aoEntrar,
                    carregando = estado.carregando,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
