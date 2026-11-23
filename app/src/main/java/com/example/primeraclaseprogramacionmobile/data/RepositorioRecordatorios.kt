package com.example.primeraclaseprogramacionmobile.data

import com.example.primeraclaseprogramacionmobile.data.local.Orden
import com.example.primeraclaseprogramacionmobile.data.local.Preferencias
import com.example.primeraclaseprogramacionmobile.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class Sincronizacion { FaltaPermiso, EnEspera, EnCurso, AlDia, SinConexion, TiempoAgotado, SolicitudInvalida, ErrorServidor, DatosInvalidos, ErrorLocal }

interface RepositorioRecordatorios {
    val preferencias: Flow<Preferencias>
    val sincronizacion: StateFlow<Sincronizacion>
    fun registros(estado: EstadoRecordatorio? = null): Flow<List<Recordatorio>>
    fun observar(id: Long): Flow<Recordatorio?>
    suspend fun guardar(borrador: Borrador)
    suspend fun modificar(id: Long, borrador: Borrador, estado: EstadoRecordatorio)
    suspend fun eliminar(id: Long)
    suspend fun tema(claro: Boolean)
    suspend fun mostrarCompletados(mostrar: Boolean)
    suspend fun ordenar(orden: Orden)
    fun solicitarSincronizacion()
    /** Comprueba la sesión antes de sincronizar al abrir la app o recuperar la red. */
    fun solicitarComprobacion()
}
