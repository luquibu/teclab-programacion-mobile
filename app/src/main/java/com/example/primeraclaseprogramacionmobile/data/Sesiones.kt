package com.example.primeraclaseprogramacionmobile.data

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.example.primeraclaseprogramacionmobile.data.remoto.ServicioNoDisponible
import com.example.primeraclaseprogramacionmobile.data.remoto.SesionNoAutenticada
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class CuentaActiva(val uid: String, val correo: String, val repositorio: RepositorioRecordatorios)

enum class MotivoAcceso { Conexion, ClaveDebil, CorreoRegistrado, Credenciales, Espera }
class ErrorAcceso(val motivo: MotivoAcceso) : Exception()

interface Sesiones {
    val activa: StateFlow<CuentaActiva?>
    suspend fun autenticar(correo: String, clave: String, registrar: Boolean)
    fun cerrar()
    fun comprobar()
}

class SesionesFirebase(
    private val auth: FirebaseAuth,
    private val repositorio: (String, CoroutineScope, suspend () -> Unit) -> RepositorioRecordatorios,
) : Sesiones {
    private val actual = MutableStateFlow<CuentaActiva?>(null)
    override val activa = actual.asStateFlow()
    private var trabajo: CoroutineScope? = null

    init {
        leerCuentaActual()
        auth.addAuthStateListener { leerCuentaActual() }
    }

    @Synchronized
    private fun leerCuentaActual() = cambiar(auth.currentUser)

    @Synchronized
    private fun cambiar(usuario: FirebaseUser?) {
        if (actual.value?.uid == usuario?.uid) return
        trabajo?.cancel()
        actual.value = null
        trabajo = null
        if (usuario != null) {
            val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            trabajo = alcance
            val repo = repositorio(usuario.uid, alcance) { renovar(usuario, alcance) }
            actual.value = CuentaActiva(usuario.uid, usuario.email.orEmpty(), repo)
            repo.solicitarSincronizacion()
        }
    }

    override suspend fun autenticar(correo: String, clave: String, registrar: Boolean) {
        try {
            if (registrar) auth.createUserWithEmailAndPassword(correo, clave).await()
                else auth.signInWithEmailAndPassword(correo, clave).await()
            leerCuentaActual()
        } catch (e: FirebaseException) {
            throw ErrorAcceso(when (e) {
                is FirebaseNetworkException -> MotivoAcceso.Conexion
                is FirebaseTooManyRequestsException -> MotivoAcceso.Espera
                is FirebaseAuthWeakPasswordException -> MotivoAcceso.ClaveDebil
                is FirebaseAuthUserCollisionException -> MotivoAcceso.CorreoRegistrado
                else -> MotivoAcceso.Credenciales
            })
        }
    }

    @Synchronized
    override fun cerrar() {
        cambiar(null)
        auth.signOut()
    }

    @Synchronized
    override fun comprobar() {
        actual.value?.repositorio?.solicitarComprobacion()
    }

    @Synchronized
    private fun exigirSesion(usuario: FirebaseUser, alcance: CoroutineScope) {
        if (trabajo !== alcance || auth.currentUser?.uid != usuario.uid) {
            throw CancellationException("La sesión cambió")
        }
    }

    private suspend fun renovar(usuario: FirebaseUser, alcance: CoroutineScope) {
        exigirSesion(usuario, alcance)
        try {
            usuario.getIdToken(true).await()
        } catch (e: FirebaseNetworkException) {
            throw IOException("Sin conexión", e)
        } catch (e: FirebaseAuthException) {
            if (e.errorCode in setOf("ERROR_USER_DISABLED", "ERROR_USER_NOT_FOUND", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN")) {
                synchronized(this) {
                    exigirSesion(usuario, alcance)
                    cerrar()
                }
                throw SesionNoAutenticada()
            }
            throw ServicioNoDisponible()
        } catch (_: FirebaseException) {
            throw ServicioNoDisponible()
        }
        exigirSesion(usuario, alcance)
    }
}
