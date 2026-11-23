package com.example.primeraclaseprogramacionmobile.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recordatorios", primaryKeys = ["uid", "id"])
data class FilaRecordatorio(
    val id: Long,
    val titulo: String, val descripcion: String?, val categoria: String,
    val prioridad: String, val estado: String, val fecha: Long,
    val pendiente: Boolean, val borrado: Boolean = false, val revision: Long = 1,
    val uid: String,
)

@Dao
interface RecordatorioDao {
    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND borrado = 0 ORDER BY fecha DESC, id DESC")
    fun todos(uid: String): Flow<List<FilaRecordatorio>>

    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND borrado = 0 AND estado = :estado ORDER BY fecha DESC, id DESC")
    fun porEstado(uid: String, estado: String): Flow<List<FilaRecordatorio>>

    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND id = :id AND borrado = 0")
    fun observar(uid: String, id: Long): Flow<FilaRecordatorio?>

    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND id = :id")
    suspend fun buscar(uid: String, id: Long): FilaRecordatorio?

    @Insert suspend fun insertar(fila: FilaRecordatorio)
    @Update suspend fun actualizar(fila: FilaRecordatorio)
    @Delete suspend fun eliminar(fila: FilaRecordatorio)

    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND pendiente = 1 ORDER BY fecha, id")
    suspend fun pendientes(uid: String): List<FilaRecordatorio>

    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND pendiente = 0")
    suspend fun confirmados(uid: String): List<FilaRecordatorio>

    @Query("DELETE FROM recordatorios WHERE uid = :uid AND id = :id AND borrado = 1 AND revision = :revision")
    suspend fun confirmarBaja(uid: String, id: Long, revision: Long)
}

@Database(entities = [FilaRecordatorio::class], version = 2, exportSchema = true)
abstract class BaseRecordatorios : RoomDatabase() {
    abstract fun recordatorios(): RecordatorioDao
}

// Las filas de API3 nunca tuvieron dueño. El uid vacío las conserva fuera de cualquier sesión.
val migracionDeApi3 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE recordatorios_por_cuenta (
            id INTEGER NOT NULL, titulo TEXT NOT NULL, descripcion TEXT, categoria TEXT NOT NULL,
            prioridad TEXT NOT NULL, estado TEXT NOT NULL, fecha INTEGER NOT NULL,
            pendiente INTEGER NOT NULL, borrado INTEGER NOT NULL, revision INTEGER NOT NULL,
            uid TEXT NOT NULL, PRIMARY KEY(uid, id))""")
        db.execSQL("""INSERT INTO recordatorios_por_cuenta
            SELECT id,titulo,descripcion,categoria,prioridad,estado,fecha,pendiente,borrado,revision,'' FROM recordatorios""")
        db.execSQL("DROP TABLE recordatorios")
        db.execSQL("ALTER TABLE recordatorios_por_cuenta RENAME TO recordatorios")
    }
}
