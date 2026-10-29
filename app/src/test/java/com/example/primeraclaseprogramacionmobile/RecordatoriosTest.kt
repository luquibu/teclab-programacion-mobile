package com.example.primeraclaseprogramacionmobile

import com.example.primeraclaseprogramacionmobile.data.DatosRecordatorios
import com.example.primeraclaseprogramacionmobile.model.Categoria
import com.example.primeraclaseprogramacionmobile.model.EstadoRecordatorio
import com.example.primeraclaseprogramacionmobile.model.Prioridad
import com.example.primeraclaseprogramacionmobile.model.Recordatorio
import com.example.primeraclaseprogramacionmobile.model.filtrarRecordatorios
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordatoriosTest {
    private val datos = DatosRecordatorios.recordatorios

    @Test
    fun rechazaIdentificadoresNoPositivos() {
        listOf(0L, -1L).forEach { id ->
            assertThrows(IllegalArgumentException::class.java) { datos.first().copy(id = id) }
        }
    }

    @Test
    fun rechazaTitulosVaciosOCortosAunqueTenganEspaciosExternos() {
        listOf("", "   ", "abc", "  abc  ").forEach { titulo ->
            assertThrows(IllegalArgumentException::class.java) { datos.first().copy(titulo = titulo) }
        }
    }

    @Test
    fun admiteElLimiteDeCuatroCaracteresYDescripcionAusente() {
        val recordatorio = Recordatorio(
            1, "  Leer  ", Categoria.Estudio, Prioridad.Alta, EstadoRecordatorio.Pendiente,
        )
        assertEquals("Leer", recordatorio.titulo.trim())
        assertNull(recordatorio.descripcion)
    }

    @Test
    fun sinFiltrosConservaLosSeisRecordatoriosYSuOrden() {
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L), filtrarRecordatorios(datos).map { it.id })
    }

    @Test
    fun filtraCadaPrioridad() {
        assertEquals(listOf(1L, 2L), filtrarRecordatorios(datos, Prioridad.Alta).map { it.id })
        assertEquals(listOf(3L, 5L), filtrarRecordatorios(datos, Prioridad.Media).map { it.id })
        assertEquals(listOf(4L, 6L), filtrarRecordatorios(datos, Prioridad.Baja).map { it.id })
    }

    @Test
    fun filtraCadaEstado() {
        assertEquals(
            listOf(1L, 2L, 3L, 6L),
            filtrarRecordatorios(datos, estado = EstadoRecordatorio.Pendiente).map { it.id },
        )
        assertEquals(
            listOf(4L, 5L),
            filtrarRecordatorios(datos, estado = EstadoRecordatorio.Completado).map { it.id },
        )
    }

    @Test
    fun combinaLosFiltrosSinModificarLaListaOriginal() {
        val original = datos.toList()
        val resultado = filtrarRecordatorios(datos, Prioridad.Media, EstadoRecordatorio.Completado)
        assertEquals(listOf(5L), resultado.map { it.id })
        assertEquals(original, datos)
    }

    @Test
    fun devuelveVacioCuandoNoHayCoincidencias() {
        assertTrue(filtrarRecordatorios(datos, Prioridad.Alta, EstadoRecordatorio.Completado).isEmpty())
    }

    @Test
    fun admiteUnaListaDeOrigenVacia() {
        assertTrue(filtrarRecordatorios(emptyList(), Prioridad.Alta).isEmpty())
    }

    @Test
    fun agrupaCategoriasRepetidasEnUnSet() {
        assertEquals(setOf(Categoria.Estudio, Categoria.Entregas, Categoria.Examenes), DatosRecordatorios.categorias)
        assertEquals(6, datos.map { it.id }.toSet().size)
    }
}
