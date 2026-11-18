package com.example.primeraclaseprogramacionmobile

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import androidx.room.Room
import com.example.primeraclaseprogramacionmobile.data.*
import com.example.primeraclaseprogramacionmobile.data.local.*
import com.example.primeraclaseprogramacionmobile.data.remoto.servicioRecordatorios
import kotlinx.coroutines.*

interface FuenteRecordatorios { val repositorio: RepositorioRecordatorios }

class AplicacionMobile : Application(), FuenteRecordatorios {
    override val repositorio by lazy {
        RepositorioLocal(Room.databaseBuilder(this, BaseRecordatorios::class.java, "recordatorios.db").build(),
            PreferenciasLocales(almacenPreferencias), servicioRecordatorios(BuildConfig.URL_SERVICIO),
            CoroutineScope(SupervisorJob() + Dispatchers.IO), this::faltaPermisoRedLocal)
    }

    override fun onCreate() {
        super.onCreate()
        repositorio.solicitarSincronizacion()
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { repositorio.solicitarSincronizacion() }
            },
        )
    }
}
