package com.example.primeraclaseprogramacionmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeraclaseprogramacionmobile.data.RepositorioRecordatorios
import com.example.primeraclaseprogramacionmobile.model.Borrador
import com.example.primeraclaseprogramacionmobile.model.Categoria
import com.example.primeraclaseprogramacionmobile.model.Prioridad
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Envio { Edicion, EnCurso, Guardado, Error }

data class CreacionUiState(
    val borrador: Borrador = Borrador(),
    val tituloEditado: Boolean = false,
    val envio: Envio = Envio.Edicion,
) {
    val editable get() = envio == Envio.Edicion || envio == Envio.Error
    val puedeGuardar get() = editable && borrador.valido
    val errorTitulo get() = tituloEditado && !borrador.tituloValido
}

class CreacionViewModel(private val repositorio: RepositorioRecordatorios) : ViewModel() {
    private val estado = MutableStateFlow(CreacionUiState())
    val uiState = estado.asStateFlow()

    private fun editar(cambio: (CreacionUiState) -> CreacionUiState) = estado.update {
        if (it.editable) cambio(it).copy(envio = Envio.Edicion) else it
    }

    fun titulo(valor: String) = editar { it.copy(borrador = it.borrador.copy(titulo = valor), tituloEditado = true) }
    fun descripcion(valor: String) = editar { it.copy(borrador = it.borrador.copy(descripcion = valor)) }
    fun categoria(valor: Categoria) = editar { it.copy(borrador = it.borrador.copy(categoria = valor)) }
    fun prioridad(valor: Prioridad) = editar { it.copy(borrador = it.borrador.copy(prioridad = valor)) }

    fun guardar() {
        val actual = estado.value
        if (!actual.puedeGuardar || !estado.compareAndSet(actual, actual.copy(envio = Envio.EnCurso))) return
        viewModelScope.launch {
            val resultado = try {
                repositorio.guardar(actual.borrador)
                Envio.Guardado
            } catch (_: IOException) {
                Envio.Error
            }
            estado.update { it.copy(envio = resultado) }
        }
    }
}
