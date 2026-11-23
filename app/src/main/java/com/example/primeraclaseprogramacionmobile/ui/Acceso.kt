package com.example.primeraclaseprogramacionmobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.primeraclaseprogramacionmobile.data.Sesiones
import com.example.primeraclaseprogramacionmobile.R
import com.example.primeraclaseprogramacionmobile.data.ErrorAcceso
import com.example.primeraclaseprogramacionmobile.data.MotivoAcceso
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstadoAcceso(val enviando: Boolean = false, val error: Int? = null)

class AccesoViewModel(private val sesiones: Sesiones) : ViewModel() {
    private val estado = MutableStateFlow(EstadoAcceso())
    val ui = estado.asStateFlow()

    fun limpiarError() {
        estado.value = estado.value.copy(error = null)
    }

    fun enviar(correo: String, clave: String, registrar: Boolean) {
        if (estado.value.enviando) return
        estado.value = EstadoAcceso(enviando = true)
        viewModelScope.launch {
            try {
                sesiones.autenticar(correo.trim(), clave, registrar)
                estado.value = EstadoAcceso()
            } catch (e: ErrorAcceso) {
                val mensaje = when (e.motivo) {
                    MotivoAcceso.Conexion -> R.string.auth_network
                    MotivoAcceso.ClaveDebil -> R.string.auth_weak
                    MotivoAcceso.CorreoRegistrado -> R.string.auth_used
                    MotivoAcceso.Espera -> R.string.auth_wait
                    MotivoAcceso.Credenciales -> R.string.auth_failed
                }
                estado.value = EstadoAcceso(error = mensaje)
            }
        }
    }
}

@Composable
fun Acceso(vm: AccesoViewModel) {
    val estado by vm.ui.collectAsStateWithLifecycle()
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var registrar by rememberSaveable { mutableStateOf(false) }
    val foco = LocalFocusManager.current
    val puedeEnviar = correo.isNotBlank() && clave.isNotEmpty() && !estado.enviando
    val enviar = {
        if (puedeEnviar) {
            foco.clearFocus()
            vm.enviar(correo, clave, registrar)
        }
    }
    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 420.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(if (registrar) R.string.auth_new else R.string.auth_login), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(correo, {
                correo = it
                vm.limpiarError()
            }, Modifier.fillMaxWidth(), enabled = !estado.enviando,
                label = { Text(stringResource(R.string.auth_email)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
            OutlinedTextField(clave, {
                clave = it
                vm.limpiarError()
            }, Modifier.fillMaxWidth(), enabled = !estado.enviando,
                label = { Text(stringResource(R.string.auth_password)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { enviar() }))
            estado.error?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            Button(onClick = enviar, enabled = puedeEnviar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (estado.enviando) R.string.auth_working else if (registrar) R.string.auth_register else R.string.auth_enter))
            }
            TextButton(onClick = {
                registrar = !registrar
                vm.limpiarError()
            }, enabled = !estado.enviando) {
                Text(stringResource(if (registrar) R.string.auth_existing else R.string.auth_create))
            }
        }
    }
}
