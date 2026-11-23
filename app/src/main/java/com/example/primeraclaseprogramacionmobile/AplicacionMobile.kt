package com.example.primeraclaseprogramacionmobile

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import androidx.room.Room
import com.example.primeraclaseprogramacionmobile.data.*
import com.example.primeraclaseprogramacionmobile.data.local.*
import com.example.primeraclaseprogramacionmobile.data.remoto.FirestoreRecordatorios
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings

interface FuenteRecordatorios {
    val sesiones: Sesiones
    val usaEmuladoresFirebase: Boolean
}

class AplicacionMobile : Application(), FuenteRecordatorios {
    override val usaEmuladoresFirebase = BuildConfig.DEBUG
    override val sesiones: Sesiones by lazy {
        val auth = FirebaseAuth.getInstance()
        val nube = FirebaseFirestore.getInstance()
        nube.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
        if (usaEmuladoresFirebase) {
            auth.useEmulator("10.0.2.2", 9099)
            nube.useEmulator("10.0.2.2", 8184)
        }
        val base = Room.databaseBuilder(this, BaseRecordatorios::class.java, "recordatorios.db")
            .addMigrations(migracionDeApi3).build()
        val preferencias = PreferenciasLocales(almacenPreferencias)
        SesionesFirebase(auth) { uid, alcance, renovar ->
            RepositorioLocal(uid, base, preferencias, FirestoreRecordatorios(nube, uid), alcance, renovar) {
                usaEmuladoresFirebase && !permisoRedConcedido()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val cuentas = sesiones
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { cuentas.comprobar() }
            },
        )
    }
}
