package com.example.primeraclaseprogramacionmobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.primeraclaseprogramacionmobile.R

@Composable
fun PantallaDetalle(ui: DetalleUiState, onVolver: () -> Unit) {
    Scaffold(topBar = { BarraPantalla(stringResource(R.string.detail_title), onVolver = onVolver) }) { margen ->
        val contenido = Modifier.fillMaxSize().padding(margen)
        when (ui) {
            DetalleUiState.Cargando -> MensajePantalla(R.string.loading, modifier = contenido, cargando = true)
            DetalleUiState.Ausente -> MensajePantalla(R.string.missing, accion = R.string.return_list, onAccion = onVolver, modifier = contenido)
            is DetalleUiState.Datos -> {
                val r = ui.registro
                Column(contenido.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.id_label, r.id), style = MaterialTheme.typography.labelMedium)
                    Text(r.titulo, style = MaterialTheme.typography.headlineMedium)
                    Text(r.descripcion ?: stringResource(R.string.without_description))
                    HorizontalDivider()
                    Atributo(R.string.category_field, presentacionesCategoria.getValue(r.categoria).etiqueta)
                    Atributo(R.string.filter_priority, etiquetaPrioridad(r.prioridad))
                    Atributo(R.string.filter_state, etiquetaEstado(r.estado))
                }
            }
        }
    }
}

@Composable
private fun Atributo(etiqueta: Int, valor: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(etiqueta), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(valor), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
    }
}
