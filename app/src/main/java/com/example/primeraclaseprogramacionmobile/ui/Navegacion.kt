package com.example.primeraclaseprogramacionmobile.ui

import androidx.compose.runtime.*
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.example.primeraclaseprogramacionmobile.data.RepositorioRecordatorios
import com.example.primeraclaseprogramacionmobile.data.Sincronizacion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable @SerialName("listado") private data object Listado
@Serializable @SerialName("crear") private data object Crear
@Serializable @SerialName("detalle") private data class Detalle(val id: Long)
@Serializable @SerialName("editar") private data class Editar(val id: Long)
@Serializable @SerialName("ajustes") private data object Ajustes

@Composable
fun Navegacion(repositorio: RepositorioRecordatorios, correo: String, cerrarSesion: () -> Unit, pedirPermiso: () -> Unit) {
    val nav = rememberNavController()
    val volver: () -> Unit = { nav.popBackStack() }
    NavHost(nav, startDestination = Listado) {
        composable<Listado> {
            val vm = viewModel { ListadoViewModel(repositorio, createSavedStateHandle()) }
            val estado by vm.uiState.collectAsStateWithLifecycle()
            val red by vm.sincronizacion.collectAsStateWithLifecycle()
            PantallaRecordatorios(estado, vm::prioridad, vm::estado,
                onCrear = { nav.navigate(Crear) { launchSingleTop = true } },
                onSeleccionar = { id -> nav.navigate(Detalle(id)) { launchSingleTop = true } },
                onReintentar = vm::cargar, red = red, onSincronizar = { if (red == Sincronizacion.FaltaPermiso) pedirPermiso() else vm.sincronizar() },
                onAjustes = { nav.navigate(Ajustes) { launchSingleTop = true } })
        }
        composable<Crear> { Formulario(repositorio, null, volver) }
        composable<Editar> { Formulario(repositorio, it.toRoute<Editar>().id, volver) }
        composable<Detalle> { entrada ->
            val id = entrada.toRoute<Detalle>().id
            val vm = viewModel { DetalleViewModel(id, repositorio) }
            val estado by vm.uiState.collectAsStateWithLifecycle()
            PantallaDetalle(estado, volver, onEditar = { nav.navigate(Editar(id)) { launchSingleTop = true } },
                onEliminar = { vm.eliminar(volver) })
        }
        composable<Ajustes> {
            val vm = viewModel { PreferenciasViewModel(repositorio) }
            val ajustes by vm.valores.collectAsStateWithLifecycle()
            val error by vm.error.collectAsStateWithLifecycle()
            PantallaPreferencias(ajustes, error, vm::tema, vm::completados, vm::orden, volver, correo, cerrarSesion)
        }
    }
}

@Composable
private fun Formulario(repositorio: RepositorioRecordatorios, id: Long?, volver: () -> Unit) {
    val vm = viewModel { CreacionViewModel(repositorio, id) }
    val estado by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(estado.envio) { if (estado.envio == Envio.Guardado) volver() }
    PantallaCreacion(estado, vm::titulo, vm::descripcion, vm::categoria, vm::prioridad,
        vm::guardar, volver, edicion = id != null, onEstado = vm::estadoRegistro)
}
