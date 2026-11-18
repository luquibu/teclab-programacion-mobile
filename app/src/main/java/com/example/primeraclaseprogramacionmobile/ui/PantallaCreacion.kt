package com.example.primeraclaseprogramacionmobile.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.primeraclaseprogramacionmobile.ui.theme.ColoresRecordatorios
import com.example.primeraclaseprogramacionmobile.R
import com.example.primeraclaseprogramacionmobile.model.Categoria
import com.example.primeraclaseprogramacionmobile.model.Prioridad

@Composable
fun PantallaCreacion(
    ui: CreacionUiState,
    onTitulo: (String) -> Unit,
    onDescripcion: (String) -> Unit,
    onCategoria: (Categoria) -> Unit,
    onPrioridad: (Prioridad) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    edicion: Boolean = false,
    onEstado: (com.example.primeraclaseprogramacionmobile.model.EstadoRecordatorio) -> Unit = {},
) {
    if (ui.envio == Envio.Inaccesible || ui.envio == Envio.Ausente) {
        MensajePantalla(if (ui.envio == Envio.Ausente) R.string.missing else R.string.local_error, Modifier.fillMaxSize(), accion = R.string.return_list, onAccion = onCancelar)
        return
    }
    BackHandler(enabled = ui.envio == Envio.EnCurso) { /* El envío en curso conserva su destino. */ }
    Scaffold(topBar = { BarraPantalla(stringResource(if (edicion) R.string.edit_reminder else R.string.new_reminder)) }) { margen ->
        Column(
            Modifier.fillMaxSize().padding(margen).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val ancho = Modifier.widthIn(max = 560.dp).fillMaxWidth()
            val b = ui.borrador
            CampoTexto(b.titulo, onTitulo, R.string.title_field, ui.editable, ancho.testTag("titulo"),
                error = if (ui.errorTitulo) R.string.title_invalid else null)
            CampoTexto(b.descripcion, onDescripcion, R.string.description_field, ui.editable, ancho.testTag("descripcion"),
                error = if (b.descripcionValida) null else R.string.description_invalid,
                contador = stringResource(R.string.description_count, b.largoDescripcion))
            Opciones(
                R.string.category_field, presentacionesCategoria.keys.toList(), b.categoria,
                { presentacionesCategoria.getValue(it).etiqueta }, ui.editable, onCategoria, ancho,
            )
            Opciones(R.string.filter_priority, Prioridad.opciones, b.prioridad, ::etiquetaPrioridad, ui.editable, onPrioridad, ancho)
            if (edicion) Opciones(R.string.filter_state, com.example.primeraclaseprogramacionmobile.model.EstadoRecordatorio.opciones,
                ui.estadoRegistro, ::etiquetaEstado, ui.editable, onEstado, ancho)
            if (ui.envio == Envio.Error) {
                Text(stringResource(R.string.save_error), ancho.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.error)
            }
            Row(ancho, horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                OutlinedButton(onCancelar, enabled = ui.editable) { Text(stringResource(R.string.cancel)) }
                Button(onGuardar, Modifier.testTag("guardar"), enabled = ui.puedeGuardar) {
                    Text(stringResource(if (ui.envio == Envio.EnCurso) R.string.saving else R.string.save))
                }
            }
        }
    }
}

@Composable
private fun CampoTexto(
    valor: String, onCambio: (String) -> Unit, @StringRes etiqueta: Int, habilitado: Boolean,
    modifier: Modifier = Modifier, @StringRes error: Int? = null, contador: String? = null,
) {
    OutlinedTextField(
        value = valor, onValueChange = onCambio, modifier = modifier,
        label = { Text(stringResource(etiqueta)) },
        enabled = habilitado, singleLine = contador == null, minLines = if (contador == null) 1 else 2,
        isError = error != null,
        supportingText = if (error != null || contador != null) {
            { Text(error?.let { stringResource(it) } ?: contador.orEmpty()) }
        } else null,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Opciones(
    @StringRes etiqueta: Int, opciones: List<T>, seleccion: T?, nombre: (T) -> Int,
    habilitado: Boolean, onCambio: (T) -> Unit, modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(stringResource(etiqueta), style = MaterialTheme.typography.labelLarge)
        if (seleccion == null) {
            Text(stringResource(R.string.choose_option), style = MaterialTheme.typography.bodySmall)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opciones.forEach { opcion ->
                FilterChip(
                    selected = opcion == seleccion, onClick = { onCambio(opcion) }, enabled = habilitado,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ColoresRecordatorios.Seleccion.copy(alpha = 0.20f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    label = { Text(stringResource(nombre(opcion))) }, modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}
