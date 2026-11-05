package br.com.letssign.ui.signatarios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.regra.ValidacaoCampos
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.EstadoErro

@Composable
fun SignatarioFormScreen(
    onVoltar: () -> Unit,
    viewModel: SignatarioFormViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.salvo) {
        if (estado.salvo) onVoltar()
    }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = stringResource(
                    if (estado.edicao) R.string.signatario_editar_titulo else R.string.signatario_novo_titulo,
                ),
                onVoltar = onVoltar,
            )
        },
    ) { margens ->
        when {
            estado.carregando -> Carregando(Modifier.padding(margens))
            estado.erroCarregar -> EstadoErro(
                mensagem = stringResource(R.string.signatario_erro_nao_encontrado),
                modifier = Modifier.padding(margens),
                onTentarNovamente = viewModel::carregar,
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(margens)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = estado.nome,
                    onValueChange = viewModel::aoMudarNome,
                    label = { Text(stringResource(R.string.campo_nome_completo)) },
                    singleLine = true,
                    isError = estado.erroNome != null,
                    supportingText = estado.erroNome?.let { { Text(stringResource(it)) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = estado.email,
                    onValueChange = viewModel::aoMudarEmail,
                    label = { Text(stringResource(R.string.campo_email)) },
                    singleLine = true,
                    isError = estado.erroEmail != null,
                    supportingText = {
                        Text(stringResource(estado.erroEmail ?: R.string.signatario_dica_email))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = estado.telefone,
                    onValueChange = viewModel::aoMudarTelefone,
                    label = { Text(stringResource(R.string.campo_telefone)) },
                    singleLine = true,
                    isError = estado.erroTelefone != null,
                    supportingText = {
                        Text(stringResource(estado.erroTelefone ?: R.string.signatario_dica_telefone))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = estado.senha,
                    onValueChange = viewModel::aoMudarSenha,
                    label = {
                        Text(
                            stringResource(
                                if (estado.edicao) R.string.campo_nova_senha else R.string.campo_senha,
                            ),
                        )
                    },
                    singleLine = true,
                    isError = estado.erroSenha != null,
                    supportingText = {
                        Text(
                            if (estado.erroSenha != null) {
                                stringResource(estado.erroSenha!!, ValidacaoCampos.TAMANHO_MINIMO_SENHA)
                            } else if (estado.edicao) {
                                stringResource(R.string.signatario_dica_senha_edicao)
                            } else {
                                stringResource(
                                    R.string.signatario_dica_senha,
                                    ValidacaoCampos.TAMANHO_MINIMO_SENHA,
                                )
                            },
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                estado.erroGeral?.let { erro ->
                    Text(
                        text = stringResource(erro),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Button(
                    onClick = viewModel::salvar,
                    enabled = !estado.salvando,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (estado.salvando) R.string.acao_salvando else R.string.acao_salvar,
                        ),
                    )
                }
            }
        }
    }
}
