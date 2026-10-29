package com.example.primeraclaseprogramacionmobile.data

import com.example.primeraclaseprogramacionmobile.model.Categoria
import com.example.primeraclaseprogramacionmobile.model.EstadoRecordatorio
import com.example.primeraclaseprogramacionmobile.model.Prioridad
import com.example.primeraclaseprogramacionmobile.model.Recordatorio

object DatosRecordatorios {
    val recordatorios: List<Recordatorio> = listOf(
        Recordatorio(
            id = 1,
            titulo = "Leer fundamentos de Kotlin",
            categoria = Categoria.Estudio,
            prioridad = Prioridad.Alta,
            estado = EstadoRecordatorio.Pendiente,
            descripcion = "Repasar clases de datos, colecciones y valores opcionales.",
        ),
        Recordatorio(
            id = 2,
            titulo = "Preparar la entrega de API1",
            categoria = Categoria.Entregas,
            prioridad = Prioridad.Alta,
            estado = EstadoRecordatorio.Pendiente,
            descripcion = "Reunir las capturas y revisar el proyecto antes de entregar.",
        ),
        Recordatorio(
            id = 3,
            titulo = "Practicar Jetpack Compose",
            categoria = Categoria.Estudio,
            prioridad = Prioridad.Media,
            estado = EstadoRecordatorio.Pendiente,
        ),
        Recordatorio(
            id = 4,
            titulo = "Resolver ejercicios de colecciones",
            categoria = Categoria.Estudio,
            prioridad = Prioridad.Baja,
            estado = EstadoRecordatorio.Completado,
            descripcion = "Comparar List, Set y Map con ejemplos propios.",
        ),
        Recordatorio(
            id = 5,
            titulo = "Repasar para la autoevaluación",
            categoria = Categoria.Examenes,
            prioridad = Prioridad.Media,
            estado = EstadoRecordatorio.Completado,
            descripcion = "Revisar el ciclo de vida y la configuración de Gradle.",
        ),
        Recordatorio(
            id = 6,
            titulo = "Organizar las evidencias de ejecución",
            categoria = Categoria.Entregas,
            prioridad = Prioridad.Baja,
            estado = EstadoRecordatorio.Pendiente,
        ),
    )

    val categorias: Set<Categoria> = recordatorios.map { it.categoria }.toSet()
}
