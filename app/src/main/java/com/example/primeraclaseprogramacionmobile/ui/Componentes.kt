package com.example.primeraclaseprogramacionmobile.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.primeraclaseprogramacionmobile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BarraPantalla(titulo: String, modifier: Modifier = Modifier, onVolver: (() -> Unit)? = null) {
    TopAppBar(
        modifier = modifier,
        title = { Text(titulo, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onVolver != null) {
                val volver = stringResource(R.string.return_list)
                IconButton(onVolver, Modifier.semantics { contentDescription = volver }) {
                    Text("‹", style = MaterialTheme.typography.headlineLarge)
                }
            }
        },
    )
}

@Composable
internal fun MensajePantalla(
    @StringRes titulo: Int,
    modifier: Modifier = Modifier,
    @StringRes mensaje: Int? = null,
    @StringRes accion: Int? = null,
    onAccion: () -> Unit = {},
    cargando: Boolean = false,
) {
    Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 400.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(stringResource(titulo), style = MaterialTheme.typography.headlineSmall)
            mensaje?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            accion?.let { Button(onClick = onAccion) { Text(stringResource(it)) } }
        }
    }
}
