package com.example.primeraclaseprogramacionmobile.data.remoto

import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*

@Serializable
data class RecordatorioDto(
    val id: Long, val titulo: String, val descripcion: String? = null, val categoria: String,
    val prioridad: String, val estado: String, val fecha: Long,
)

interface ServicioRecordatorios {
    @GET("recordatorios") suspend fun listar(): retrofit2.Response<List<RecordatorioDto>>
    @PUT("recordatorios/{id}") suspend fun guardar(@Path("id") id: Long, @Body registro: RecordatorioDto): retrofit2.Response<RecordatorioDto>
    @DELETE("recordatorios/{id}") suspend fun eliminar(@Path("id") id: Long): retrofit2.Response<Unit>
}

private val formato = Json { ignoreUnknownKeys = true }

fun servicioRecordatorios(url: String, esperaMs: Long = 4_000): ServicioRecordatorios {
    val cliente = OkHttpClient.Builder().callTimeout(esperaMs, TimeUnit.MILLISECONDS)
        .readTimeout(esperaMs, TimeUnit.MILLISECONDS).retryOnConnectionFailure(false).build()
    return Retrofit.Builder().baseUrl(url).client(cliente)
        .addConverterFactory(formato.asConverterFactory("application/json".toMediaType()))
        .build().create(ServicioRecordatorios::class.java)
}
