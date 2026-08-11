package com.example.data.remote

import android.util.Log
import com.example.data.db.SubmissionEntity
import com.example.data.db.UserEntity
import com.example.data.db.WithdrawalEntity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseSyncService {
    private const val SUPABASE_URL = "https://lyxwbcdtgjbnzkyqbxqc.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_WPGNPCSGQ4B20vzAnjBV6A_EibscRC0"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun buildRequest(endpoint: String, method: String = "GET", body: RequestBody? = null): Request {
        val url = "$SUPABASE_URL/rest/v1/$endpoint"
        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")

        return when (method.uppercase()) {
            "POST" -> builder.post(body ?: "".toRequestBody(jsonMediaType)).build()
            "PATCH" -> builder.patch(body ?: "".toRequestBody(jsonMediaType)).build()
            "DELETE" -> builder.delete(body).build()
            else -> builder.get().build()
        }
    }

    // 1. PUSH SUBMISSION
    fun pushSubmission(submission: SubmissionEntity) {
        try {
            val json = JSONObject().apply {
                put("id", submission.id)
                put("user_id", submission.userId)
                put("user_name", submission.userName)
                put("user_email", submission.userEmail)
                put("category_id", submission.categoryId)
                put("category_name", submission.categoryName)
                put("uid", submission.uid)
                put("password", submission.password)
                put("raw_cookie", submission.rawCookie)
                put("formatted_string", submission.formattedString)
                put("submitted_rate", submission.submittedRate)
                put("status", submission.status)
                put("date_string", submission.dateString)
            }

            val request = buildRequest("submissions", "POST", json.toString().toRequestBody(jsonMediaType))
            okHttpClient.newCall(request).execute().use { response ->
                Log.d("SupabaseSync", "Push submission code: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to push submission: ${e.message}")
        }
    }

    // 2. FETCH SUBMISSIONS
    fun fetchSubmissions(): List<SubmissionEntity> {
        val list = mutableListOf<SubmissionEntity>()
        try {
            val request = buildRequest("submissions?select=*")
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(bodyStr)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            SubmissionEntity(
                                id = obj.optInt("id", 0),
                                userId = obj.optInt("user_id", 0),
                                userName = obj.optString("user_name", ""),
                                userEmail = obj.optString("user_email", ""),
                                categoryId = obj.optInt("category_id", 0),
                                categoryName = obj.optString("category_name", ""),
                                uid = obj.optString("uid", ""),
                                password = obj.optString("password", ""),
                                rawCookie = obj.optString("raw_cookie", ""),
                                formattedString = obj.optString("formatted_string", ""),
                                submittedRate = obj.optDouble("submitted_rate", 0.0),
                                status = obj.optString("status", "PENDING"),
                                dateString = obj.optString("date_string", "")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to fetch submissions: ${e.message}")
        }
        return list
    }

    // 3. PUSH USER
    fun pushUser(user: UserEntity) {
        try {
            val json = JSONObject().apply {
                put("id", user.id)
                put("name", user.name)
                put("email", user.email)
                put("password", user.password)
                put("refer_code", user.referCode)
                put("used_refer_code", user.usedReferCode)
                put("telegram_username", user.telegramUsername)
                put("balance", user.balance)
                put("is_admin", user.isAdmin)
                put("referred_count", user.referredCount)
            }

            val request = buildRequest("users", "POST", json.toString().toRequestBody(jsonMediaType))
            okHttpClient.newCall(request).execute().use { response ->
                Log.d("SupabaseSync", "Push user code: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to push user: ${e.message}")
        }
    }

    // 4. FETCH USERS
    fun fetchUsers(): List<UserEntity> {
        val list = mutableListOf<UserEntity>()
        try {
            val request = buildRequest("users?select=*")
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(bodyStr)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            UserEntity(
                                id = obj.optInt("id", 0),
                                name = obj.optString("name", ""),
                                email = obj.optString("email", ""),
                                password = obj.optString("password", ""),
                                referCode = obj.optString("refer_code", ""),
                                usedReferCode = obj.optString("used_refer_code", ""),
                                telegramUsername = obj.optString("telegram_username", ""),
                                balance = obj.optDouble("balance", 0.0),
                                isAdmin = obj.optBoolean("is_admin", false),
                                referredCount = obj.optInt("referred_count", 0)
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to fetch users: ${e.message}")
        }
        return list
    }

    // 5. PUSH WITHDRAWAL
    fun pushWithdrawal(withdrawal: WithdrawalEntity) {
        try {
            val json = JSONObject().apply {
                put("id", withdrawal.id)
                put("user_id", withdrawal.userId)
                put("user_name", withdrawal.userName)
                put("user_email", withdrawal.userEmail)
                put("method", withdrawal.method)
                put("account_details", withdrawal.accountDetails)
                put("amount_tk", withdrawal.amountTk)
                put("amount_usd", withdrawal.amountUsd)
                put("status", withdrawal.status)
                put("date_string", withdrawal.dateString)
            }

            val request = buildRequest("withdrawals", "POST", json.toString().toRequestBody(jsonMediaType))
            okHttpClient.newCall(request).execute().use { response ->
                Log.d("SupabaseSync", "Push withdrawal code: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to push withdrawal: ${e.message}")
        }
    }

    // 6. FETCH WITHDRAWALS
    fun fetchWithdrawals(): List<WithdrawalEntity> {
        val list = mutableListOf<WithdrawalEntity>()
        try {
            val request = buildRequest("withdrawals?select=*")
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(bodyStr)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            WithdrawalEntity(
                                id = obj.optInt("id", 0),
                                userId = obj.optInt("user_id", 0),
                                userName = obj.optString("user_name", ""),
                                userEmail = obj.optString("user_email", ""),
                                method = obj.optString("method", ""),
                                accountDetails = obj.optString("account_details", ""),
                                amountTk = obj.optDouble("amount_tk", 0.0),
                                amountUsd = obj.optDouble("amount_usd", 0.0),
                                status = obj.optString("status", "PENDING"),
                                dateString = obj.optString("date_string", "")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to fetch withdrawals: ${e.message}")
        }
        return list
    }

    // 7. UPDATE SUBMISSION STATUS
    fun updateSubmissionStatus(submissionId: Int, status: String) {
        try {
            val json = JSONObject().apply {
                put("status", status)
            }
            val request = buildRequest("submissions?id=eq.$submissionId", "PATCH", json.toString().toRequestBody(jsonMediaType))
            okHttpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to update submission status: ${e.message}")
        }
    }

    // 8. UPDATE WITHDRAWAL STATUS
    fun updateWithdrawalStatus(withdrawalId: Int, status: String) {
        try {
            val json = JSONObject().apply {
                put("status", status)
            }
            val request = buildRequest("withdrawals?id=eq.$withdrawalId", "PATCH", json.toString().toRequestBody(jsonMediaType))
            okHttpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to update withdrawal status: ${e.message}")
        }
    }

    // 9. UPDATE USER BALANCE
    fun updateUserBalance(userId: Int, newBalance: Double) {
        try {
            val json = JSONObject().apply {
                put("balance", newBalance)
            }
            val request = buildRequest("users?id=eq.$userId", "PATCH", json.toString().toRequestBody(jsonMediaType))
            okHttpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to update user balance: ${e.message}")
        }
    }
}
