package com.example.primeraclaseprogramacionmobile.data.remoto

import kotlinx.serialization.Serializable
import java.io.IOException

@Serializable
data class RecordatorioDto(
    val id: Long, val titulo: String, val descripcion: String? = null, val categoria: String,
    val prioridad: String, val estado: String, val fecha: Long,
)

interface ServicioRecordatorios {
    /** Devuelve la colección completa de la cuenta, confirmada por el servidor, o falla.
     * El repositorio interpreta cada ID ausente como una baja remota. */
    suspend fun listar(): List<RecordatorioDto>
    suspend fun guardar(id: Long, registro: RecordatorioDto): RecordatorioDto
    suspend fun eliminar(id: Long)
}

class SolicitudRechazada : IOException()
class SesionNoAutenticada : IOException()
class ServicioNoDisponible : IOException()
