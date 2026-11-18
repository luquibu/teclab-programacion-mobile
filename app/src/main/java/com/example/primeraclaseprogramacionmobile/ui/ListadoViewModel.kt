package com.example.primeraclaseprogramacionmobile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeraclaseprogramacionmobile.data.RepositorioRecordatorios
import com.example.primeraclaseprogramacionmobile.data.local.Orden
import com.example.primeraclaseprogramacionmobile.model.*
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class Filtros(val prioridad: Prioridad? = null, val estado: EstadoRecordatorio? = null)

sealed interface ListadoUiState {
    data object Cargando : ListadoUiState
    data object Vacio : ListadoUiState
    data object Error : ListadoUiState
    data class Datos(val visibles: List<Recordatorio>, val total: Int, val filtros: Filtros) : ListadoUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class ListadoViewModel(private val repositorio: RepositorioRecordatorios, private val guardado: SavedStateHandle) : ViewModel() {
    val sincronizacion = repositorio.sincronizacion
    fun sincronizar() = repositorio.solicitarSincronizacion()

    private val lecturas = MutableStateFlow(0)
    private val filtros = combine(guardado.getStateFlow<String?>("prioridad", null), guardado.getStateFlow<String?>("estado", null)) { p, e ->
        Filtros(Prioridad.opciones.find { it.clave == p }, EstadoRecordatorio.opciones.find { it.clave == e })
    }
    val uiState = lecturas.flatMapLatest {
        combine(repositorio.registros(), repositorio.preferencias, filtros) { datos, ajustes, filtro ->
            if (datos.isEmpty()) return@combine ListadoUiState.Vacio
            val visibles = filtrarRecordatorios(datos, filtro.prioridad, filtro.estado)
                .filter { ajustes.mostrarCompletados || it.estado != EstadoRecordatorio.Completado }
            val ordenados = when (ajustes.orden) {
                Orden.Recientes -> visibles
                Orden.Titulo -> visibles.sortedBy { it.titulo.lowercase(Locale.ROOT) }
                Orden.Prioridad -> visibles.sortedBy { Prioridad.opciones.indexOf(it.prioridad) }
            }
            ListadoUiState.Datos(ordenados, datos.size, filtro)
        }.catch { emit(ListadoUiState.Error) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListadoUiState.Cargando)

    fun cargar() = lecturas.update { it + 1 }
    fun prioridad(valor: Prioridad?) { guardado["prioridad"] = valor?.clave }
    fun estado(valor: EstadoRecordatorio?) { guardado["estado"] = valor?.clave }
}
