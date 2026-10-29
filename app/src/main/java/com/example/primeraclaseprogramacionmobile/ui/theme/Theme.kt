package com.example.primeraclaseprogramacionmobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EsquemaOscuro = darkColorScheme(
    primary = ColoresRecordatorios.Primario,
    onPrimary = ColoresRecordatorios.Fondo,
    secondary = ColoresRecordatorios.Secundario,
    onSecondary = ColoresRecordatorios.Texto,
    background = ColoresRecordatorios.Fondo,
    onBackground = ColoresRecordatorios.Texto,
    surface = ColoresRecordatorios.Tarjeta,
    onSurface = ColoresRecordatorios.Texto,
    surfaceVariant = ColoresRecordatorios.Secundario,
    onSurfaceVariant = ColoresRecordatorios.TextoSecundario,
    outline = ColoresRecordatorios.Borde,
    error = ColoresRecordatorios.Error,
    onError = ColoresRecordatorios.Texto,
)

@Composable
fun PrimeraClaseProgramacionMobileTheme(content: @Composable () -> Unit) {
    // La paleta es fija: los colores dinámicos del dispositivo romperían los roles de AxForge.
    MaterialTheme(
        colorScheme = EsquemaOscuro,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(8.dp),
            extraLarge = RoundedCornerShape(12.dp),
        ),
        content = content,
    )
}
