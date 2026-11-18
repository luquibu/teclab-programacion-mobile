package com.example.primeraclaseprogramacionmobile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeraclaseprogramacionmobile.data.RepositorioRecordatorios
import com.example.primeraclaseprogramacionmobile.model.EstadoRecordatorio
import com.example.primeraclaseprogramacionmobile.model.Prioridad
import com.example.primeraclaseprogramacionmobile.model.Recordatorio
import com.example.primeraclaseprogramacionmobile.model.filtrarRecordatorios
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Filtros(val prioridad: Prioridad? = null, val estado: EstadoRecordatorio? = null)

sealed interface ListadoUiState {
    data object Cargando : ListadoUiState
    data object Vacio : ListadoUiState
    data object Error : ListadoUiState
    data class Datos(val visibles: List<Recordatorio>, val total: Int, val filtros: Filtros) : ListadoUiState
}

class ListadoViewModel(
    private val repositorio: RepositorioRecordatorios,
    private val guardado: SavedStateHandle,
) : ViewModel() {
    private enum class Carga { EN_CURSO, LISTA, ERROR }
    private val carga = MutableStateFlow(Carga.EN_CURSO)
    private val filtros = combine(
        guardado.getStateFlow<String?>("prioridad", null),
        guardado.getStateFlow<String?>("estado", null),
    ) { prioridad, estado ->
        Filtros(Prioridad.opciones.find { it.clave == prioridad }, EstadoRecordatorio.opciones.find { it.clave == estado })
    }
    private var lectura: Job? = null

    val uiState: StateFlow<ListadoUiState> = combine(carga, repositorio.recordatorios, filtros) { fase, datos, filtro ->
        when (fase) {
            Carga.EN_CURSO -> ListadoUiState.Cargando
            Carga.ERROR -> ListadoUiState.Error
            Carga.LISTA -> if (datos.isEmpty()) ListadoUiState.Vacio else ListadoUiState.Datos(
                filtrarRecordatorios(datos, filtro.prioridad, filtro.estado), datos.size, filtro,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListadoUiState.Cargando)

    init { cargar() }

    fun cargar() {
        if (lectura?.isActive == true) return
        lectura = viewModelScope.launch {
            carga.value = Carga.EN_CURSO
            carga.value = try {
                repositorio.cargar()
                Carga.LISTA
            } catch (_: IOException) {
                Carga.ERROR
            }
        }
    }

    fun prioridad(valor: Prioridad?) { guardado["prioridad"] = valor?.clave }
    fun estado(valor: EstadoRecordatorio?) { guardado["estado"] = valor?.clave }
}
