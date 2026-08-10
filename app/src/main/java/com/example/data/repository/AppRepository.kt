package com.example.data.repository

import com.example.data.db.*
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
            adminConfigDao.saveConfig(
                AdminConfigEntity(
                    id = 1,
                    isSubmissionEnabled = true,
                    defaultPassword = "aponkhan21",
                    referralBonus = 10.0,
                    perDollarRate = 120.0,
                    randomFirstNames = "Tanvir,Sabbir,Karim,Rahim,Mamun,Hasan,Arif,Sumon,Shakil,Ripon",
                    randomLastNames = "Ahmed,Khan,Hossain,Chowdhury,Islam,Roy,Das,Miah,Raman,Sarkar"
                )
            )
        }

        // Seed Admin Account if missing
        val adminUser = userDao.getUserByEmail("syfaff2@gmail.com")
        if (adminUser == null) {
            userDao.insertUser(
                UserEntity(
                    name = "Admin Control",
                    email = "syfaff2@gmail.com",
                    password = "aponkhan21",
                    referCode = "ADMIN100",
                    telegramUsername = "admin_control",
                    balance = 0.0,
                    isAdmin = true
                )
            )
        }

        // Seed default Category if empty
        val categories = categoryDao.getCategoryById(1)
        if (categories == null) {
            categoryDao.insertCategory(
                CategoryEntity(
                    name = "Facebook Account Cookie",
                    rate = 35.0,
                    requiresCookieHook = true,
                    description = "Submit raw cookie containing c_user UID."
                )
            )
        }
    }

    suspend fun registerUser(user: UserEntity, usedReferCodeInput: String): Result<Long> {
        return try {
            val existing = userDao.getUserByEmail(user.email)
            if (existing != null) {
                return Result.failure(Exception("এই Gmail দিয়ে ইতোমধ্যে account তৈরি করা হয়েছে।"))
            }

            // Generate unique random refer code for new user
            var uniqueCode = generateRandomReferCode()
            while (userDao.getUserByReferCode(uniqueCode) != null) {
                uniqueCode = generateRandomReferCode()
            }

            val finalUser = user.copy(
                referCode = uniqueCode,
                usedReferCode = usedReferCodeInput.trim().uppercase()
            )

            val id = userDao.insertUser(finalUser)

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
        val user = userDao.getUserByEmail(email.trim())
        if (user != null && user.password == pass.trim()) {
            return user
        }
        return null
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

        submissionDao.insertSubmissions(submissionsList)
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
        withdrawalDao.insertWithdrawal(
            WithdrawalEntity(
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
        )

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
            if (successUidsSet.contains(sub.uid)) {
                submissionDao.updateSubmissionStatus(sub.id, "SUCCESS")
                userDao.addToBalance(sub.userId, sub.submittedRate)
                successCount++
            } else {
                submissionDao.updateSubmissionStatus(sub.id, "REJECTED")
                rejectCount++
            }
        }

        return Result.success(Pair(successCount, rejectCount))
    }

    suspend fun getFormattedExportText(categoryId: Int, dateString: String): String {
        val list = submissionDao.getSubmissionsForCategoryAndDate(categoryId, dateString)
        val groupedByUser = list.groupBy { it.userId }
        val builder = StringBuilder()

        for ((_, userSubmissions) in groupedByUser) {
            for (sub in userSubmissions) {
                builder.append(sub.formattedString).append("\n")
            }
        }

        return builder.toString().trim()
    }
}
