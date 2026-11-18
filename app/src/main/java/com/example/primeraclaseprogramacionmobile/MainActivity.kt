package com.example.primeraclaseprogramacionmobile

import com.example.primeraclaseprogramacionmobile.data.Sincronizacion
import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.primeraclaseprogramacionmobile.ui.PreferenciasViewModel
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.primeraclaseprogramacionmobile.ui.Navegacion
import com.example.primeraclaseprogramacionmobile.ui.theme.PrimeraClaseProgramacionMobileTheme

class MainActivity : ComponentActivity() {
    private val permiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) (application as FuenteRecordatorios).repositorio.solicitarSincronizacion()
        else Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        val repo = (application as FuenteRecordatorios).repositorio
        if (repo.sincronizacion.value == Sincronizacion.FaltaPermiso && permisoRedConcedido()) repo.solicitarSincronizacion()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            val repo = (application as FuenteRecordatorios).repositorio
            val vm = viewModel { PreferenciasViewModel(repo) }
            val ajustes by vm.valores.collectAsStateWithLifecycle()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = ajustes.claro
                    isAppearanceLightNavigationBars = ajustes.claro
                }
            }
            PrimeraClaseProgramacionMobileTheme(ajustes.claro) {
                Navegacion(repo) {
                    if (Build.VERSION.SDK_INT >= 37 && !permisoRedConcedido()) permiso.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                    else repo.solicitarSincronizacion()
                }
            }
        }
    }
}
