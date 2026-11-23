package com.example.primeraclaseprogramacionmobile.data.remoto

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import java.io.IOException
import java.io.InterruptedIOException
import kotlinx.coroutines.tasks.await

class FirestoreRecordatorios(private val firestore: FirebaseFirestore, uid: String,
    private val verificarSesion: () -> Unit,
) : ServicioRecordatorios {
    private val coleccion = firestore.collection("users").document(uid).collection("reminders")

    override suspend fun listar() = operacion {
        coleccion.get(Source.SERVER).await().documents.map { documento ->
            registroDesdeFirestore(documento.id, requireNotNull(documento.data))
        }
    }

    override suspend fun guardar(id: Long, registro: RecordatorioDto): RecordatorioDto = operacion {
        val documento = coleccion.document(id.toString())
        val campos = mapOf("titulo" to registro.titulo, "descripcion" to registro.descripcion,
            "categoria" to registro.categoria, "prioridad" to registro.prioridad,
            "estado" to registro.estado, "fecha" to registro.fecha)
        // Una transacción requiere confirmación del servidor. Room conserva la única cola offline.
        firestore.runTransaction { transaccion -> transaccion.set(documento, campos); Unit }.await()
        registro
    }

    override suspend fun eliminar(id: Long) = operacion {
        firestore.runTransaction { transaccion -> transaccion.delete(coleccion.document(id.toString())); Unit }.await()
    }

    private suspend fun <T> operacion(accion: suspend () -> T): T = try {
        accion()
    } catch (e: FirebaseFirestoreException) {
        throw when (e.code) {
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> { verificarSesion(); SolicitudRechazada() }
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.INVALID_ARGUMENT -> SolicitudRechazada()
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> InterruptedIOException()
            FirebaseFirestoreException.Code.UNAVAILABLE -> IOException("Sin conexión", e)
            else -> ServicioNoDisponible()
        }
    }
}

internal fun registroDesdeFirestore(id: String, campos: Map<String, Any?>): RecordatorioDto {
    fun texto(campo: String) = requireNotNull(campos[campo] as? String) { campo }
    val descripcion = campos["descripcion"]
    require(descripcion == null || descripcion is String)
    return RecordatorioDto(id.toLong(), texto("titulo"), descripcion as? String,
        texto("categoria"), texto("prioridad"), texto("estado"), requireNotNull(campos["fecha"] as? Long))
}
