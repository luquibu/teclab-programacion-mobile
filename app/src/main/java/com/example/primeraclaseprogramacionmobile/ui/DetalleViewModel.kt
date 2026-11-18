package com.example.primeraclaseprogramacionmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeraclaseprogramacionmobile.data.RepositorioRecordatorios
import com.example.primeraclaseprogramacionmobile.model.Recordatorio
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface DetalleUiState {
    data object Cargando : DetalleUiState
    data object Ausente : DetalleUiState
    data class Datos(val registro: Recordatorio) : DetalleUiState
}

class DetalleViewModel(id: Long, repositorio: RepositorioRecordatorios) : ViewModel() {
    val uiState: StateFlow<DetalleUiState> = repositorio.recordatorios.map { datos ->
        datos.find { it.id == id }?.let { DetalleUiState.Datos(it) } ?: DetalleUiState.Ausente
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState.Cargando)
}
