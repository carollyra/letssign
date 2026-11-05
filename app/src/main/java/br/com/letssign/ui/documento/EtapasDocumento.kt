package br.com.letssign.ui.documento

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.letssign.R

enum class EtapaDocumento(@StringRes val titulo: Int) {
    ARQUIVO(R.string.etapa_arquivo),
    SIGNATARIOS(R.string.etapa_signatarios),
    CAMPOS(R.string.etapa_campos),
    REVISAO(R.string.etapa_revisao),
}

@Composable
fun IndicadorEtapa(etapa: EtapaDocumento, modifier: Modifier = Modifier) {
    val total = EtapaDocumento.entries.size
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.etapa_posicao, etapa.ordinal + 1, total, stringResource(etapa.titulo)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (etapa.ordinal + 1) / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
