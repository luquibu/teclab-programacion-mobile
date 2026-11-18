package com.example.primeraclaseprogramacionmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeraclaseprogramacionmobile.data.RepositorioRecordatorios
import com.example.primeraclaseprogramacionmobile.model.Borrador
import com.example.primeraclaseprogramacionmobile.model.Categoria
import com.example.primeraclaseprogramacionmobile.model.Prioridad
import com.example.primeraclaseprogramacionmobile.model.EstadoRecordatorio
import kotlinx.coroutines.flow.first
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Envio { Cargando, Ausente, Inaccesible, Edicion, EnCurso, Guardado, Error }

data class CreacionUiState(
    val borrador: Borrador = Borrador(),
    val estadoRegistro: EstadoRecordatorio = EstadoRecordatorio.Pendiente,
    val tituloEditado: Boolean = false,
    val envio: Envio = Envio.Edicion,
) {
    val editable get() = envio == Envio.Edicion || envio == Envio.Error
    val puedeGuardar get() = editable && borrador.valido
    val errorTitulo get() = tituloEditado && !borrador.tituloValido
}

class CreacionViewModel(private val repositorio: RepositorioRecordatorios, private val id: Long? = null) : ViewModel() {
    private val estado = MutableStateFlow(CreacionUiState(envio = if (id == null) Envio.Edicion else Envio.Cargando))
    val uiState = estado.asStateFlow()

    init {
        if (id != null) viewModelScope.launch {
            val r = try { repositorio.observar(id).first() } catch (_: android.database.sqlite.SQLiteException) {
                estado.value = CreacionUiState(envio = Envio.Inaccesible)
                return@launch
            }
            estado.value = if (r == null) CreacionUiState(envio = Envio.Ausente) else
                CreacionUiState(Borrador(r.titulo, r.descripcion.orEmpty(), r.categoria, r.prioridad), r.estado)
        }
    }

    fun estadoRegistro(valor: EstadoRecordatorio) = editar { it.copy(estadoRegistro = valor) }

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
                if (id == null) repositorio.guardar(actual.borrador)
                else repositorio.modificar(id, actual.borrador, actual.estadoRegistro)
                Envio.Guardado
            } catch (_: IOException) {
                Envio.Error
            }
            estado.update { it.copy(envio = resultado) }
        }
    }
}
