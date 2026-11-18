package com.example.primeraclaseprogramacionmobile

import androidx.datastore.preferences.core.*
import androidx.room.Room
import com.example.primeraclaseprogramacionmobile.data.*
import com.example.primeraclaseprogramacionmobile.data.local.*
import com.example.primeraclaseprogramacionmobile.data.remoto.servicioRecordatorios
import com.example.primeraclaseprogramacionmobile.model.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.mockwebserver.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RepositorioLocalTest {
    private lateinit var base: BaseRecordatorios
    private lateinit var servidor: MockWebServer
    private lateinit var repo: RepositorioLocal
    private val nuevo = Borrador("Repasar Room", categoria = Categoria.Estudio, prioridad = Prioridad.Media)
    private val inactivo = CoroutineScope(Job().also { it.cancel() })

    @Before fun iniciar() {
        base = Room.inMemoryDatabaseBuilder(appPrueba, BaseRecordatorios::class.java).build()
        servidor = MockWebServer().apply { start() }
        repo = RepositorioLocal(base, PreferenciasLocales(appPrueba.ajustesPrueba),
            servicioRecordatorios(servidor.url("/").toString(), 600), inactivo)
    }

    @After fun cerrar() { servidor.shutdown(); base.close() }

    private fun json(cuerpo: String) = MockResponse().setBody(cuerpo).setHeader("Content-Type", "application/json")

    @Test fun cierreDeRedDespuesDeGuardarReintentaElMismoId() = runBlocking {
        var guardado: String? = null
        val rutas = mutableListOf<String>()
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.method == "GET") return json("[" + guardado + "]")
                rutas.add(requireNotNull(request.path))
                guardado = request.body.readUtf8()
                return if (rutas.size == 1) MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
                    else json(requireNotNull(guardado))
            }
        }
        repo.guardar(nuevo)
        val id = repo.registros().first().single().id
        repo.sincronizar()
        assertFalse(repo.observar(id).first()!!.sincronizado)
        coroutineScope { repeat(3) { launch { repo.sincronizar() } } }
        assertEquals(listOf("/recordatorios/" + id, "/recordatorios/" + id), rutas)
        assertTrue(repo.registros().first().single().sincronizado)
    }

    @Test fun conservaAltaDuranteErroresHttpYEsperaAgotada() = runBlocking {
        repo.guardar(nuevo)
        for ((codigo, esperado) in listOf(400 to Sincronizacion.SolicitudInvalida, 503 to Sincronizacion.ErrorServidor)) {
            servidor.enqueue(MockResponse().setResponseCode(codigo))
            if (codigo == 400) servidor.enqueue(json("[]"))
            repo.sincronizar()
            assertEquals(esperado, repo.sincronizacion.value)
            assertFalse(repo.registros().first().single().sincronizado)
        }
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        repo.sincronizar()
        assertEquals(Sincronizacion.TiempoAgotado, repo.sincronizacion.value)
        assertEquals(nuevo.titulo, repo.registros().first().single().titulo)
    }

    @Test fun crudSinServidorYBajaConfirmadaAlReconectar() = runBlocking {
        val local = RepositorioLocal(base, PreferenciasLocales(appPrueba.ajustesPrueba),
            servicioRecordatorios("http://127.0.0.1:1/", 400), inactivo)
        local.guardar(nuevo)
        val id = local.registros().first().single().id
        local.modificar(id, nuevo.copy(titulo = "Repasar Retrofit"), EstadoRecordatorio.Completado)
        local.sincronizar()
        assertEquals(Sincronizacion.SinConexion, local.sincronizacion.value)
        assertEquals("Repasar Retrofit", local.registros(EstadoRecordatorio.Completado).first().single().titulo)
        local.eliminar(id)
        assertNull(local.observar(id).first())
        assertTrue(base.recordatorios().buscar(id)!!.borrado)
        servidor.enqueue(MockResponse().setResponseCode(204))
        servidor.enqueue(json("[]"))
        repo.sincronizar()
        assertEquals("DELETE", servidor.takeRequest().method)
        assertNull(base.recordatorios().buscar(id))
    }

    @Test fun edicionMientrasSeEnviaNoSePierde() = runBlocking {
        repo.guardar(nuevo)
        val id = repo.registros().first().single().id
        var remoto = ""
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.method == "GET") return json("[" + remoto + "]")
                remoto = request.body.readUtf8()
                return json(remoto).setBodyDelay(100, TimeUnit.MILLISECONDS)
            }
        }
        coroutineScope {
            val enCurso = launch(Dispatchers.IO) { repo.sincronizar() }
            assertNotNull(servidor.takeRequest(3, TimeUnit.SECONDS))
            repo.modificar(id, nuevo.copy(titulo = "Cambio posterior"), EstadoRecordatorio.Completado)
            enCurso.join()
        }
        assertFalse(repo.observar(id).first()!!.sincronizado)
        repo.sincronizar()
        assertEquals("Cambio posterior", repo.observar(id).first()!!.titulo)
        assertTrue(repo.observar(id).first()!!.sincronizado)
    }

    @Test fun sinPermisoLocalConservaLaColaYReanudaAlAutorizar() = runBlocking {
        var faltaPermiso = true
        val restringido = RepositorioLocal(base, PreferenciasLocales(appPrueba.ajustesPrueba),
            servicioRecordatorios(servidor.url("/").toString()), inactivo) { faltaPermiso }
        restringido.guardar(nuevo)
        restringido.sincronizar()
        assertEquals(Sincronizacion.FaltaPermiso, restringido.sincronizacion.value)
        assertEquals(0, servidor.requestCount)
        val fila = base.recordatorios().pendientes().single()
        val dato = kotlinx.serialization.json.Json.encodeToString(com.example.primeraclaseprogramacionmobile.data.remoto.RecordatorioDto.serializer(), fila.dto())
        servidor.enqueue(json(dato)); servidor.enqueue(json("[" + dato + "]"))
        faltaPermiso = false
        restringido.sincronizar()
        assertTrue(restringido.registros().first().single().sincronizado)
    }

    @Test fun descargaValidadaYRespuestaInvalidaConservaRoom() = runBlocking {
        val dato = """{"id":71,"titulo":"Revisar informe","categoria":"entregas","prioridad":"alta","estado":"pendiente","fecha":1000}"""
        servidor.enqueue(json("[" + dato + "]")); repo.sincronizar()
        assertTrue(repo.observar(71).first()!!.sincronizado)
        servidor.enqueue(json("[" + dato.replace("entregas", "inexistente") + "]")); repo.sincronizar()
        assertEquals(Sincronizacion.DatosInvalidos, repo.sincronizacion.value)
        assertEquals("Revisar informe", repo.registros().first().single().titulo)
        servidor.enqueue(json("[]")); repo.sincronizar()
        assertTrue(repo.registros().first().isEmpty())
    }

    @Test fun exitoSinCuerpoConservaPendientesYPermiteReintentar() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(204))
        repo.sincronizar()
        assertEquals(Sincronizacion.DatosInvalidos, repo.sincronizacion.value)
        repo.guardar(nuevo)
        servidor.enqueue(MockResponse().setResponseCode(204))
        repo.sincronizar()
        assertFalse(repo.registros().first().single().sincronizado)
        val fila = base.recordatorios().pendientes().single()
        val dato = kotlinx.serialization.json.Json.encodeToString(com.example.primeraclaseprogramacionmobile.data.remoto.RecordatorioDto.serializer(), fila.dto())
        servidor.enqueue(json(dato)); servidor.enqueue(json("[" + dato + "]"))
        repo.sincronizar()
        assertTrue(repo.registros().first().single().sincronizado)
    }

    @Test fun preferenciasAusentesDesconocidasYErrorDeLectura() = runBlocking {
        val datos = appPrueba.ajustesPrueba
        datos.edit { it.clear() }
        assertEquals(Preferencias(), repo.preferencias.first())
        repo.tema(true); repo.mostrarCompletados(false); repo.ordenar(Orden.Titulo)
        assertEquals(Preferencias(true, false, Orden.Titulo), repo.preferencias.first())
        datos.edit { it[stringPreferencesKey("orden")] = "anterior" }
        assertEquals(Preferencias(true, false, Orden.Recientes), repo.preferencias.first())
        val ilegible = object : androidx.datastore.core.DataStore<Preferences> {
            var primera = true
            override val data = flow {
                if (primera) { primera = false; throw IOException("Lectura interrumpida") }
                emitAll(datos.data)
            }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = error("Solo lectura")
        }
        assertEquals(listOf(Preferencias(), Preferencias(true, false, Orden.Recientes)),
            withTimeout(4_000) { PreferenciasLocales(ilegible).valores.take(2).toList() })
    }
}
