package com.example.primeraclaseprogramacionmobile.data

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.example.primeraclaseprogramacionmobile.data.local.*
import com.example.primeraclaseprogramacionmobile.data.remoto.ServicioRecordatorios
import com.example.primeraclaseprogramacionmobile.model.*
import java.io.IOException
import java.io.InterruptedIOException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
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
    private val faltaPermisoRed: suspend () -> Boolean = { false },
) : RepositorioRecordatorios {
    private val dao = base.recordatorios()
    private val solicitudes = Channel<Unit>(Channel.CONFLATED)
    private val turno = Mutex()
    private val estadoRed = MutableStateFlow(Sincronizacion.EnEspera)
    override val sincronizacion = estadoRed.asStateFlow()
    override val preferencias = config.valores
    override suspend fun tema(claro: Boolean) = config.cambiarTema(claro)
    override suspend fun mostrarCompletados(mostrar: Boolean) = config.cambiarCompletados(mostrar)
    override suspend fun ordenar(orden: Orden) = config.cambiarOrden(orden)

    init {
        require(uid.isNotBlank())
        alcance.launch { for (solicitud in solicitudes) sincronizar() }
    }

    override fun solicitarSincronizacion() { solicitudes.trySend(Unit) }

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
            base.withTransaction { operacion() }
        } catch (e: SQLiteException) {
            throw IOException("No se pudo escribir en el dispositivo", e)
        }
        solicitarSincronizacion()
    }

    internal suspend fun sincronizar() = turno.withLock {
        if (faltaPermisoRed()) {
            estadoRed.value = Sincronizacion.FaltaPermiso
            return@withLock
        }
        estadoRed.value = Sincronizacion.EnCurso
        estadoRed.value = try {
            var rechazado = false
            for (fila in dao.pendientes(uid)) {
                try {
                    enviar(fila)
                } catch (_: SolicitudRechazada) {
                    rechazado = true
                }
            }
            val recibidos = servicio.listar().map { it.fila(uid) }
            require(recibidos.distinctBy { it.id }.size == recibidos.size)
            base.withTransaction {
                for (fila in recibidos) {
                    val local = dao.buscar(uid, fila.id)
                    if (local == null) dao.insertar(fila)
                    else if (!local.pendiente) dao.actualizar(fila.copy(revision = local.revision))
                }
                // La consulta remota devuelve la colección completa. Una ausencia confirma una baja remota.
                val remotos = recibidos.mapTo(hashSetOf()) { it.id }
                dao.confirmados(uid).filter { it.id !in remotos }.forEach { dao.eliminar(it) }
            }
            if (rechazado) Sincronizacion.SolicitudInvalida else Sincronizacion.AlDia
        } catch (_: SolicitudRechazada) {
            Sincronizacion.SolicitudInvalida
        } catch (_: ServicioNoDisponible) {
            Sincronizacion.ErrorServidor
        } catch (_: InterruptedIOException) {
            Sincronizacion.TiempoAgotado
        } catch (_: IOException) {
            Sincronizacion.SinConexion
        } catch (_: SerializationException) {
            Sincronizacion.DatosInvalidos
        } catch (_: IllegalArgumentException) {
            Sincronizacion.DatosInvalidos
        } catch (_: SQLiteException) {
            Sincronizacion.ErrorLocal
        }
    }

    private suspend fun enviar(fila: FilaRecordatorio) {
        if (fila.borrado) {
            servicio.eliminar(fila.id)
            dao.confirmarBaja(uid, fila.id, fila.revision)
        } else {
            val confirmado = servicio.guardar(fila.id, fila.dto()).fila(uid)
            require(confirmado.id == fila.id)
            // Una edición local durante el envío debe permanecer pendiente.
            base.withTransaction {
                val actual = dao.buscar(uid, fila.id)
                if (actual?.revision == fila.revision) dao.actualizar(confirmado.copy(revision = fila.revision))
            }
        }
    }
}
