package com.example.primeraclaseprogramacionmobile

import androidx.datastore.preferences.core.*
import androidx.room.Room
import com.example.primeraclaseprogramacionmobile.data.*
import com.example.primeraclaseprogramacionmobile.data.local.*
import com.example.primeraclaseprogramacionmobile.data.remoto.RecordatorioDto
import com.example.primeraclaseprogramacionmobile.data.remoto.ServicioRecordatorios
import com.example.primeraclaseprogramacionmobile.data.remoto.SesionNoAutenticada
import com.example.primeraclaseprogramacionmobile.data.remoto.registroDesdeFirestore
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

    private open class NubeDePrueba : ServicioRecordatorios {
        val datos = mutableMapOf<Long, RecordatorioDto>()
        override suspend fun listar() = datos.values.toList()
        override suspend fun guardar(id: Long, registro: RecordatorioDto): RecordatorioDto {
            datos[id] = registro
            return registro
        }
        override suspend fun eliminar(id: Long) { datos.remove(id) }
    }

    private fun local(nube: ServicioRecordatorios, alcance: CoroutineScope = inactivo, uid: String = "cuenta-prueba",
                      renovarSesion: suspend () -> Unit = { error("Renovación inesperada") }) =
        RepositorioLocal(uid, base, PreferenciasLocales(appPrueba.ajustesPrueba), nube, alcance, renovarSesion)

    @Before fun iniciar() {
        base = Room.inMemoryDatabaseBuilder(appPrueba, BaseRecordatorios::class.java).build()
        servidor = MockWebServer().apply { start() }
        repo = RepositorioLocal("cuenta-prueba", base, PreferenciasLocales(appPrueba.ajustesPrueba),
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
        val local = RepositorioLocal("cuenta-prueba", base, PreferenciasLocales(appPrueba.ajustesPrueba),
            servicioRecordatorios("http://127.0.0.1:1/", 400), inactivo)
        local.guardar(nuevo)
        val id = local.registros().first().single().id
        local.modificar(id, nuevo.copy(titulo = "Repasar Retrofit"), EstadoRecordatorio.Completado)
        local.sincronizar()
        assertEquals(Sincronizacion.SinConexion, local.sincronizacion.value)
        assertEquals("Repasar Retrofit", local.registros(EstadoRecordatorio.Completado).first().single().titulo)
        local.eliminar(id)
        assertNull(local.observar(id).first())
        assertTrue(base.recordatorios().buscar("cuenta-prueba", id)!!.borrado)
        servidor.enqueue(MockResponse().setResponseCode(204))
        servidor.enqueue(json("[]"))
        repo.sincronizar()
        assertEquals("DELETE", servidor.takeRequest().method)
        assertNull(base.recordatorios().buscar("cuenta-prueba", id))
    }

    @Test fun edicionMientrasSeEnviaNoSePierde() = runBlocking {
        val enviando = CompletableDeferred<Unit>()
        val confirmar = CompletableDeferred<Unit>()
        val nube = object : NubeDePrueba() {
            override suspend fun guardar(id: Long, registro: RecordatorioDto): RecordatorioDto {
                enviando.complete(Unit)
                confirmar.await()
                return super.guardar(id, registro)
            }
        }
        repo = local(nube)
        repo.guardar(nuevo)
        val id = repo.registros().first().single().id
        coroutineScope {
            val enCurso = launch { repo.sincronizar() }
            withTimeout(3_000) { enviando.await() }
            repo.modificar(id, nuevo.copy(titulo = "Cambio posterior"), EstadoRecordatorio.Completado)
            confirmar.complete(Unit)
            enCurso.join()
        }
        assertFalse(repo.observar(id).first()!!.sincronizado)
        assertEquals(Sincronizacion.EnEspera, repo.sincronizacion.value)
        repo.sincronizar()
        assertEquals("Cambio posterior", repo.observar(id).first()!!.titulo)
        assertTrue(repo.observar(id).first()!!.sincronizado)
        assertEquals(Sincronizacion.AlDia, repo.sincronizacion.value)
    }

    @Test fun rechazoDeSesionDetieneLaColaTrasUnaSolaRenovacion() = runBlocking {
        var rechazar = true
        var renovaciones = 0
        val intentos = mutableListOf<Long>()
        val nube = object : NubeDePrueba() {
            override suspend fun guardar(id: Long, registro: RecordatorioDto): RecordatorioDto {
                intentos.add(id)
                if (rechazar) throw SesionNoAutenticada()
                return super.guardar(id, registro)
            }
        }
        repo = local(nube) { renovaciones++ }
        repeat(3) { repo.guardar(nuevo) }
        val iniciales = base.recordatorios().pendientes("cuenta-prueba").map { it.id }
        repo.sincronizar()
        assertEquals(listOf(iniciales.first(), iniciales.first()), intentos)
        assertEquals(1, renovaciones)
        assertEquals(Sincronizacion.SolicitudInvalida, repo.sincronizacion.value)
        assertEquals(iniciales, base.recordatorios().pendientes("cuenta-prueba").map { it.id })
        rechazar = false
        repo.sincronizar()
        assertEquals(iniciales.toSet(), nube.datos.keys)
        assertEquals(Sincronizacion.AlDia, repo.sincronizacion.value)
    }

    @Test fun reconexionComparteLaComprobacionYReintentaUnFalloTransitorio() = runBlocking {
        val tarea = Job()
        val iniciado = CompletableDeferred<Unit>()
        val continuar = CompletableDeferred<Unit>()
        var intentos = 0
        var lecturas = 0
        val nube = object : NubeDePrueba() {
            override suspend fun listar(): List<RecordatorioDto> { lecturas++; return super.listar() }
        }
        try {
            repo = local(nube, CoroutineScope(coroutineContext + tarea)) {
                if (++intentos == 1) {
                    iniciado.complete(Unit)
                    continuar.await()
                    throw IOException("La red todavía no responde")
                }
            }
            repo.solicitarComprobacion()
            withTimeout(3_000) { iniciado.await() }
            repeat(5) { repo.solicitarComprobacion() }
            continuar.complete(Unit)
            withTimeout(8_000) { repo.sincronizacion.first { it == Sincronizacion.AlDia } }
            assertEquals(2, intentos)
            assertEquals(1, lecturas)
        } finally { tarea.cancelAndJoin() }
    }

    @Test fun conservaElAvisoDeUnaEdicionSiLaRenovacionFalla() = runBlocking {
        val tarea = Job()
        val inicio = CompletableDeferred<Unit>()
        val continuar = CompletableDeferred<Unit>()
        var renovaciones = 0
        var rechazar = true
        val nube = object : NubeDePrueba() {
            override suspend fun guardar(id: Long, registro: RecordatorioDto): RecordatorioDto {
                if (rechazar) throw SesionNoAutenticada()
                return super.guardar(id, registro)
            }
        }
        try {
            repo = local(nube, CoroutineScope(coroutineContext + tarea)) {
                renovaciones++
                inicio.complete(Unit)
                continuar.await()
                throw IOException("Renovación interrumpida")
            }
            repo.guardar(nuevo)
            withTimeout(3_000) { inicio.await() }
            repo.guardar(nuevo.copy(titulo = "Trabajo posterior"))
            rechazar = false
            continuar.complete(Unit)
            withTimeout(8_000) { repo.sincronizacion.first { it == Sincronizacion.AlDia } }
            assertEquals(2, renovaciones)
            assertEquals(setOf(nuevo.titulo, "Trabajo posterior"), nube.datos.values.map { it.titulo }.toSet())
        } finally { tarea.cancelAndJoin() }
    }

    @Test fun cerrarDuranteLaRenovacionConservaLaColaDeLaCuentaAnterior() = runBlocking {
        val tarea = Job()
        val iniciado = CompletableDeferred<Unit>()
        val continuar = CompletableDeferred<Unit>()
        val nubeA = NubeDePrueba()
        val nubeB = NubeDePrueba()
        val fila = FilaRecordatorio(42, "Pendiente de A", null, "estudio", "alta", "pendiente", 1, true, uid = "A")
        base.recordatorios().insertar(fila)
        val a = local(nubeA, CoroutineScope(coroutineContext + tarea), "A") {
            iniciado.complete(Unit)
            withContext(NonCancellable) { continuar.await() }
        }
        try {
            a.solicitarComprobacion()
            withTimeout(3_000) { iniciado.await() }
            tarea.cancel()
            val b = local(nubeB, uid = "B")
            b.guardar(nuevo)
            b.sincronizar()
            continuar.complete(Unit)
            withTimeout(3_000) { tarea.join() }
            assertTrue(nubeA.datos.isEmpty())
            assertEquals(listOf(fila), base.recordatorios().pendientes("A"))
            assertEquals(nuevo.titulo, nubeB.datos.values.single().titulo)
            assertEquals(Sincronizacion.AlDia, b.sincronizacion.value)
        } finally { continuar.complete(Unit); tarea.cancelAndJoin() }
    }

    @Test fun sinPermisoLocalConservaLaColaYReanudaAlAutorizar() = runBlocking {
        var faltaPermiso = true
        val restringido = RepositorioLocal("cuenta-prueba", base, PreferenciasLocales(appPrueba.ajustesPrueba),
            servicioRecordatorios(servidor.url("/").toString()), inactivo) { faltaPermiso }
        restringido.guardar(nuevo)
        restringido.sincronizar()
        assertEquals(Sincronizacion.FaltaPermiso, restringido.sincronizacion.value)
        assertEquals(0, servidor.requestCount)
        val fila = base.recordatorios().pendientes("cuenta-prueba").single()
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

    @Test fun unaRutaFirestoreNoCanonicaConservaLaInstantaneaAnterior() = runBlocking {
        val original = FilaRecordatorio(42, "Título anterior", null, "estudio", "alta", "pendiente", 1, false, uid = "cuenta-prueba")
        base.recordatorios().insertar(original)
        val campos = mapOf("titulo" to "Cambio remoto", "categoria" to "estudio", "prioridad" to "alta",
            "estado" to "pendiente", "fecha" to 1L)
        repo = local(object : NubeDePrueba() {
            override suspend fun listar() = listOf("7", "042").map { registroDesdeFirestore(it, campos) }
        })
        repo.sincronizar()
        assertEquals(Sincronizacion.DatosInvalidos, repo.sincronizacion.value)
        assertEquals(listOf(original), base.recordatorios().confirmados("cuenta-prueba"))
    }

    @Test fun exitoSinCuerpoConservaPendientesYPermiteReintentar() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(204))
        repo.sincronizar()
        assertEquals(Sincronizacion.DatosInvalidos, repo.sincronizacion.value)
        repo.guardar(nuevo)
        servidor.enqueue(MockResponse().setResponseCode(204))
        repo.sincronizar()
        assertFalse(repo.registros().first().single().sincronizado)
        val fila = base.recordatorios().pendientes("cuenta-prueba").single()
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
