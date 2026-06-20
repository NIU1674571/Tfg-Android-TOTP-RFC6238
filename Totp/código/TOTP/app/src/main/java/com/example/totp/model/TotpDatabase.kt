package com.example.totp.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos Room para la aplicación.
 * Contiene dos tablas:
 * - totp_accounts: cuentas TOTP para la verificación en dos pasos.
 * - password_entries: credenciales del gestor de contraseñas.
 *
 * Versión 5: se añade el campo iconUri a password_entries
 *            para soportar iconos personalizados.
 */
@Database(
    entities = [TotpAccount::class, PasswordEntry::class],
    version = 5,
    exportSchema = false
)
abstract class TotpDatabase : RoomDatabase() {

    abstract fun totpAccountDao(): TotpAccountDao
    abstract fun passwordEntryDao(): PasswordEntryDao

    companion object {
        @Volatile
        private var INSTANCE: TotpDatabase? = null

        fun getDatabase(context: Context): TotpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TotpDatabase::class.java,
                    "totp_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}