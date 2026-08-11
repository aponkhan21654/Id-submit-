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
            .addHeader("Prefer", "resolution=merge-duplicates, return=minimal")

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
                if (submission.id > 0) {
                    put("id", submission.id)
                }
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
            val cleanEmail = user.email.trim().lowercase()
            val json = JSONObject().apply {
                put("name", user.name)
                put("email", cleanEmail)
                put("password", user.password.trim())
                put("refer_code", user.referCode)
                put("used_refer_code", user.usedReferCode)
                put("telegram_username", user.telegramUsername)
                put("balance", user.balance)
                put("is_admin", user.isAdmin)
                put("referred_count", user.referredCount)
                put("withdraw_pin", user.withdrawPin)
            }

            val request = buildRequest("users?on_conflict=email", "POST", json.toString().toRequestBody(jsonMediaType))
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
                                referredCount = obj.optInt("referred_count", 0),
                                withdrawPin = obj.optString("withdraw_pin", "")
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

    // UPDATE USER PIN
    fun updateUserPin(userEmail: String, pin: String) {
        try {
            val cleanEmail = userEmail.trim().lowercase()
            val json = JSONObject().apply {
                put("withdraw_pin", pin)
            }
            val request = buildRequest("users?email=eq.$cleanEmail", "PATCH", json.toString().toRequestBody(jsonMediaType))
            okHttpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to update user pin: ${e.message}")
        }
    }

    // 5. PUSH WITHDRAWAL
    fun pushWithdrawal(withdrawal: WithdrawalEntity) {
        try {
            val json = JSONObject().apply {
                if (withdrawal.id > 0) {
                    put("id", withdrawal.id)
                }
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
    fun updateUserBalance(userEmail: String, newBalance: Double) {
        try {
            val cleanEmail = userEmail.trim().lowercase()
            val json = JSONObject().apply {
                put("balance", newBalance)
            }
            val request = buildRequest("users?email=eq.$cleanEmail", "PATCH", json.toString().toRequestBody(jsonMediaType))
            okHttpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to update user balance: ${e.message}")
        }
    }

    // 10. PUSH ADMIN CONFIG
    fun pushAdminConfig(config: com.example.data.db.AdminConfigEntity) {
        try {
            val json = JSONObject().apply {
                put("id", 1)
                put("is_submission_enabled", config.isSubmissionEnabled)
                put("default_password", config.defaultPassword)
                put("referral_bonus", config.referralBonus)
                put("per_dollar_rate", config.perDollarRate)
                put("random_first_names", config.randomFirstNames)
                put("random_last_names", config.randomLastNames)
            }
            val request = buildRequest("admin_config", "POST", json.toString().toRequestBody(jsonMediaType))
            okHttpClient.newCall(request).execute().use { response ->
                Log.d("SupabaseSync", "Push admin config code: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to push admin config: ${e.message}")
        }
    }

    // 11. FETCH ADMIN CONFIG
    fun fetchAdminConfig(): com.example.data.db.AdminConfigEntity? {
        try {
            val request = buildRequest("admin_config?id=eq.1&select=*")
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(bodyStr)
                    if (jsonArray.length() > 0) {
                        val obj = jsonArray.getJSONObject(0)
                        return com.example.data.db.AdminConfigEntity(
                            id = 1,
                            isSubmissionEnabled = obj.optBoolean("is_submission_enabled", true),
                            defaultPassword = obj.optString("default_password", "aponkhan21"),
                            referralBonus = obj.optDouble("referral_bonus", 10.0),
                            perDollarRate = obj.optDouble("per_dollar_rate", 120.0),
                            randomFirstNames = obj.optString("random_first_names", "Tanvir,Sabbir,Karim,Rahim,Mamun,Hasan,Arif,Sumon,Shakil,Ripon"),
                            randomLastNames = obj.optString("random_last_names", "Ahmed,Khan,Hossain,Chowdhury,Islam,Roy,Das,Miah,Raman,Sarkar")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to fetch admin config: ${e.message}")
        }
        return null
    }

    // 12. PUSH CATEGORY
    fun pushCategory(category: com.example.data.db.CategoryEntity) {
        try {
            val json = JSONObject().apply {
                if (category.id > 0) {
                    put("id", category.id)
                }
                put("name", category.name)
                put("rate", category.rate)
                put("requires_cookie_hook", category.requiresCookieHook)
                put("description", category.description)
            }
            val request = buildRequest("categories", "POST", json.toString().toRequestBody(jsonMediaType))
            okHttpClient.newCall(request).execute().use { response ->
                Log.d("SupabaseSync", "Push category code: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to push category: ${e.message}")
        }
    }

    // 13. FETCH CATEGORIES
    fun fetchCategories(): List<com.example.data.db.CategoryEntity> {
        val list = mutableListOf<com.example.data.db.CategoryEntity>()
        try {
            val request = buildRequest("categories?select=*")
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(bodyStr)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            com.example.data.db.CategoryEntity(
                                id = obj.optInt("id", 0),
                                name = obj.optString("name", ""),
                                rate = obj.optDouble("rate", 0.0),
                                requiresCookieHook = obj.optBoolean("requires_cookie_hook", true),
                                description = obj.optString("description", "")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to fetch categories: ${e.message}")
        }
        return list
    }

    // 14. DELETE CATEGORY
    fun deleteCategory(categoryId: Int) {
        try {
            val request = buildRequest("categories?id=eq.$categoryId", "DELETE")
            okHttpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to delete category: ${e.message}")
        }
    }
}
