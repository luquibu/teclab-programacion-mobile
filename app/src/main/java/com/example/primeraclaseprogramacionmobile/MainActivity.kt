package com.example.primeraclaseprogramacionmobile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.content.edit
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.primeraclaseprogramacionmobile.ui.*
import com.example.primeraclaseprogramacionmobile.ui.theme.PrimeraClaseProgramacionMobileTheme
import com.example.primeraclaseprogramacionmobile.data.local.Preferencias

class MainActivity : ComponentActivity() {
    private enum class AccionPermiso(@get:StringRes val etiqueta: Int) {
        Solicitar(R.string.allow_network),
        AbrirAjustes(R.string.open_app_settings),
    }

    private val fuente get() = application as FuenteRecordatorios
    private val sesiones get() = fuente.sesiones
    private var accionPermiso by mutableStateOf<AccionPermiso?>(null)
    private val ajustesPermiso by lazy { getPreferences(MODE_PRIVATE) }
    private val permiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (Build.VERSION.SDK_INT >= 37) {
            val bloqueado = !concedido && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_LOCAL_NETWORK)
            ajustesPermiso.edit { putBoolean(PERMISO_BLOQUEADO, bloqueado) }
            actualizarPermiso()
            if (concedido) sesiones.comprobar()
        }
    }

    private fun actualizarPermiso() {
        accionPermiso = when {
            !fuente.usaEmuladoresFirebase || Build.VERSION.SDK_INT < 37 -> null
            permisoRedConcedido() -> {
                if (ajustesPermiso.getBoolean(PERMISO_BLOQUEADO, false)) {
                    ajustesPermiso.edit { remove(PERMISO_BLOQUEADO) }
                }
                null
            }
            ajustesPermiso.getBoolean(PERMISO_BLOQUEADO, false) &&
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_LOCAL_NETWORK) -> AccionPermiso.AbrirAjustes
            else -> AccionPermiso.Solicitar
        }
    }

    private fun pedirPermisoRed() {
        when (accionPermiso) {
            AccionPermiso.Solicitar -> if (Build.VERSION.SDK_INT >= 37) permiso.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            AccionPermiso.AbrirAjustes -> startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            )
            null -> Unit
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarPermiso()
        if (accionPermiso == null) sesiones.comprobar()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val activa by sesiones.activa.collectAsStateWithLifecycle()
            val cuenta = activa
            key(cuenta?.uid) {
                val claro = if (cuenta == null) false else cuenta.repositorio.preferencias
                    .collectAsStateWithLifecycle(Preferencias()).value.claro
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = claro
                        isAppearanceLightNavigationBars = claro
                    }
                }
                PrimeraClaseProgramacionMobileTheme(claro) {
                    Surface(Modifier.fillMaxSize()) {
                        val accion = accionPermiso
                        if (cuenta != null) {
                            Navegacion(cuenta.repositorio, cuenta.correo, sesiones::cerrar, accion?.etiqueta, ::pedirPermisoRed)
                        } else if (accion != null) {
                            Column(
                                modifier = Modifier.safeDrawingPadding().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                            ) {
                                Text(stringResource(R.string.auth_emulator))
                                Button(onClick = ::pedirPermisoRed) { Text(stringResource(accion.etiqueta)) }
                            }
                        } else {
                            Acceso(viewModel { AccesoViewModel(sesiones) })
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val PERMISO_BLOQUEADO = "permiso_red_bloqueado"
    }
}
