package com.example.primeraclaseprogramacionmobile.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.primeraclaseprogramacionmobile.R
import com.example.primeraclaseprogramacionmobile.data.DatosRecordatorios
import com.example.primeraclaseprogramacionmobile.model.Categoria
import com.example.primeraclaseprogramacionmobile.model.EstadoRecordatorio
import com.example.primeraclaseprogramacionmobile.model.Prioridad
import com.example.primeraclaseprogramacionmobile.model.Recordatorio
import com.example.primeraclaseprogramacionmobile.model.filtrarRecordatorios
import com.example.primeraclaseprogramacionmobile.ui.theme.ColoresRecordatorios
import com.example.primeraclaseprogramacionmobile.ui.theme.PrimeraClaseProgramacionMobileTheme

private data class PresentacionCategoria(@param:StringRes val etiqueta: Int, val color: Color)

private val presentacionesCategoria: Map<Categoria, PresentacionCategoria> = mapOf(
    Categoria.Estudio to PresentacionCategoria(R.string.category_study, ColoresRecordatorios.IdentidadEstudio),
    Categoria.Entregas to PresentacionCategoria(R.string.category_deliveries, ColoresRecordatorios.IdentidadEntregas),
    Categoria.Examenes to PresentacionCategoria(R.string.category_exams, ColoresRecordatorios.IdentidadExamenes),
)

@Composable
fun PantallaRecordatorios() {
    // Guardar claves simples permite restaurar los filtros al recrear la Activity.
    var prioridadClave by rememberSaveable { mutableStateOf<String?>(null) }
    var estadoClave by rememberSaveable { mutableStateOf<String?>(null) }
    val prioridad = Prioridad.opciones.find { it.clave == prioridadClave }
    val estado = EstadoRecordatorio.opciones.find { it.clave == estadoClave }
    val recordatorios = DatosRecordatorios.recordatorios
    val visibles = filtrarRecordatorios(recordatorios, prioridad, estado)

    Scaffold { espacioSistema ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(espacioSistema).testTag("lista-recordatorios"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "encabezado") {
                Encabezado(recordatorios.size, DatosRecordatorios.categorias.size)
            }
            item(key = "filtros") {
                Column {
                    EtiquetaSeccion(R.string.filter_state)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FiltroRecordatorio(estado == null, { estadoClave = null }, R.string.filter_all, "estado-todos")
                        EstadoRecordatorio.opciones.forEach { opcion ->
                            FiltroRecordatorio(
                                estado == opcion, { estadoClave = opcion.clave },
                                etiquetaEstado(opcion), "estado-${opcion.clave}",
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    EtiquetaSeccion(R.string.filter_priority)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FiltroRecordatorio(prioridad == null, { prioridadClave = null }, R.string.filter_all, "prioridad-todos")
                        Prioridad.opciones.forEach { opcion ->
                            FiltroRecordatorio(
                                prioridad == opcion, { prioridadClave = opcion.clave },
                                etiquetaPrioridad(opcion), "prioridad-${opcion.clave}",
                            )
                        }
                    }
                }
            }
            item(key = "resultados") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    EtiquetaSeccion(R.string.results_title, Modifier.weight(1f))
                    Text(
                        stringResource(R.string.results_count, visibles.size, recordatorios.size),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = ColoresRecordatorios.TextoSecundario,
                        modifier = Modifier.testTag("cantidad-resultados"),
                    )
                }
            }
            if (visibles.isEmpty()) {
                item(key = "vacio") {
                    Card(
                        Modifier.fillMaxWidth().testTag("sin-coincidencias"),
                        colors = CardDefaults.cardColors(containerColor = ColoresRecordatorios.Tarjeta),
                        border = BorderStroke(1.dp, ColoresRecordatorios.Borde),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.empty_body), style = MaterialTheme.typography.bodyMedium,
                                color = ColoresRecordatorios.TextoSecundario,
                            )
                        }
                    }
                }
            } else {
                items(visibles, key = { it.id }) { recordatorio -> TarjetaRecordatorio(recordatorio) }
            }
        }
    }
}

@Composable
private fun EtiquetaSeccion(@StringRes etiqueta: Int, modifier: Modifier = Modifier) {
    Text(
        stringResource(etiqueta), modifier,
        style = MaterialTheme.typography.labelSmall, color = ColoresRecordatorios.TextoSecundario,
    )
}

@Composable
private fun FiltroRecordatorio(selected: Boolean, onClick: () -> Unit, @StringRes etiqueta: Int, tag: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(etiqueta)) },
        modifier = Modifier.heightIn(min = 48.dp).testTag(tag),
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = ColoresRecordatorios.Tarjeta,
            labelColor = ColoresRecordatorios.TextoSecundario,
            selectedContainerColor = ColoresRecordatorios.Seleccion.copy(alpha = 0.15f),
            selectedLabelColor = ColoresRecordatorios.Texto,
        ),
        border = BorderStroke(1.dp, if (selected) ColoresRecordatorios.Seleccion else ColoresRecordatorios.Borde),
    )
}

@Composable
private fun Encabezado(total: Int, categorias: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EtiquetaSeccion(R.string.study_space)
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.screen_intro), style = MaterialTheme.typography.bodyMedium,
            color = ColoresRecordatorios.TextoSecundario,
        )
        Text(
            stringResource(R.string.summary, total, categorias), style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace, color = ColoresRecordatorios.TextoSecundario,
        )
        Text(
            stringResource(R.string.sample_data), style = MaterialTheme.typography.labelMedium,
            color = ColoresRecordatorios.TextoSecundario,
        )
    }
}

@Composable
private fun TarjetaRecordatorio(recordatorio: Recordatorio) {
    val categoria = presentacionesCategoria.getValue(recordatorio.categoria)
    Card(
        modifier = Modifier.fillMaxWidth().testTag("recordatorio-${recordatorio.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = ColoresRecordatorios.Tarjeta),
        border = BorderStroke(1.dp, ColoresRecordatorios.Borde),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = ColoresRecordatorios.Secundario, shape = MaterialTheme.shapes.extraSmall) {
                Text(
                    stringResource(categoria.etiqueta), Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium, color = categoria.color,
                )
            }
            Text(recordatorio.titulo, style = MaterialTheme.typography.titleMedium)
            recordatorio.descripcion?.let { descripcion ->
                Text(descripcion, style = MaterialTheme.typography.bodyMedium, color = ColoresRecordatorios.TextoSecundario)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.priority_label, stringResource(etiquetaPrioridad(recordatorio.prioridad))),
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                    color = ColoresRecordatorios.TextoSecundario,
                )
                IndicadorEstado(recordatorio.estado, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun IndicadorEstado(estado: EstadoRecordatorio, modifier: Modifier = Modifier) {
    val completado = estado == EstadoRecordatorio.Completado
    val color = if (completado) ColoresRecordatorios.Completado else ColoresRecordatorios.TextoSecundario
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(6.dp).then(
                if (completado) Modifier.background(color, CircleShape)
                else Modifier.border(1.dp, color, CircleShape),
            ),
        )
        Text(stringResource(etiquetaEstado(estado)), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@StringRes
private fun etiquetaPrioridad(prioridad: Prioridad): Int = when (prioridad) {
    Prioridad.Alta -> R.string.priority_high
    Prioridad.Media -> R.string.priority_medium
    Prioridad.Baja -> R.string.priority_low
}

@StringRes
private fun etiquetaEstado(estado: EstadoRecordatorio): Int = when (estado) {
    EstadoRecordatorio.Pendiente -> R.string.state_pending
    EstadoRecordatorio.Completado -> R.string.state_completed
}

@Preview(showBackground = true)
@Composable
private fun VistaPreviaRecordatorios() {
    PrimeraClaseProgramacionMobileTheme { PantallaRecordatorios() }
}
