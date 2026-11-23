package com.example.primeraclaseprogramacionmobile

import com.example.primeraclaseprogramacionmobile.data.remoto.registroDesdeFirestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DocumentoFirestoreTest {
    private val datos = mapOf("titulo" to "Prueba final", "categoria" to "estudio", "prioridad" to "alta",
        "estado" to "pendiente", "descripcion" to null, "fecha" to 1L)
    @Test fun leeElIdEstableDeLaRuta() {
        for (id in listOf("1", "42", Long.MAX_VALUE.toString())) {
            assertEquals(id, registroDesdeFirestore(id, datos).id.toString())
        }
    }
    @Test fun rechazaIdsQueCambiarianDeRutaOQuedanFueraDelDominio() {
        for (id in listOf("042", "+42", "0", "-1", "9223372036854775808", "otro")) {
            assertThrows(IllegalArgumentException::class.java) { registroDesdeFirestore(id, datos) }
        }
    }
    @Test fun tiposIncompatiblesSeClasificanComoDatosInvalidos() {
        for ((clave, valor) in mapOf("titulo" to 42, "fecha" to "hoy", "descripcion" to false)) {
            assertThrows(IllegalArgumentException::class.java) { registroDesdeFirestore("42", datos + (clave to valor)) }
        }
    }
}
