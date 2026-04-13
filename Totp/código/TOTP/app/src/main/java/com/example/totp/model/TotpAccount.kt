package com.example.totp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "totp_accounts")
data class TotpAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val issuer: String,
    val algorithm: String = "HmacSHA1",
    val digits: Int = 6,
    val period: Int = 30,
    val iconUri: String? = null
)
