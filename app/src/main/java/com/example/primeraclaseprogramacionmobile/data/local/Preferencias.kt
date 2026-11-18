package com.example.primeraclaseprogramacionmobile.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.map

enum class Orden { Recientes, Titulo, Prioridad }
data class Preferencias(val claro: Boolean = false, val mostrarCompletados: Boolean = true, val orden: Orden = Orden.Recientes)

val Context.almacenPreferencias by preferencesDataStore(
    name = "preferencias", corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class PreferenciasLocales(private val almacen: DataStore<Preferences>) {
    private val tema = booleanPreferencesKey("tema_claro")
    private val completados = booleanPreferencesKey("mostrar_completados")
    private val orden = stringPreferencesKey("orden")

    val valores: Flow<Preferencias> = almacen.data.retryWhen { causa, _ ->
        if (causa !is IOException) return@retryWhen false
        emit(emptyPreferences())
        delay(500)
        true
    }.map { datos ->
        val valores = datos.asMap()
        Preferencias(valores[tema] as? Boolean ?: false, valores[completados] as? Boolean ?: true,
            Orden.entries.find { it.name == valores[orden] } ?: Orden.Recientes)
    }

    suspend fun cambiarTema(claro: Boolean) { almacen.edit { it[tema] = claro } }
    suspend fun cambiarCompletados(mostrar: Boolean) { almacen.edit { it[completados] = mostrar } }
    suspend fun cambiarOrden(valor: Orden) { almacen.edit { it[orden] = valor.name } }
}
