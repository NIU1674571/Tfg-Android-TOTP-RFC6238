package com.example.totp.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos Room para la aplicación TOTP.
 * Contiene una sola tabla: totp_accounts.
 *
 * Se usa el patrón Singleton para que solo exista una instancia
 * de la base de datos en toda la aplicación.
 *
 * UNIR 1+2
 */
@Database(entities = [TotpAccount::class], version = 2, exportSchema = false)
abstract class TotpDatabase : RoomDatabase() {

    abstract fun totpAccountDao(): TotpAccountDao

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