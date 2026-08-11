package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val password: String,
    val referCode: String,
    val usedReferCode: String = "",
    val referredCount: Int = 0,
    val telegramUsername: String,
    val balance: Double = 0.0,
    val isAdmin: Boolean = false,
    val withdrawPin: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rate: Double,
    val requiresCookieHook: Boolean = true,
    val description: String = "Submit Facebook account cookie with c_user UID",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "submissions")
data class SubmissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val userName: String,
    val userEmail: String,
    val categoryId: Int,
    val categoryName: String,
    val uid: String,
    val password: String,
    val rawCookie: String,
    val formattedString: String,
    val submittedRate: Double,
    val status: String = "PENDING", // PENDING, SUCCESS, REJECTED
    val dateString: String, // e.g. "2026-08-10"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "withdrawals")
data class WithdrawalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val userName: String,
    val userEmail: String,
    val method: String, // Bkash, Binance
    val accountDetails: String, // Bkash Phone Number or Binance Pay ID / USDT TRC20 address
    val amountTk: Double,
    val amountUsd: Double = 0.0,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val dateString: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_config")
data class AdminConfigEntity(
    @PrimaryKey val id: Int = 1,
    val isSubmissionEnabled: Boolean = true,
    val defaultPassword: String = "aponkhan21",
    val referralBonus: Double = 10.0,
    val perDollarRate: Double = 120.0,
    val randomFirstNames: String = "Tanvir,Sabbir,Karim,Rahim,Mamun,Hasan,Arif,Sumon,Shakil,Ripon",
    val randomLastNames: String = "Ahmed,Khan,Hossain,Chowdhury,Islam,Roy,Das,Miah,Raman,Sarkar"
)
