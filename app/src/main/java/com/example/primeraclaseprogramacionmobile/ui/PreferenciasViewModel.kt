package com.example.primeraclaseprogramacionmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeraclaseprogramacionmobile.data.RepositorioRecordatorios
import com.example.primeraclaseprogramacionmobile.data.local.*
import java.io.IOException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PreferenciasViewModel(private val repositorio: RepositorioRecordatorios) : ViewModel() {
    val valores = repositorio.preferencias.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Preferencias())
    private val fallo = MutableStateFlow(false)
    val error = fallo.asStateFlow()

    fun tema(claro: Boolean) = guardar { repositorio.tema(claro) }
    fun completados(mostrar: Boolean) = guardar { repositorio.mostrarCompletados(mostrar) }
    fun orden(orden: Orden) = guardar { repositorio.ordenar(orden) }

    private fun guardar(accion: suspend () -> Unit) {
        viewModelScope.launch { try { accion(); fallo.value = false } catch (_: IOException) { fallo.value = true } }
    }
}
