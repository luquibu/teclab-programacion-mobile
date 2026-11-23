package com.example.primeraclaseprogramacionmobile

import android.content.Context
import androidx.room.Room
import com.example.primeraclaseprogramacionmobile.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class CuentasRoomTest {
    @Test fun mismoIdDosCuentasYColasSeparadas() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(appPrueba, BaseRecordatorios::class.java).build()
        try {
            val dao = db.recordatorios()
            val a = FilaRecordatorio(12, "Lectura de A", null, "estudio", "alta", "pendiente", 1, true, uid = "A")
            dao.insertar(a)
            assertTrue(dao.todos("B").first().isEmpty())
            dao.insertar(a.copy(uid = "B", titulo = "Examen de B", pendiente = false))
            assertEquals("Lectura de A", dao.pendientes("A").single().titulo)
            assertTrue(dao.pendientes("B").isEmpty())
            assertEquals("Examen de B", dao.observar("B", 12).first()?.titulo)
            dao.eliminar(a)
            assertNotNull(dao.buscar("B", 12))
        } finally { db.close() }
    }

    @Test fun migracionConservaFilasAntiguasSinAsignarlesUnaCuenta() = runBlocking {
        val nombre = "migracion-" + UUID.randomUUID() + ".db"
        appPrueba.openOrCreateDatabase(nombre, Context.MODE_PRIVATE, null).use { antigua ->
            antigua.execSQL("""CREATE TABLE recordatorios(id INTEGER NOT NULL PRIMARY KEY, titulo TEXT NOT NULL,
                descripcion TEXT, categoria TEXT NOT NULL, prioridad TEXT NOT NULL, estado TEXT NOT NULL,
                fecha INTEGER NOT NULL, pendiente INTEGER NOT NULL, borrado INTEGER NOT NULL, revision INTEGER NOT NULL)""")
            antigua.execSQL("INSERT INTO recordatorios VALUES(8,'Recordatorio anterior',NULL,'estudio','alta','pendiente',1,1,0,1)")
            antigua.version = 1
        }
        val db = Room.databaseBuilder(appPrueba, BaseRecordatorios::class.java, nombre).addMigrations(migracionDeApi3).build()
        try {
            assertEquals("Recordatorio anterior", db.recordatorios().buscar("",8)?.titulo)
            assertTrue(db.recordatorios().todos("A").first().isEmpty())
            assertTrue(db.recordatorios().pendientes("A").isEmpty())
        } finally { db.close(); appPrueba.deleteDatabase(nombre) }
    }
}
