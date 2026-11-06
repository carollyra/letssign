package br.com.letssign.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import br.com.letssign.R

/**
 * Campo de formulário do app. [erro] toma o lugar de [dica] abaixo do campo; com [senha] o texto
 * fica oculto e ganha o botão de mostrar.
 */
@Composable
fun CampoTexto(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    erro: String? = null,
    dica: String? = null,
    marcarErro: Boolean = erro != null,
    senha: Boolean = false,
    habilitado: Boolean = true,
    umaLinha: Boolean = true,
    minimoDeLinhas: Int = 1,
    maximoDeLinhas: Int = if (umaLinha) 1 else Int.MAX_VALUE,
    opcoesDoTeclado: KeyboardOptions = KeyboardOptions.Default,
    acoesDoTeclado: KeyboardActions = KeyboardActions.Default,
) {
    var senhaVisivel by rememberSaveable { mutableStateOf(false) }
    val cores = MaterialTheme.colorScheme
    val apoio = erro ?: dica

    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        enabled = habilitado,
        singleLine = umaLinha,
        minLines = minimoDeLinhas,
        maxLines = maximoDeLinhas,
        isError = marcarErro,
        supportingText = apoio?.let { { Text(it) } },
        visualTransformation = if (senha && !senhaVisivel) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = if (senha) {
            {
                BotaoIcone(
                    icone = if (senhaVisivel) Icones.OlhoFechado else Icones.Olho,
                    descricao = stringResource(if (senhaVisivel) R.string.acao_ocultar else R.string.acao_mostrar),
                    onClick = { senhaVisivel = !senhaVisivel },
                    cor = cores.onSurfaceVariant,
                )
            }
        } else {
            null
        },
        keyboardOptions = opcoesDoTeclado,
        keyboardActions = acoesDoTeclado,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = cores.surface,
            unfocusedContainerColor = cores.surface,
            disabledContainerColor = cores.surface,
            errorContainerColor = cores.surface,
            unfocusedBorderColor = cores.outlineVariant,
            disabledBorderColor = cores.outlineVariant,
            unfocusedLabelColor = cores.onSurfaceVariant,
            unfocusedSupportingTextColor = cores.onSurfaceVariant,
            focusedSupportingTextColor = cores.onSurfaceVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
