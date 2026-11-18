package com.example.primeraclaseprogramacionmobile.data

import com.example.primeraclaseprogramacionmobile.model.Borrador
import com.example.primeraclaseprogramacionmobile.model.Recordatorio
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface RepositorioRecordatorios {
    val recordatorios: StateFlow<List<Recordatorio>>
    suspend fun cargar()
    suspend fun guardar(borrador: Borrador)
}

class RepositorioEnMemoria(escenario: String? = null) : RepositorioRecordatorios {
    private val datos = MutableStateFlow(
        if (escenario == "vacio") emptyList() else DatosRecordatorios.recordatorios,
    )
    override val recordatorios = datos.asStateFlow()
    private var fallaCarga = escenario == "error"
    private var fallaGuardado = escenario == "error_guardado"

    override suspend fun cargar() {
        delay(350)
        if (fallaCarga) {
            fallaCarga = false
            throw IOException("Error de carga simulado")
        }
    }

    override suspend fun guardar(borrador: Borrador) {
        delay(700)
        if (fallaGuardado) {
            fallaGuardado = false
            throw IOException("Error de guardado simulado")
        }
        datos.update { actuales ->
            listOf(borrador.recordatorio((actuales.maxOfOrNull { it.id } ?: 0L) + 1)) + actuales
        }
    }
}
