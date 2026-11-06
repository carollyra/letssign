package br.com.letssign.ui.signatarios

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.letssign.R
import br.com.letssign.domain.regra.ValidacaoCampos
import br.com.letssign.ui.common.AvisoDeErro
import br.com.letssign.ui.common.BarraSuperior
import br.com.letssign.ui.common.BotaoPrincipal
import br.com.letssign.ui.common.CampoTexto
import br.com.letssign.ui.common.Carregando
import br.com.letssign.ui.common.EstadoErro
import br.com.letssign.ui.theme.Espacos
import br.com.letssign.ui.theme.Movimento

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
                    .consumeWindowInsets(margens)
                    .imePadding(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Espacos.tela, vertical = Espacos.xs),
                    verticalArrangement = Arrangement.spacedBy(Espacos.xs),
                ) {
                    CampoTexto(
                        valor = estado.nome,
                        aoMudar = viewModel::aoMudarNome,
                        rotulo = stringResource(R.string.campo_nome_completo),
                        erro = estado.erroNome?.let { stringResource(it) },
                        opcoesDoTeclado = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    CampoTexto(
                        valor = estado.email,
                        aoMudar = viewModel::aoMudarEmail,
                        rotulo = stringResource(R.string.campo_email),
                        erro = estado.erroEmail?.let { stringResource(it) },
                        dica = stringResource(R.string.signatario_dica_email),
                        opcoesDoTeclado = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    CampoTexto(
                        valor = estado.telefone,
                        aoMudar = viewModel::aoMudarTelefone,
                        rotulo = stringResource(R.string.campo_telefone),
                        erro = estado.erroTelefone?.let { stringResource(it) },
                        dica = stringResource(R.string.signatario_dica_telefone),
                        opcoesDoTeclado = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    CampoTexto(
                        valor = estado.senha,
                        aoMudar = viewModel::aoMudarSenha,
                        rotulo = stringResource(
                            if (estado.edicao) R.string.campo_nova_senha else R.string.campo_senha,
                        ),
                        erro = estado.erroSenha?.let {
                            stringResource(it, ValidacaoCampos.TAMANHO_MINIMO_SENHA)
                        },
                        dica = if (estado.edicao) {
                            stringResource(R.string.signatario_dica_senha_edicao)
                        } else {
                            stringResource(R.string.signatario_dica_senha, ValidacaoCampos.TAMANHO_MINIMO_SENHA)
                        },
                        senha = true,
                        opcoesDoTeclado = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                    )

                    AnimatedContent(
                        targetState = estado.erroGeral,
                        transitionSpec = { fadeIn(Movimento.medio()) togetherWith fadeOut(Movimento.rapido()) },
                        label = "erroDoFormulario",
                    ) { erro ->
                        if (erro != null) AvisoDeErro(texto = stringResource(erro))
                    }
                }
                BotaoPrincipal(
                    texto = stringResource(if (estado.salvando) R.string.acao_salvando else R.string.acao_salvar),
                    onClick = viewModel::salvar,
                    carregando = estado.salvando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Espacos.tela, end = Espacos.tela, top = Espacos.s, bottom = Espacos.m),
                )
            }
        }
    }
}
