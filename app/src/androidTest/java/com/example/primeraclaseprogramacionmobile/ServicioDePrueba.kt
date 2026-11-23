package com.example.primeraclaseprogramacionmobile.data.remoto

import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*

private interface ApiHttp {
    @GET("recordatorios") suspend fun listar(): retrofit2.Response<List<RecordatorioDto>>
    @PUT("recordatorios/{id}") suspend fun guardar(@Path("id") id: Long, @Body registro: RecordatorioDto): retrofit2.Response<RecordatorioDto>
    @DELETE("recordatorios/{id}") suspend fun eliminar(@Path("id") id: Long): retrofit2.Response<Unit>
}

private val formato = Json { ignoreUnknownKeys = true }

fun servicioRecordatorios(url: String, esperaMs: Long = 4_000): ServicioRecordatorios {
    val cliente = OkHttpClient.Builder().callTimeout(esperaMs, TimeUnit.MILLISECONDS)
        .readTimeout(esperaMs, TimeUnit.MILLISECONDS).retryOnConnectionFailure(false).build()
    val api = Retrofit.Builder().baseUrl(url).client(cliente)
        .addConverterFactory(formato.asConverterFactory("application/json".toMediaType()))
        .build().create(ApiHttp::class.java)
    return object : ServicioRecordatorios {
        override suspend fun listar() = api.listar().valor()
        override suspend fun guardar(id: Long, registro: RecordatorioDto) = api.guardar(id, registro).valor()
        override suspend fun eliminar(id: Long) {
            val respuesta = api.eliminar(id)
            if (respuesta.code() != 404) respuesta.verificar()
        }
    }
}

private fun retrofit2.Response<*>.verificar() {
    if (code() in 400..499) throw SolicitudRechazada()
    if (!isSuccessful) throw ServicioNoDisponible()
}

private fun <T> retrofit2.Response<T>.valor(): T {
    verificar()
    return requireNotNull(body())
}
