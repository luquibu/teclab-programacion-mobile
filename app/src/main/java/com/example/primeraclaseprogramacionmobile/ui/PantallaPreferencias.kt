package com.example.primeraclaseprogramacionmobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.primeraclaseprogramacionmobile.R
import com.example.primeraclaseprogramacionmobile.data.local.*

@Composable
fun PantallaPreferencias(p: Preferencias, error: Boolean, tema: (Boolean) -> Unit,
    completados: (Boolean) -> Unit, orden: (Orden) -> Unit, volver: () -> Unit, correo: String, cerrarSesion: () -> Unit) {
    Scaffold(topBar = { BarraPantalla(stringResource(R.string.settings), onVolver = volver) }) { margen ->
        Column(Modifier.padding(margen).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Interruptor(R.string.light_theme, p.claro, tema)
            Interruptor(R.string.show_completed, p.mostrarCompletados, completados)
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text(stringResource(R.string.sort_by), style = MaterialTheme.typography.titleMedium)
            Column(Modifier.selectableGroup()) {
                for (opcion in Orden.entries) {
                    val etiqueta = when (opcion) {
                        Orden.Recientes -> R.string.most_recent
                        Orden.Titulo -> R.string.title_field
                        Orden.Prioridad -> R.string.filter_priority
                    }
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(p.orden == opcion,
                        role = Role.RadioButton, onClick = { orden(opcion) }), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(p.orden == opcion, onClick = null)
                        Text(stringResource(etiqueta), Modifier.padding(start = 12.dp))
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text(correo, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = cerrarSesion) { Text(stringResource(R.string.auth_logout)) }
            if (error) Text(stringResource(R.string.settings_error), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun Interruptor(etiqueta: Int, activo: Boolean, cambiar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(activo, role = Role.Switch,
        onValueChange = cambiar), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(etiqueta), Modifier.weight(1f))
        Switch(activo, onCheckedChange = null)
    }
}
