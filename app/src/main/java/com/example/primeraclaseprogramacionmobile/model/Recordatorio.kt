package com.example.primeraclaseprogramacionmobile.model

sealed class Categoria {
    data object Estudio : Categoria()
    data object Entregas : Categoria()
    data object Examenes : Categoria()
}

sealed class Prioridad(val clave: String) {
    data object Alta : Prioridad("alta")
    data object Media : Prioridad("media")
    data object Baja : Prioridad("baja")

    companion object {
        val opciones: List<Prioridad> = listOf(Alta, Media, Baja)
    }
}

sealed class EstadoRecordatorio(val clave: String) {
    data object Pendiente : EstadoRecordatorio("pendiente")
    data object Completado : EstadoRecordatorio("completado")

    companion object {
        val opciones: List<EstadoRecordatorio> = listOf(Pendiente, Completado)
    }
}

/**
 * Datos de un recordatorio sin dependencia del entorno Android.
 *
 * @throws IllegalArgumentException si el identificador no es positivo o el título,
 * sin espacios externos, tiene menos de cuatro caracteres. La descripción puede ser null.
 */
data class Recordatorio(
    val id: Long,
    val titulo: String,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: EstadoRecordatorio,
    val descripcion: String? = null,
) {
    init {
        require(id > 0) { "El identificador debe ser positivo." }
        require(titulo.trim().length >= 4) { "El título debe tener al menos cuatro caracteres." }
    }
}

/** null representa todos los valores del filtro; ambos filtros se combinan con AND. */
fun filtrarRecordatorios(
    recordatorios: List<Recordatorio>,
    prioridad: Prioridad? = null,
    estado: EstadoRecordatorio? = null,
): List<Recordatorio> = recordatorios.filter { recordatorio ->
    (prioridad == null || recordatorio.prioridad == prioridad) &&
        (estado == null || recordatorio.estado == estado)
}
