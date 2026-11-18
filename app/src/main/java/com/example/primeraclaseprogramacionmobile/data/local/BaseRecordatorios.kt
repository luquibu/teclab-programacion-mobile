package com.example.primeraclaseprogramacionmobile.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recordatorios")
data class FilaRecordatorio(
    @PrimaryKey val id: Long,
    val titulo: String, val descripcion: String?, val categoria: String,
    val prioridad: String, val estado: String, val fecha: Long,
    val pendiente: Boolean, val borrado: Boolean = false, val revision: Long = 1,
)

@Dao
interface RecordatorioDao {
    @Query("SELECT * FROM recordatorios WHERE borrado = 0 ORDER BY fecha DESC, id DESC")
    fun todos(): Flow<List<FilaRecordatorio>>

    @Query("SELECT * FROM recordatorios WHERE borrado = 0 AND estado = :estado ORDER BY fecha DESC, id DESC")
    fun porEstado(estado: String): Flow<List<FilaRecordatorio>>

    @Query("SELECT * FROM recordatorios WHERE id = :id AND borrado = 0")
    fun observar(id: Long): Flow<FilaRecordatorio?>

    @Query("SELECT * FROM recordatorios WHERE id = :id")
    suspend fun buscar(id: Long): FilaRecordatorio?

    @Insert suspend fun insertar(fila: FilaRecordatorio)
    @Update suspend fun actualizar(fila: FilaRecordatorio)
    @Delete suspend fun eliminar(fila: FilaRecordatorio)

    @Query("SELECT * FROM recordatorios WHERE pendiente = 1 ORDER BY fecha, id")
    suspend fun pendientes(): List<FilaRecordatorio>

    @Query("SELECT * FROM recordatorios WHERE pendiente = 0")
    suspend fun confirmados(): List<FilaRecordatorio>

    @Query("DELETE FROM recordatorios WHERE id = :id AND borrado = 1 AND revision = :revision")
    suspend fun confirmarBaja(id: Long, revision: Long)
}

@Database(entities = [FilaRecordatorio::class], version = 1, exportSchema = true)
abstract class BaseRecordatorios : RoomDatabase() {
    abstract fun recordatorios(): RecordatorioDao
}
