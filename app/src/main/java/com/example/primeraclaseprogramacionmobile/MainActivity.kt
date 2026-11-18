package com.example.primeraclaseprogramacionmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.primeraclaseprogramacionmobile.data.RepositorioEnMemoria
import com.example.primeraclaseprogramacionmobile.ui.Navegacion
import com.example.primeraclaseprogramacionmobile.ui.theme.PrimeraClaseProgramacionMobileTheme

class SesionRecordatorios(escenario: String?) : ViewModel() {
    val repositorio = RepositorioEnMemoria(escenario)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            PrimeraClaseProgramacionMobileTheme {
                val sesion = viewModel { SesionRecordatorios(intent.getStringExtra("escenario")) }
                Navegacion(sesion.repositorio)
            }
        }
    }
}
