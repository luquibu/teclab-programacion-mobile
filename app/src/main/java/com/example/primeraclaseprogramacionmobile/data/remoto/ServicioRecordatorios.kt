package com.example.primeraclaseprogramacionmobile.data.remoto

import kotlinx.serialization.Serializable
import java.io.IOException

@Serializable
data class RecordatorioDto(
    val id: Long, val titulo: String, val descripcion: String? = null, val categoria: String,
    val prioridad: String, val estado: String, val fecha: Long,
)

interface ServicioRecordatorios {
    suspend fun listar(): List<RecordatorioDto>
    suspend fun guardar(id: Long, registro: RecordatorioDto): RecordatorioDto
    suspend fun eliminar(id: Long)
}

class SolicitudRechazada : IOException()
class ServicioNoDisponible : IOException()
