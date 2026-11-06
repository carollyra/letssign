package br.com.letssign.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Ícones de linha do app, todos na mesma grade de 24 e com a mesma espessura de traço,
 * para não misturar estilos diferentes na interface.
 */
object Icones {
    val Voltar: ImageVector by lazy {
        icone("Voltar") { traco { poligonal(14.5f, 5f, 7.5f, 12f, 14.5f, 19f) } }
    }
    val Avancar: ImageVector by lazy {
        icone("Avancar") { traco { poligonal(9.5f, 5f, 16.5f, 12f, 9.5f, 19f) } }
    }
    val Fechar: ImageVector by lazy {
        icone("Fechar") {
            traco {
                linha(6f, 6f, 18f, 18f)
                linha(18f, 6f, 6f, 18f)
            }
        }
    }
    val Mais: ImageVector by lazy {
        icone("Mais") {
            traco {
                linha(12f, 5f, 12f, 19f)
                linha(5f, 12f, 19f, 12f)
            }
        }
    }
    val Check: ImageVector by lazy {
        icone("Check") { traco { poligonal(5f, 12.5f, 10f, 17.5f, 19f, 7.5f) } }
    }
    val CheckCirculo: ImageVector by lazy {
        icone("CheckCirculo") {
            traco {
                circulo(12f, 12f, 8.5f)
                poligonal(8.2f, 12.3f, 10.9f, 15f, 15.8f, 9.5f)
            }
        }
    }
    val Pessoa: ImageVector by lazy {
        icone("Pessoa") {
            traco {
                circulo(12f, 8f, 3.6f)
                moveTo(4.8f, 20f)
                arcTo(7.2f, 6.2f, 0f, false, true, 19.2f, 20f)
            }
        }
    }
    val Pessoas: ImageVector by lazy {
        icone("Pessoas") {
            traco {
                circulo(9f, 8.5f, 3.2f)
                moveTo(2.8f, 19.5f)
                arcTo(6.2f, 5.6f, 0f, false, true, 15.2f, 19.5f)
                moveTo(15.6f, 5.5f)
                arcTo(3.2f, 3.2f, 0f, false, true, 15.6f, 11.5f)
                moveTo(17.8f, 14.3f)
                arcTo(5.6f, 5.6f, 0f, false, true, 21.2f, 19.5f)
            }
        }
    }
    val Sair: ImageVector by lazy {
        icone("Sair") {
            traco {
                moveTo(10f, 4.5f)
                lineTo(7f, 4.5f)
                arcTo(2.5f, 2.5f, 0f, false, false, 4.5f, 7f)
                lineTo(4.5f, 17f)
                arcTo(2.5f, 2.5f, 0f, false, false, 7f, 19.5f)
                lineTo(10f, 19.5f)
                linha(10.5f, 12f, 20f, 12f)
                poligonal(16.5f, 8.5f, 20f, 12f, 16.5f, 15.5f)
            }
        }
    }
    val Documento: ImageVector by lazy {
        icone("Documento") {
            traco {
                folha()
                linha(9f, 12.5f, 15f, 12.5f)
                linha(9f, 16.5f, 13f, 16.5f)
            }
        }
    }
    val DocumentoNovo: ImageVector by lazy {
        icone("DocumentoNovo") {
            traco {
                folha()
                linha(12f, 11.5f, 12f, 17.5f)
                linha(9f, 14.5f, 15f, 14.5f)
            }
        }
    }
    val Editar: ImageVector by lazy {
        icone("Editar") {
            traco {
                moveTo(15.5f, 4.5f)
                lineTo(19.5f, 8.5f)
                lineTo(8.5f, 19.5f)
                lineTo(4f, 20f)
                lineTo(4.5f, 15.5f)
                close()
                linha(13f, 7f, 17f, 11f)
            }
        }
    }
    val Lixeira: ImageVector by lazy {
        icone("Lixeira") {
            traco {
                linha(4.5f, 7f, 19.5f, 7f)
                moveTo(6.5f, 7f)
                lineTo(7.3f, 18.7f)
                arcTo(2f, 2f, 0f, false, false, 9.3f, 20.5f)
                lineTo(14.7f, 20.5f)
                arcTo(2f, 2f, 0f, false, false, 16.7f, 18.7f)
                lineTo(17.5f, 7f)
                moveTo(9.5f, 7f)
                lineTo(9.5f, 5f)
                arcTo(1.5f, 1.5f, 0f, false, true, 11f, 3.5f)
                lineTo(13f, 3.5f)
                arcTo(1.5f, 1.5f, 0f, false, true, 14.5f, 5f)
                lineTo(14.5f, 7f)
                linha(10.3f, 11f, 10.3f, 16.5f)
                linha(13.7f, 11f, 13.7f, 16.5f)
            }
        }
    }
    val Olho: ImageVector by lazy {
        icone("Olho") { traco { olho() } }
    }
    val OlhoFechado: ImageVector by lazy {
        icone("OlhoFechado") {
            traco {
                olho()
                linha(4.5f, 4.5f, 19.5f, 19.5f)
            }
        }
    }
    val Enviar: ImageVector by lazy {
        icone("Enviar") {
            traco {
                moveTo(20.5f, 3.5f)
                lineTo(3.5f, 10.2f)
                lineTo(10.2f, 13.8f)
                lineTo(13.8f, 20.5f)
                close()
                linha(20.5f, 3.5f, 10.2f, 13.8f)
            }
        }
    }
    val Relogio: ImageVector by lazy {
        icone("Relogio") {
            traco {
                circulo(12f, 12f, 8.5f)
                poligonal(12f, 7.5f, 12f, 12f, 15f, 13.8f)
            }
        }
    }
    val Alerta: ImageVector by lazy {
        icone("Alerta") {
            traco {
                circulo(12f, 12f, 8.5f)
                linha(12f, 8f, 12f, 12.8f)
                linha(12f, 16f, 12f, 16.01f)
            }
        }
    }
    val Assinar: ImageVector by lazy {
        icone("Assinar") {
            traco {
                moveTo(15.8f, 3.8f)
                lineTo(19.2f, 7.2f)
                lineTo(9f, 17.4f)
                lineTo(5f, 18f)
                lineTo(5.6f, 14f)
                close()
                linha(13f, 20.5f, 20f, 20.5f)
            }
        }
    }
    val Copia: ImageVector by lazy {
        icone("Copia") {
            traco {
                retangulo(8.5f, 8.5f, 12f, 12f, 2.5f)
                moveTo(15.5f, 5.5f)
                arcTo(2.5f, 2.5f, 0f, false, false, 13f, 3f)
                lineTo(5.5f, 3f)
                arcTo(2.5f, 2.5f, 0f, false, false, 3f, 5.5f)
                lineTo(3f, 13f)
                arcTo(2.5f, 2.5f, 0f, false, false, 5.5f, 15.5f)
            }
        }
    }
    val Bandeja: ImageVector by lazy {
        icone("Bandeja") {
            traco {
                retangulo(3.5f, 4.5f, 17f, 15f, 3f)
                poligonal(3.5f, 13f, 8.3f, 13f, 9.8f, 15.5f, 14.2f, 15.5f, 15.7f, 13f, 20.5f, 13f)
            }
        }
    }
    val Lupa: ImageVector by lazy {
        icone("Lupa") {
            traco {
                circulo(10.5f, 10.5f, 6.5f)
                linha(15.4f, 15.4f, 20f, 20f)
            }
        }
    }
}

private const val ESPESSURA_DO_TRACO = 1.8f

private fun icone(nome: String, desenho: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = nome,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(desenho).build()

private fun ImageVector.Builder.traco(caminho: PathBuilder.() -> Unit) {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = ESPESSURA_DO_TRACO,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = caminho,
    )
}

private fun PathBuilder.linha(x1: Float, y1: Float, x2: Float, y2: Float) {
    moveTo(x1, y1)
    lineTo(x2, y2)
}

/** Pontos em pares x, y ligados em sequência, sem fechar. */
private fun PathBuilder.poligonal(vararg pontos: Float) {
    moveTo(pontos[0], pontos[1])
    for (i in 2 until pontos.size step 2) lineTo(pontos[i], pontos[i + 1])
}

private fun PathBuilder.circulo(cx: Float, cy: Float, raio: Float) {
    moveTo(cx - raio, cy)
    arcTo(raio, raio, 0f, false, true, cx + raio, cy)
    arcTo(raio, raio, 0f, false, true, cx - raio, cy)
    close()
}

private fun PathBuilder.retangulo(x: Float, y: Float, largura: Float, altura: Float, raio: Float) {
    moveTo(x + raio, y)
    lineTo(x + largura - raio, y)
    arcTo(raio, raio, 0f, false, true, x + largura, y + raio)
    lineTo(x + largura, y + altura - raio)
    arcTo(raio, raio, 0f, false, true, x + largura - raio, y + altura)
    lineTo(x + raio, y + altura)
    arcTo(raio, raio, 0f, false, true, x, y + altura - raio)
    lineTo(x, y + raio)
    arcTo(raio, raio, 0f, false, true, x + raio, y)
    close()
}

/** Folha com o canto superior direito dobrado. */
private fun PathBuilder.folha() {
    moveTo(13.5f, 3f)
    lineTo(8f, 3f)
    arcTo(2.5f, 2.5f, 0f, false, false, 5.5f, 5.5f)
    lineTo(5.5f, 18.5f)
    arcTo(2.5f, 2.5f, 0f, false, false, 8f, 21f)
    lineTo(16f, 21f)
    arcTo(2.5f, 2.5f, 0f, false, false, 18.5f, 18.5f)
    lineTo(18.5f, 8f)
    close()
    poligonal(13.5f, 3.5f, 13.5f, 8f, 18f, 8f)
}

private fun PathBuilder.olho() {
    moveTo(2.8f, 12f)
    curveTo(5.8f, 6.6f, 18.2f, 6.6f, 21.2f, 12f)
    curveTo(18.2f, 17.4f, 5.8f, 17.4f, 2.8f, 12f)
    close()
    circulo(12f, 12f, 2.8f)
}
