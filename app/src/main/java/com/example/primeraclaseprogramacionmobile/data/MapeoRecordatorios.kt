package com.example.primeraclaseprogramacionmobile.data

import com.example.primeraclaseprogramacionmobile.data.local.FilaRecordatorio
import com.example.primeraclaseprogramacionmobile.data.remoto.RecordatorioDto
import com.example.primeraclaseprogramacionmobile.model.*

internal fun Categoria.codigo() = when (this) {
    Categoria.Estudio -> "estudio"
    Categoria.Entregas -> "entregas"
    Categoria.Examenes -> "examenes"
}

internal fun Recordatorio.fila(revision: Long = 1) = FilaRecordatorio(
    id, titulo, descripcion, categoria.codigo(), prioridad.clave, estado.clave, fecha, !sincronizado, revision = revision,
)

internal fun FilaRecordatorio.dominio() = Recordatorio(
    id, titulo, when (categoria) {
        "estudio" -> Categoria.Estudio
        "entregas" -> Categoria.Entregas
        "examenes" -> Categoria.Examenes
        else -> throw IllegalArgumentException("Categoría desconocida")
    }, Prioridad.opciones.first { it.clave == prioridad }, EstadoRecordatorio.opciones.first { it.clave == estado },
    descripcion, fecha, !pendiente,
)

internal fun FilaRecordatorio.dto() = RecordatorioDto(id, titulo, descripcion, categoria, prioridad, estado, fecha)

internal fun RecordatorioDto.fila(): FilaRecordatorio {
    require(fecha > 0 && (descripcion?.cantidadCaracteres() ?: 0) <= 200)
    require(prioridad in Prioridad.opciones.map { it.clave } && estado in EstadoRecordatorio.opciones.map { it.clave })
    val fila = FilaRecordatorio(id, titulo, descripcion, categoria, prioridad, estado, fecha, pendiente = false)
    fila.dominio() // Valida el ID, el título y la categoría antes de aceptar una respuesta remota.
    return fila
}
