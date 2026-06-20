package com.example.totp.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para acceder a la tabla password_entries.
 * Define las operaciones CRUD y de búsqueda/filtrado.
 * Usa Flow para que la UI se actualice automáticamente al cambiar los datos.
 */
@Dao
interface PasswordEntryDao {

    // Obtener todas las credenciales, ordenadas por nombre de servicio.
    // Flow hace que Compose se re-renderice automáticamente cuando cambian los datos.
    @Query("SELECT * FROM password_entries ORDER BY serviceName ASC")
    fun getAllEntries(): Flow<List<PasswordEntry>>

    // Buscar credenciales por servicio o usuario.
    // Se usa LIKE con comodines para búsqueda parcial (contiene el texto).
    // Cubre el objetivo OF-S4: búsqueda y filtrado por servicio o usuario.
    @Query("""
        SELECT * FROM password_entries 
        WHERE serviceName LIKE '%' || :query || '%' 
           OR username LIKE '%' || :query || '%' 
        ORDER BY serviceName ASC
    """)
    fun searchEntries(query: String): Flow<List<PasswordEntry>>

    // Insertar una credencial nueva
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PasswordEntry)

    // Actualizar una credencial existente
    @Update
    suspend fun updateEntry(entry: PasswordEntry)

    // Eliminar una credencial
    @Delete
    suspend fun deleteEntry(entry: PasswordEntry)

    // Obtener una credencial por su id
    @Query("SELECT * FROM password_entries WHERE id = :id")
    suspend fun getEntryById(id: Int): PasswordEntry?

    // Obtener todas las credenciales (una sola vez, sin Flow)
    // Necesario para obtener el id generado tras insertar
    @Query("SELECT * FROM password_entries ORDER BY serviceName ASC")
    suspend fun getAllEntriesOnce(): List<PasswordEntry>
}