package com.example.primeraclaseprogramacionmobile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun Context.permisoRedConcedido(): Boolean = Build.VERSION.SDK_INT < 37 ||
    checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED

internal suspend fun Context.faltaPermisoRedLocal(): Boolean {
    if (permisoRedConcedido()) return false
    val host = URI(BuildConfig.URL_SERVICIO).host.removeSurrounding("[", "]")
    return withContext(Dispatchers.IO) {
        if (host.endsWith(".local", ignoreCase = true)) return@withContext true
        try {
            InetAddress.getAllByName(host).any { ip ->
                ip.isSiteLocalAddress || ip.isLinkLocalAddress ||
                    (ip.address.size == 16 && ip.address[0].toInt() and 0xfe == 0xfc)
            }
        } catch (_: UnknownHostException) {
            // Retrofit informará la falta de conexión si el nombre continúa sin resolver.
            false
        }
    }
}
