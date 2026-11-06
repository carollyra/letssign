package br.com.letssign.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun estilo(tamanho: Int, linha: Int, peso: FontWeight, espacamento: Double = 0.0) = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = tamanho.sp,
    lineHeight = linha.sp,
    fontWeight = peso,
    letterSpacing = espacamento.sp,
)

internal val TipografiaLetsSign = Typography(
    displayLarge = estilo(44, 50, FontWeight.Bold, -0.6),
    displayMedium = estilo(38, 44, FontWeight.Bold, -0.5),
    displaySmall = estilo(34, 40, FontWeight.Bold, -0.5),
    headlineLarge = estilo(30, 36, FontWeight.Bold, -0.4),
    headlineMedium = estilo(26, 32, FontWeight.Bold, -0.3),
    headlineSmall = estilo(22, 28, FontWeight.SemiBold, -0.2),
    titleLarge = estilo(20, 26, FontWeight.SemiBold, -0.2),
    titleMedium = estilo(17, 23, FontWeight.SemiBold, -0.1),
    titleSmall = estilo(15, 20, FontWeight.SemiBold),
    bodyLarge = estilo(16, 24, FontWeight.Normal),
    bodyMedium = estilo(15, 21, FontWeight.Normal),
    bodySmall = estilo(13, 18, FontWeight.Normal),
    labelLarge = estilo(16, 20, FontWeight.SemiBold),
    labelMedium = estilo(13, 16, FontWeight.Medium),
    labelSmall = estilo(12, 16, FontWeight.Medium),
)
