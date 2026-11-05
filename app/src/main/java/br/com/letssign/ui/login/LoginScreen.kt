package br.com.letssign.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var senhaVisivel by rememberSaveable { mutableStateOf(false) }

    Scaffold { margens ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(margens)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.login_subtitulo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = estado.email,
                onValueChange = viewModel::aoMudarEmail,
                label = { Text(stringResource(R.string.campo_email)) },
                singleLine = true,
                isError = estado.erroEmail != null,
                supportingText = estado.erroEmail?.let { { Text(stringResource(it)) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = estado.senha,
                onValueChange = viewModel::aoMudarSenha,
                label = { Text(stringResource(R.string.campo_senha)) },
                singleLine = true,
                isError = estado.erroSenha != null,
                supportingText = estado.erroSenha?.let { { Text(stringResource(it)) } },
                visualTransformation = if (senhaVisivel) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(onClick = { senhaVisivel = !senhaVisivel }) {
                        Text(
                            stringResource(
                                if (senhaVisivel) R.string.acao_ocultar else R.string.acao_mostrar,
                            ),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.aoEntrar() }),
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
            )

            estado.erroGeral?.let { erro ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(erro),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::aoEntrar,
                enabled = !estado.carregando,
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
            ) {
                if (estado.carregando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.login_entrar))
                }
            }
        }
    }
}
