package com.example.totp.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para acceder a la tabla totp_accounts.
 * Define las operaciones de lectura, inserción y eliminación.
 * Usa Flow para que la UI se actualice automáticamente al cambiar los datos.
 *
 * 2. Las operaciones que defino (select,insert,delete) para la base de datos
 */
@Dao
interface TotpAccountDao {

    // Obtener todas las cuentas, ordenadas por id.
    // Flow hace que Compose se re-renderice automáticamente cuando cambian los datos.
    @Query("SELECT * FROM totp_accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<TotpAccount>>

    // Insertar una cuenta nueva
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: TotpAccount)

    // Eliminar una cuenta
    @Delete
    suspend fun deleteAccount(account: TotpAccount)

    // Obtener una cuenta por su id
    @Query("SELECT * FROM totp_accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): TotpAccount?

    // Obtener todas las cuentas (una sola vez, sin Flow)
    // Necesario para obtener el id generado tras insertar
    @Query("SELECT * FROM totp_accounts ORDER BY id ASC")
    suspend fun getAllAccountsOnce(): List<TotpAccount>

    // Actualizar una cuenta existente
    @Update
    suspend fun updateAccount(account: TotpAccount)
}