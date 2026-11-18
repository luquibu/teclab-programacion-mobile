package com.example.primeraclaseprogramacionmobile.model

internal fun String.cantidadCaracteres() = codePointCount(0, length)

data class Borrador(
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: Categoria? = null,
    val prioridad: Prioridad? = null,
) {
    val tituloValido get() = titulo.trim().cantidadCaracteres() >= 4
    val largoDescripcion get() = descripcion.cantidadCaracteres()
    val descripcionValida get() = largoDescripcion <= 200
    val valido get() = tituloValido && descripcionValida && categoria != null && prioridad != null

    fun recordatorio(id: Long): Recordatorio {
        require(valido)
        return Recordatorio(
            id, titulo.trim(), requireNotNull(categoria), requireNotNull(prioridad),
            EstadoRecordatorio.Pendiente, descripcion.trim().ifEmpty { null },
        )
    }
}
