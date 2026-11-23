package com.example.primeraclaseprogramacionmobile.data

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.example.primeraclaseprogramacionmobile.data.local.*
import com.example.primeraclaseprogramacionmobile.data.remoto.ServicioRecordatorios
import com.example.primeraclaseprogramacionmobile.data.remoto.SesionNoAutenticada
import com.example.primeraclaseprogramacionmobile.model.*
import java.io.IOException
import java.io.InterruptedIOException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import com.example.primeraclaseprogramacionmobile.data.remoto.SolicitudRechazada
import com.example.primeraclaseprogramacionmobile.data.remoto.ServicioNoDisponible

class RepositorioLocal(
    private val uid: String,
    private val base: BaseRecordatorios,
    private val config: PreferenciasLocales,
    private val servicio: ServicioRecordatorios,
    alcance: CoroutineScope,
    private val renovarSesion: suspend () -> Unit = { throw SesionNoAutenticada() },
    private val faltaPermisoRed: suspend () -> Boolean = { false },
) : RepositorioRecordatorios {
    private enum class Solicitud { Sincronizar, Comprobar }
    private val dao = base.recordatorios()
    private val avisos = Channel<Unit>(Channel.CONFLATED)
    private val solicitudes = mutableSetOf<Solicitud>()
    private var comprobando = false
    private val turno = Mutex()
    private val estadoRed = MutableStateFlow(Sincronizacion.EnEspera)
    override val sincronizacion = estadoRed.asStateFlow()
    override val preferencias = config.valores
    override suspend fun tema(claro: Boolean) = config.cambiarTema(claro)
    override suspend fun mostrarCompletados(mostrar: Boolean) = config.cambiarCompletados(mostrar)
    override suspend fun ordenar(orden: Orden) = config.cambiarOrden(orden)

    init {
        require(uid.isNotBlank())
        alcance.launch {
            for (aviso in avisos) {
                val solicitud = tomarSolicitud() ?: continue
                sincronizar(solicitud == Solicitud.Comprobar)
            }
        }
    }

    @Synchronized
    private fun tomarSolicitud(): Solicitud? {
        if (solicitudes.isEmpty()) return null
        comprobando = Solicitud.Comprobar in solicitudes
        solicitudes.clear()
        return if (comprobando) Solicitud.Comprobar else Solicitud.Sincronizar
    }

    @Synchronized
    private fun encolar(solicitud: Solicitud) {
        if (solicitud == Solicitud.Comprobar && comprobando) return
        if (solicitudes.add(solicitud)) avisos.trySend(Unit)
    }

    override fun solicitarSincronizacion() = encolar(Solicitud.Sincronizar)
    override fun solicitarComprobacion() = encolar(Solicitud.Comprobar)

    override fun registros(estado: EstadoRecordatorio?): Flow<List<Recordatorio>> {
        val filas = if (estado == null) dao.todos(uid) else dao.porEstado(uid, estado.clave)
        return filas.map { datos -> datos.map { it.dominio() } }
    }

    override fun observar(id: Long) = dao.observar(uid, id).map { it?.dominio() }

    override suspend fun guardar(borrador: Borrador) = escribir {
        val id = (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE).coerceAtLeast(1L)
        dao.insertar(borrador.recordatorio(id).copy(fecha = System.currentTimeMillis()).fila(uid))
    }

    override suspend fun modificar(id: Long, borrador: Borrador, estado: EstadoRecordatorio) = escribir {
        val anterior = dao.buscar(uid, id)?.takeUnless { it.borrado } ?: throw IOException("El registro ya no existe")
        val nuevo = borrador.recordatorio(id).copy(estado = estado, fecha = anterior.fecha)
        dao.actualizar(nuevo.fila(uid, anterior.revision + 1))
    }

    override suspend fun eliminar(id: Long) = escribir {
        val anterior = dao.buscar(uid, id) ?: return@escribir
        dao.actualizar(anterior.copy(pendiente = true, borrado = true, revision = anterior.revision + 1))
    }

    private suspend fun escribir(operacion: suspend () -> Unit) {
        try {
            base.withTransaction {
                operacion()
                if (estadoRed.value == Sincronizacion.AlDia) estadoRed.value = Sincronizacion.EnEspera
            }
        } catch (e: SQLiteException) {
            throw IOException("No se pudo escribir en el dispositivo", e)
        }
        solicitarSincronizacion()
    }

    internal suspend fun sincronizar(comprobar: Boolean = false) = turno.withLock {
        try {
            if (faltaPermisoRed()) {
                estadoRed.value = Sincronizacion.FaltaPermiso
                return@withLock
            }
            estadoRed.value = Sincronizacion.EnCurso
            if (comprobar) renovarConReintento()
            try {
                sincronizarCambios()
            } catch (_: SesionNoAutenticada) {
                if (!comprobar) {
                    synchronized(this@RepositorioLocal) {
                        comprobando = true
                        solicitudes.remove(Solicitud.Comprobar)
                    }
                    renovarConReintento()
                }
                currentCoroutineContext().ensureActive()
                // La segunda pasada relee Room. Las nuevas escrituras conservan su aviso si la renovación falla.
                synchronized(this@RepositorioLocal) { solicitudes.remove(Solicitud.Sincronizar) }
                sincronizarCambios()
            }
        } catch (_: SesionNoAutenticada) {
            estadoRed.value = Sincronizacion.SolicitudInvalida
        } catch (_: SolicitudRechazada) {
            estadoRed.value = Sincronizacion.SolicitudInvalida
        } catch (_: ServicioNoDisponible) {
            estadoRed.value = Sincronizacion.ErrorServidor
        } catch (_: InterruptedIOException) {
            estadoRed.value = Sincronizacion.TiempoAgotado
        } catch (_: IOException) {
            estadoRed.value = Sincronizacion.SinConexion
        } catch (_: SerializationException) {
            estadoRed.value = Sincronizacion.DatosInvalidos
        } catch (_: IllegalArgumentException) {
            estadoRed.value = Sincronizacion.DatosInvalidos
        } catch (_: SQLiteException) {
            estadoRed.value = Sincronizacion.ErrorLocal
        } finally {
            synchronized(this@RepositorioLocal) { comprobando = false }
        }
    }

    private suspend fun renovarConReintento() {
        try {
            renovarSesion()
        } catch (e: IOException) {
            if (e is SesionNoAutenticada) throw e
            delay(1_500)
            renovarSesion()
        }
        currentCoroutineContext().ensureActive()
    }

    private suspend fun sincronizarCambios() {
        var rechazado = false
        for (fila in dao.pendientes(uid)) {
            currentCoroutineContext().ensureActive()
            try {
                enviar(fila)
            } catch (_: SolicitudRechazada) {
                rechazado = true
            }
        }
        currentCoroutineContext().ensureActive()
        val recibidos = servicio.listar().map { it.fila(uid) }
        currentCoroutineContext().ensureActive()
        require(recibidos.distinctBy { it.id }.size == recibidos.size)
        base.withTransaction {
            for (fila in recibidos) {
                val local = dao.buscar(uid, fila.id)
                if (local == null) dao.insertar(fila)
                else if (!local.pendiente) dao.actualizar(fila.copy(revision = local.revision))
            }
            val remotos = recibidos.mapTo(hashSetOf()) { it.id }
            dao.confirmados(uid).filter { it.id !in remotos }.forEach { dao.eliminar(it) }
            estadoRed.value = when {
                rechazado -> Sincronizacion.SolicitudInvalida
                dao.hayPendientes(uid) -> Sincronizacion.EnEspera
                else -> Sincronizacion.AlDia
            }
        }
    }

    private suspend fun enviar(fila: FilaRecordatorio) {
        if (fila.borrado) {
            servicio.eliminar(fila.id)
            currentCoroutineContext().ensureActive()
            dao.confirmarBaja(uid, fila.id, fila.revision)
        } else {
            val confirmado = servicio.guardar(fila.id, fila.dto()).fila(uid)
            currentCoroutineContext().ensureActive()
            require(confirmado.id == fila.id)
            // Una edición local durante el envío debe permanecer pendiente.
            base.withTransaction {
                val actual = dao.buscar(uid, fila.id)
                if (actual?.revision == fila.revision) dao.actualizar(confirmado.copy(revision = fila.revision))
            }
        }
    }
}
