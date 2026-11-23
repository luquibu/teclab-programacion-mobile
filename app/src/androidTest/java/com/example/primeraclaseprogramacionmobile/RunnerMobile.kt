package com.example.primeraclaseprogramacionmobile

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.test.runner.AndroidJUnitRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.example.primeraclaseprogramacionmobile.data.*
import com.example.primeraclaseprogramacionmobile.data.local.*
import com.example.primeraclaseprogramacionmobile.data.remoto.servicioRecordatorios
import com.example.primeraclaseprogramacionmobile.model.Borrador
import kotlinx.coroutines.*
import java.io.IOException

internal val Context.ajustesPrueba by preferencesDataStore("ajustes_de_prueba")

class RunnerMobile : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, AplicacionPrueba::class.java.name, context)
}

class AplicacionPrueba : Application(), FuenteRecordatorios {
    val base by lazy { Room.inMemoryDatabaseBuilder(this, BaseRecordatorios::class.java).build() }
    private val local by lazy { RepositorioLocal("cuenta-prueba", base, PreferenciasLocales(ajustesPrueba),
        servicioRecordatorios("http://127.0.0.1:1/"), CoroutineScope(Job().apply { cancel() })) }
    override val usaEmuladoresFirebase = false
    override val sesiones by lazy {
        object : Sesiones {
            override val activa = kotlinx.coroutines.flow.MutableStateFlow<CuentaActiva?>(
                CuentaActiva("cuenta-prueba", "prueba@example.test", repositorio))
            override suspend fun autenticar(correo: String, clave: String, registrar: Boolean) = Unit
            override fun cerrar() = Unit
            override fun comprobar() = Unit
        }
    }
    var fallaGuardado = false
    val repositorio: RepositorioRecordatorios by lazy {
        object : RepositorioRecordatorios by local {
            override suspend fun guardar(borrador: Borrador) {
                delay(350)
                if (fallaGuardado) { fallaGuardado = false; throw IOException("Error de prueba") }
                local.guardar(borrador)
            }
        }
    }
}

internal val appPrueba get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AplicacionPrueba

internal fun prepararEjemplos(conDatos: Boolean = true) = runBlocking {
    val app = appPrueba
    app.fallaGuardado = false
    app.base.clearAllTables()
    app.ajustesPrueba.edit { it.clear() }
    if (conDatos) for (r in DatosRecordatorios.recordatorios) {
        app.base.recordatorios().insertar(r.copy(fecha = 10 - r.id).fila("cuenta-prueba"))
    }
}
