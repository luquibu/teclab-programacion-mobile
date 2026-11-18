package com.example.primeraclaseprogramacionmobile

import com.example.primeraclaseprogramacionmobile.model.*
import org.junit.Assert.*
import org.junit.Test

class BorradorTest {
    private val base = Borrador(" Leer ", "", Categoria.Estudio, Prioridad.Media)

    @Test fun validaTituloYRecortaElTextoAlGuardar() {
        assertFalse(base.copy(titulo = "  abc   ").valido)
        assertEquals("Leer", base.recordatorio(7).titulo)
        assertNull(base.recordatorio(7).descripcion)
    }

    @Test fun admiteDoscientosCaracteresYRechazaElSiguiente() {
        assertTrue(base.copy(descripcion = "a".repeat(200)).valido)
        assertFalse(base.copy(descripcion = "a".repeat(201)).valido)
    }

    @Test fun cuentaCadaEmojiComoUnSoloCaracter() {
        assertFalse(base.copy(titulo = "😀😀").valido)
        assertTrue(base.copy(descripcion = "😀".repeat(200)).valido)
        assertFalse(base.copy(descripcion = "😀".repeat(201)).valido)
    }

    @Test fun exigeCategoriaYPrioridad() {
        assertFalse(base.copy(categoria = null).valido)
        assertFalse(base.copy(prioridad = null).valido)
        assertThrows(IllegalArgumentException::class.java) { Borrador().recordatorio(9) }
    }
}
