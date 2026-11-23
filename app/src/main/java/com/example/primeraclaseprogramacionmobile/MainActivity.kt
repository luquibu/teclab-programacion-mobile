package com.example.primeraclaseprogramacionmobile

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.primeraclaseprogramacionmobile.ui.*
import com.example.primeraclaseprogramacionmobile.ui.theme.PrimeraClaseProgramacionMobileTheme
import com.example.primeraclaseprogramacionmobile.data.local.Preferencias

class MainActivity : ComponentActivity() {
    private val fuente get() = application as FuenteRecordatorios
    private val sesiones get() = fuente.sesiones
    private var autorizado by mutableStateOf(false)
    private val permiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        autorizado = ok
        if (ok) sesiones.comprobar()
    }
    override fun onResume() {
        super.onResume()
        autorizado = !fuente.usaEmuladoresFirebase || permisoRedConcedido()
        sesiones.comprobar()
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
                        if (!autorizado && Build.VERSION.SDK_INT >= 37) {
                            Box(contentAlignment = Alignment.Center) {
                                Button(onClick = { permiso.launch(Manifest.permission.ACCESS_LOCAL_NETWORK) }) {
                                    Text(androidx.compose.ui.res.stringResource(R.string.auth_emulator))
                                }
                            }
                        } else if (cuenta == null) {
                            Acceso(viewModel { AccesoViewModel(sesiones) })
                        } else {
                            Navegacion(cuenta.repositorio, cuenta.correo, sesiones::cerrar) {
                                cuenta.repositorio.solicitarSincronizacion()
                            }
                        }
                    }
                }
            }
        }
    }
}
