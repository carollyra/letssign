package br.com.letssign.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF2B5BE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EEFF),
    onPrimaryContainer = Color(0xFF16357F),
    inversePrimary = Color(0xFF9DB8FF),
    secondary = Color(0xFF5A6272),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDEFF3),
    onSecondaryContainer = Color(0xFF1B1F27),
    tertiary = Color(0xFF1B7F53),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F5E9),
    onTertiaryContainer = Color(0xFF0D4B30),
    error = Color(0xFFC9372C),
    onError = Color.White,
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = Color(0xFF8A1F16),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF14171C),
    surface = Color.White,
    onSurface = Color(0xFF14171C),
    surfaceVariant = Color(0xFFEDEFF3),
    onSurfaceVariant = Color(0xFF687080),
    // Sem tinta de elevação: superfícies elevadas continuam brancas em vez de puxar para o lilás.
    surfaceTint = Color.White,
    inverseSurface = Color(0xFF1D2026),
    inverseOnSurface = Color(0xFFF1F2F4),
    outline = Color(0xFFC9CED6),
    outlineVariant = Color(0xFFE4E7EC),
    scrim = Color(0xFF0B0D12),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE9EBEF),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFEDEFF3),
)

internal val EsquemaEscuro = darkColorScheme(
    primary = Color(0xFF8FB1FF),
    onPrimary = Color(0xFF0A1F52),
    primaryContainer = Color(0xFF1B2B52),
    onPrimaryContainer = Color(0xFFD5E1FF),
    inversePrimary = Color(0xFF2B5BE0),
    secondary = Color(0xFFB4BAC5),
    onSecondary = Color(0xFF1B1F27),
    secondaryContainer = Color(0xFF272A32),
    onSecondaryContainer = Color(0xFFE4E7EC),
    tertiary = Color(0xFF7DD6A5),
    onTertiary = Color(0xFF00391F),
    tertiaryContainer = Color(0xFF14321F),
    onTertiaryContainer = Color(0xFFBFF0D4),
    error = Color(0xFFFF8A7D),
    onError = Color(0xFF4A0B05),
    errorContainer = Color(0xFF3B1613),
    onErrorContainer = Color(0xFFFFD0CA),
    background = Color(0xFF0D0E11),
    onBackground = Color(0xFFF1F2F4),
    surface = Color(0xFF17191E),
    onSurface = Color(0xFFF1F2F4),
    surfaceVariant = Color(0xFF24272E),
    onSurfaceVariant = Color(0xFF9CA3AF),
    surfaceTint = Color(0xFF17191E),
    inverseSurface = Color(0xFFF1F2F4),
    inverseOnSurface = Color(0xFF1D2026),
    outline = Color(0xFF444954),
    outlineVariant = Color(0xFF2A2D35),
    scrim = Color.Black,
    surfaceBright = Color(0xFF3A3E48),
    surfaceDim = Color(0xFF0D0E11),
    surfaceContainerLowest = Color(0xFF0D0E11),
    surfaceContainerLow = Color(0xFF17191E),
    surfaceContainer = Color(0xFF1D2026),
    surfaceContainerHigh = Color(0xFF1F2229),
    surfaceContainerHighest = Color(0xFF272A32),
)

/** Par de cores de uma situação: fundo suave e conteúdo (texto, ponto ou ícone) por cima dele. */
@Immutable
data class TomDeStatus(val fundo: Color, val conteudo: Color)

/** Cores do app que o esquema do Material não cobre. */
@Immutable
data class CoresLetsSign(
    val rascunho: TomDeStatus,
    val enviado: TomDeStatus,
    val parcial: TomDeStatus,
    val concluido: TomDeStatus,
    val pendente: TomDeStatus,
    val sombra: Color,
    val bordaDoCartao: Color,
)

internal val CoresClaras = CoresLetsSign(
    rascunho = TomDeStatus(Color(0xFFEDEFF3), Color(0xFF596070)),
    enviado = TomDeStatus(Color(0xFFE8EEFF), Color(0xFF2450CC)),
    parcial = TomDeStatus(Color(0xFFFFF2D9), Color(0xFF955800)),
    concluido = TomDeStatus(Color(0xFFE0F5E9), Color(0xFF18764B)),
    pendente = TomDeStatus(Color(0xFFFFF2D9), Color(0xFF955800)),
    sombra = Color(0x1F1B2540),
    bordaDoCartao = Color.Transparent,
)

internal val CoresEscuras = CoresLetsSign(
    rascunho = TomDeStatus(Color(0xFF272A32), Color(0xFFB4BAC5)),
    enviado = TomDeStatus(Color(0xFF1B2B52), Color(0xFFA6C0FF)),
    parcial = TomDeStatus(Color(0xFF3A2C10), Color(0xFFF2C56E)),
    concluido = TomDeStatus(Color(0xFF14321F), Color(0xFF86DAAB)),
    pendente = TomDeStatus(Color(0xFF3A2C10), Color(0xFFF2C56E)),
    sombra = Color.Transparent,
    bordaDoCartao = Color(0xFF24272E),
)

internal val LocalCoresLetsSign = staticCompositionLocalOf { CoresClaras }
