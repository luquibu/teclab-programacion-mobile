package com.example.primeraclaseprogramacionmobile.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CuentaActiva(val uid: String, val correo: String, val repositorio: RepositorioRecordatorios)

enum class MotivoAcceso { Conexion, ClaveDebil, CorreoRegistrado, Credenciales }
class ErrorAcceso(val motivo: MotivoAcceso) : Exception()

interface Sesiones {
    val activa: StateFlow<CuentaActiva?>
    suspend fun autenticar(correo: String, clave: String, registrar: Boolean)
    fun cerrar()
    fun comprobar()
}

class SesionesFirebase(
    private val auth: FirebaseAuth,
    private val repositorio: (String, CoroutineScope) -> RepositorioRecordatorios,
) : Sesiones {
    private val actual = MutableStateFlow<CuentaActiva?>(null)
    override val activa = actual.asStateFlow()
    private var trabajo: CoroutineScope? = null

    init {
        cambiar(auth.currentUser)
        auth.addAuthStateListener { cambiar(it.currentUser) }
    }

    @Synchronized
    private fun cambiar(usuario: com.google.firebase.auth.FirebaseUser?) {
        if (actual.value?.uid == usuario?.uid) return
        trabajo?.cancel()
        actual.value = null
        trabajo = null
        if (usuario != null) {
            val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            trabajo = alcance
            val repo = repositorio(usuario.uid, alcance)
            actual.value = CuentaActiva(usuario.uid, usuario.email.orEmpty(), repo)
            repo.solicitarSincronizacion()
        }
    }

    override suspend fun autenticar(correo: String, clave: String, registrar: Boolean) {
        try {
            val resultado = if (registrar) auth.createUserWithEmailAndPassword(correo, clave).await()
                else auth.signInWithEmailAndPassword(correo, clave).await()
            cambiar(resultado.user)
        } catch (e: com.google.firebase.FirebaseException) {
            throw ErrorAcceso(when (e) {
                is FirebaseNetworkException -> MotivoAcceso.Conexion
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
        val usuario = auth.currentUser ?: return
        trabajo?.launch {
            try {
                usuario.getIdToken(true).await()
                if (actual.value?.uid == usuario.uid) actual.value?.repositorio?.solicitarSincronizacion()
            } catch (_: FirebaseNetworkException) {
                // Room sigue disponible si la sesión no puede verificarse sin conexión.
            } catch (e: FirebaseAuthException) {
                if (e.errorCode !in setOf("ERROR_USER_DISABLED", "ERROR_USER_NOT_FOUND", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN")) return@launch
                synchronized(this@SesionesFirebase) {
                    if (actual.value?.uid == usuario.uid) cerrar()
                }
            } catch (_: com.google.firebase.FirebaseException) {
                // Un error transitorio del servicio se vuelve a comprobar al recuperar la red.
            }
        }
    }
}
