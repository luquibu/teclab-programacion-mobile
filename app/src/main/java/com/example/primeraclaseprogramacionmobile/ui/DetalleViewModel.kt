package com.example.primeraclaseprogramacionmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeraclaseprogramacionmobile.data.RepositorioRecordatorios
import com.example.primeraclaseprogramacionmobile.model.Recordatorio
import java.io.IOException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface DetalleUiState {
    data object Cargando : DetalleUiState
    data object Ausente : DetalleUiState
    data object Error : DetalleUiState
    data class Datos(val registro: Recordatorio) : DetalleUiState
}

class DetalleViewModel(private val id: Long, private val repositorio: RepositorioRecordatorios) : ViewModel() {
    private val errorBaja = MutableStateFlow(false)
    val uiState: StateFlow<DetalleUiState> = combine(repositorio.observar(id), errorBaja) { dato, error ->
        when {
            error -> DetalleUiState.Error
            dato == null -> DetalleUiState.Ausente
            else -> DetalleUiState.Datos(dato)
        }
    }.catch { emit(DetalleUiState.Error) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState.Cargando)

    fun eliminar(terminado: () -> Unit) {
        viewModelScope.launch {
            try {
                repositorio.eliminar(id)
                terminado()
            } catch (_: IOException) { errorBaja.value = true }
        }
    }
}
