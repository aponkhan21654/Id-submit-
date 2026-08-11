package com.example.data.repository

import com.example.data.db.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class AppRepository(private val db: AppDatabase) {
    val userDao = db.userDao()
    val categoryDao = db.categoryDao()
    val submissionDao = db.submissionDao()
    val withdrawalDao = db.withdrawalDao()
    val adminConfigDao = db.adminConfigDao()

    // Flows
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()
    val allSubmissions: Flow<List<SubmissionEntity>> = submissionDao.getAllSubmissionsFlow()
    val allWithdrawals: Flow<List<WithdrawalEntity>> = withdrawalDao.getAllWithdrawalsFlow()
    val submissionDates: Flow<List<String>> = submissionDao.getSubmissionDatesFlow()
    val adminConfig: Flow<AdminConfigEntity?> = adminConfigDao.getConfigFlow()

    private fun generateRandomReferCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val randomPart = (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "REF$randomPart"
    }

    suspend fun seedInitialDataIfNeeded() {
        // Seed Admin Config if missing
        val config = adminConfigDao.getConfig()
        if (config == null) {
            val defaultConfig = AdminConfigEntity(
                id = 1,
                isSubmissionEnabled = true,
                defaultPassword = "aponkhan21",
                referralBonus = 10.0,
                perDollarRate = 120.0,
                randomFirstNames = "Tanvir,Sabbir,Karim,Rahim,Mamun,Hasan,Arif,Sumon,Shakil,Ripon",
                randomLastNames = "Ahmed,Khan,Hossain,Chowdhury,Islam,Roy,Das,Miah,Raman,Sarkar"
            )
            adminConfigDao.saveConfig(defaultConfig)
            withContext(Dispatchers.IO) {
                com.example.data.remote.SupabaseSyncService.pushAdminConfig(defaultConfig)
            }
        }

        // Seed Admin Account if missing
        val adminUser = userDao.getUserByEmail("syfaff2@gmail.com")
        if (adminUser == null) {
            val admin = UserEntity(
                name = "Admin Control",
                email = "syfaff2@gmail.com",
                password = "aponkhan21",
                referCode = "ADMIN100",
                telegramUsername = "admin_control",
                balance = 0.0,
                isAdmin = true
            )
            val id = userDao.insertUser(admin)
            withContext(Dispatchers.IO) {
                com.example.data.remote.SupabaseSyncService.pushUser(admin.copy(id = id.toInt()))
            }
        }

        // Seed default Category if empty
        val categories = categoryDao.getCategoryById(1)
        if (categories == null) {
            val defaultCat = CategoryEntity(
                name = "Facebook Account Cookie",
                rate = 35.0,
                requiresCookieHook = true,
                description = "Submit raw cookie containing c_user UID."
            )
            val catId = categoryDao.insertCategory(defaultCat)
            withContext(Dispatchers.IO) {
                com.example.data.remote.SupabaseSyncService.pushCategory(defaultCat.copy(id = catId.toInt()))
            }
        }
    }

    suspend fun syncFromRemote() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // 1. Sync Admin Config
            val remoteConfig = com.example.data.remote.SupabaseSyncService.fetchAdminConfig()
            if (remoteConfig != null) {
                adminConfigDao.saveConfig(remoteConfig)
            }

            // 2. Sync Categories
            val remoteCategories = com.example.data.remote.SupabaseSyncService.fetchCategories()
            if (remoteCategories.isNotEmpty()) {
                for (cat in remoteCategories) {
                    categoryDao.insertCategory(cat)
                }
            }

            // 3. Sync Submissions
            val remoteSubmissions = com.example.data.remote.SupabaseSyncService.fetchSubmissions()
            if (remoteSubmissions.isNotEmpty()) {
                for (sub in remoteSubmissions) {
                    val local = submissionDao.getSubmissionById(sub.id)
                    if (local == null) {
                        submissionDao.insertSubmissions(listOf(sub))
                    } else if (local.status != sub.status) {
                        submissionDao.updateSubmissionStatus(sub.id, sub.status)
                    }
                }
            }

            // 4. Sync Users
            val remoteUsers = com.example.data.remote.SupabaseSyncService.fetchUsers()
            for (u in remoteUsers) {
                val local = userDao.getUserById(u.id)
                if (local == null) {
                    userDao.insertUser(u)
                } else {
                    userDao.updateUser(u)
                }
            }

            // 5. Sync Withdrawals
            val remoteWithdrawals = com.example.data.remote.SupabaseSyncService.fetchWithdrawals()
            for (w in remoteWithdrawals) {
                val local = withdrawalDao.getWithdrawalById(w.id)
                if (local == null) {
                    withdrawalDao.insertWithdrawal(w)
                } else {
                    withdrawalDao.updateWithdrawalStatus(w.id, w.status)
                }
            }
        } catch (e: Exception) {
            // Ignore offline errors
        }
    }

    suspend fun registerUser(user: UserEntity, usedReferCodeInput: String): Result<Long> {
        return try {
            val cleanEmail = user.email.trim().lowercase()
            var existing = userDao.getUserByEmail(cleanEmail)
            if (existing == null) {
                syncFromRemote()
                existing = userDao.getUserByEmail(cleanEmail)
            }
            if (existing != null) {
                return Result.failure(Exception("এই Gmail দিয়ে ইতোমধ্যে account তৈরি করা হয়েছে।"))
            }

            // Generate unique random refer code for new user
            var uniqueCode = generateRandomReferCode()
            while (userDao.getUserByReferCode(uniqueCode) != null) {
                uniqueCode = generateRandomReferCode()
            }

            val finalUser = user.copy(
                email = cleanEmail,
                referCode = uniqueCode,
                usedReferCode = usedReferCodeInput.trim().uppercase()
            )

            val id = userDao.insertUser(finalUser)
            val insertedUser = finalUser.copy(id = id.toInt())

            // Synchronously push to Supabase to guarantee account is saved in database
            withContext(Dispatchers.IO) {
                com.example.data.remote.SupabaseSyncService.pushUser(insertedUser)
            }

            // If a valid referral code was entered, reward referrer
            val cleanUsedCode = usedReferCodeInput.trim().uppercase()
            if (cleanUsedCode.isNotEmpty() && cleanUsedCode != "N/A") {
                val referrer = userDao.getUserByReferCode(cleanUsedCode)
                if (referrer != null && referrer.id != id.toInt()) {
                    val config = adminConfigDao.getConfig()
                    val bonusAmount = config?.referralBonus ?: 10.0
                    userDao.addToBalance(referrer.id, bonusAmount)
                    userDao.incrementReferredCount(referrer.id)
                }
            }

            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, pass: String): UserEntity? {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = pass.trim()
        
        // Sync from remote first if user is not in local database (e.g. after app clear data)
        var user = userDao.getUserByEmail(cleanEmail)
        if (user == null) {
            syncFromRemote()
            user = userDao.getUserByEmail(cleanEmail)
        }

        if (user != null && user.password == cleanPass) {
            return user
        }
        return null
    }

    suspend fun setUserWithdrawPin(userId: Int, pin: String) {
        userDao.updateUserPin(userId, pin)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.example.data.remote.SupabaseSyncService.updateUserPin(userId, pin)
        }
    }

    suspend fun resetUserPinByEmail(email: String): Result<Boolean> {
        return try {
            val cleanEmail = email.trim()
            val user = userDao.getUserByEmail(cleanEmail)
            if (user != null) {
                userDao.updateUserPin(user.id, "")
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    com.example.data.remote.SupabaseSyncService.updateUserPin(user.id, "")
                }
                Result.success(true)
            } else {
                // Try sync and re-check
                syncFromRemote()
                val userRemote = userDao.getUserByEmail(cleanEmail)
                if (userRemote != null) {
                    userDao.updateUserPin(userRemote.id, "")
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        com.example.data.remote.SupabaseSyncService.updateUserPin(userRemote.id, "")
                    }
                    Result.success(true)
                } else {
                    Result.failure(Exception("এই Gmail দিয়ে কোনো User পাওয়া যায়নি।"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserFlow(userId: Int): Flow<UserEntity?> {
        return userDao.getUserByIdFlow(userId)
    }

    fun getUserSubmissions(userId: Int): Flow<List<SubmissionEntity>> {
        return submissionDao.getSubmissionsForUserFlow(userId)
    }

    fun getUserWithdrawals(userId: Int): Flow<List<WithdrawalEntity>> {
        return withdrawalDao.getWithdrawalsForUserFlow(userId)
    }

    suspend fun submitAccountCookies(
        userId: Int,
        userName: String,
        userEmail: String,
        category: CategoryEntity,
        assignedPassword: String,
        rawInput: String
    ): Result<Int> {
        val config = adminConfigDao.getConfig()
        if (config != null && !config.isSubmissionEnabled) {
            return Result.failure(Exception("Admin submission বন্ধ করে রেখেছেন। অনুগ্রহ করে পরে চেষ্টা করুন।"))
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val submissionsList = mutableListOf<SubmissionEntity>()

        if (category.requiresCookieHook) {
            val lines = rawInput.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            val cUserRegex = Regex("""c_user=([0-9]+)""")

            for (line in lines) {
                val match = cUserRegex.find(line)
                if (match != null) {
                    val uid = match.groupValues[1]
                    val formatted = "$uid/$assignedPassword/$line"
                    submissionsList.add(
                        SubmissionEntity(
                            userId = userId,
                            userName = userName,
                            userEmail = userEmail,
                            categoryId = category.id,
                            categoryName = category.name,
                            uid = uid,
                            password = assignedPassword,
                            rawCookie = line,
                            formattedString = formatted,
                            submittedRate = category.rate,
                            status = "PENDING",
                            dateString = dateStr
                        )
                    )
                }
            }

            if (submissionsList.isEmpty()) {
                val matches = cUserRegex.findAll(rawInput)
                for (match in matches) {
                    val uid = match.groupValues[1]
                    val formatted = "$uid/$assignedPassword/$rawInput"
                    submissionsList.add(
                        SubmissionEntity(
                            userId = userId,
                            userName = userName,
                            userEmail = userEmail,
                            categoryId = category.id,
                            categoryName = category.name,
                            uid = uid,
                            password = assignedPassword,
                            rawCookie = rawInput,
                            formattedString = formatted,
                            submittedRate = category.rate,
                            status = "PENDING",
                            dateString = dateStr
                        )
                    )
                }
            }
        } else {
            val lines = rawInput.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            for (line in lines) {
                submissionsList.add(
                    SubmissionEntity(
                        userId = userId,
                        userName = userName,
                        userEmail = userEmail,
                        categoryId = category.id,
                        categoryName = category.name,
                        uid = line,
                        password = assignedPassword,
                        rawCookie = line,
                        formattedString = "$line/$assignedPassword",
                        submittedRate = category.rate,
                        status = "PENDING",
                        dateString = dateStr
                    )
                )
            }
        }

        if (submissionsList.isEmpty()) {
            return Result.failure(Exception("Cookie তে c_user UID পাওয়া যায়নি। সঠিক cookie দিন।"))
        }

        val insertedIds = submissionDao.insertSubmissions(submissionsList)
        val updatedSubmissionsList = submissionsList.mapIndexed { index, sub ->
            val genId = insertedIds.getOrNull(index)?.toInt() ?: 0
            sub.copy(id = genId)
        }

        // Sync submissions to Supabase
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            for (sub in updatedSubmissionsList) {
                com.example.data.remote.SupabaseSyncService.pushSubmission(sub)
            }
        }

        return Result.success(submissionsList.size)
    }

    suspend fun requestWithdrawal(
        userId: Int,
        userName: String,
        userEmail: String,
        method: String,
        accountDetails: String,
        amountTk: Double,
        amountUsd: Double
    ): Result<Boolean> {
        val user = userDao.getUserById(userId)
            ?: return Result.failure(Exception("User পাওয়া যায়নি।"))

        if (user.balance < amountTk) {
            return Result.failure(Exception("আপনার পর্যাপ্ত ব্যালেন্স নেই! আপনার বর্তমান ব্যালেন্স ৳${"%.2f".format(user.balance)}"))
        }

        if (method == "Bkash" && amountTk < 50.0) {
            return Result.failure(Exception("বিকাশ উইথড্র এর জন্য সর্বনিম্ন ৳৫০ প্রয়োজন।"))
        }

        if (method == "Binance" && amountUsd < 20.0) {
            return Result.failure(Exception("বাইন্যান্স উইথড্র এর জন্য সর্বনিম্ন ২০$ (USDT) প্রয়োজন।"))
        }

        // Deduct balance immediately
        userDao.addToBalance(userId, -amountTk)

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val newW = WithdrawalEntity(
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            method = method,
            accountDetails = accountDetails,
            amountTk = amountTk,
            amountUsd = amountUsd,
            status = "PENDING",
            dateString = dateStr
        )
        val wId = withdrawalDao.insertWithdrawal(newW)

        // Sync withdrawal and balance update to Supabase
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.example.data.remote.SupabaseSyncService.pushWithdrawal(newW.copy(id = wId.toInt()))
            userDao.getUserById(userId)?.let { updatedUser ->
                com.example.data.remote.SupabaseSyncService.updateUserBalance(userId, updatedUser.balance)
            }
        }

        return Result.success(true)
    }

    suspend fun processWithdrawalStatus(withdrawalId: Int, newStatus: String): Result<Boolean> {
        val w = withdrawalDao.getWithdrawalById(withdrawalId)
            ?: return Result.failure(Exception("Withdrawal Request পাওয়া যায়নি।"))

        if (w.status == newStatus) return Result.success(true)

        // If rejecting an already pending/approved withdrawal, refund money back
        if (newStatus == "REJECTED" && w.status != "REJECTED") {
            userDao.addToBalance(w.userId, w.amountTk)
        }

        withdrawalDao.updateWithdrawalStatus(withdrawalId, newStatus)

        // Sync withdrawal status & user balance to Supabase
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.example.data.remote.SupabaseSyncService.updateWithdrawalStatus(withdrawalId, newStatus)
            userDao.getUserById(w.userId)?.let { updatedUser ->
                com.example.data.remote.SupabaseSyncService.updateUserBalance(w.userId, updatedUser.balance)
            }
        }

        return Result.success(true)
    }

    suspend fun processAdminReport(
        categoryId: Int,
        dateString: String,
        successUidsText: String
    ): Result<Pair<Int, Int>> {
        val uidRegex = Regex("""[0-9]{8,20}""")
        val successUidsSet = uidRegex.findAll(successUidsText).map { it.value.trim() }.toSet()

        val submissions = submissionDao.getSubmissionsForCategoryAndDate(categoryId, dateString)
        val pendingSubmissions = submissions.filter { it.status == "PENDING" }

        var successCount = 0
        var rejectCount = 0

        for (sub in pendingSubmissions) {
            val isSuccess = successUidsSet.contains(sub.uid)
            val newStatus = if (isSuccess) "SUCCESS" else "REJECTED"

            if (isSuccess) {
                submissionDao.updateSubmissionStatus(sub.id, "SUCCESS")
                userDao.addToBalance(sub.userId, sub.submittedRate)
                successCount++
            } else {
                submissionDao.updateSubmissionStatus(sub.id, "REJECTED")
                rejectCount++
            }

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                com.example.data.remote.SupabaseSyncService.updateSubmissionStatus(sub.id, newStatus)
                userDao.getUserById(sub.userId)?.let { updatedUser ->
                    com.example.data.remote.SupabaseSyncService.updateUserBalance(sub.userId, updatedUser.balance)
                }
            }
        }

        return Result.success(Pair(successCount, rejectCount))
    }

    suspend fun getFormattedExportText(categoryId: Int, dateString: String): String {
        val list = submissionDao.getSubmissionsForCategoryAndDate(categoryId, dateString)
        val builder = StringBuilder()
        for (sub in list) {
            builder.append("${sub.uid}\t${sub.password}\t${sub.rawCookie}\n")
        }
        return builder.toString().trim()
    }

    suspend fun getFormattedExportCsv(categoryId: Int, dateString: String): String {
        val list = submissionDao.getSubmissionsForCategoryAndDate(categoryId, dateString)
        val builder = StringBuilder()
        builder.append("UID,Password,Cookie\n")
        for (sub in list) {
            val cleanCookie = sub.rawCookie.replace("\"", "\"\"")
            builder.append("\"${sub.uid}\",\"${sub.password}\",\"$cleanCookie\"\n")
        }
        return builder.toString().trim()
    }
}
