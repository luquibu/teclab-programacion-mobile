package com.example.primeraclaseprogramacionmobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.primeraclaseprogramacionmobile.data.RepositorioRecordatorios
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable @SerialName("listado") private data object Listado
@Serializable @SerialName("crear") private data object Crear
@Serializable @SerialName("detalle") private data class Detalle(val id: Long)

@Composable
fun Navegacion(repositorio: RepositorioRecordatorios) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Listado) {
        composable<Listado> {
            val vm = viewModel { ListadoViewModel(repositorio, createSavedStateHandle()) }
            val estado by vm.uiState.collectAsStateWithLifecycle()
            PantallaRecordatorios(estado, vm::prioridad, vm::estado,
                onCrear = { nav.navigate(Crear) { launchSingleTop = true } },
                onSeleccionar = { id -> nav.navigate(Detalle(id)) { launchSingleTop = true } },
                onReintentar = vm::cargar)
        }
        composable<Crear> {
            val vm = viewModel { CreacionViewModel(repositorio) }
            val estado by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(estado.envio) { if (estado.envio == Envio.Guardado) nav.popBackStack() }
            PantallaCreacion(estado, vm::titulo, vm::descripcion, vm::categoria, vm::prioridad,
                onGuardar = vm::guardar, onCancelar = { nav.popBackStack() })
        }
        composable<Detalle> { entrada ->
            val vm = viewModel { DetalleViewModel(entrada.toRoute<Detalle>().id, repositorio) }
            val estado by vm.uiState.collectAsStateWithLifecycle()
            PantallaDetalle(estado) { nav.popBackStack() }
        }
    }
}
