package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.CategoryEntity
import com.example.data.db.SubmissionEntity
import com.example.data.db.UserEntity
import com.example.data.db.WithdrawalEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface AuthState {
    object LoggedOut : AuthState
    data class LoggedInUser(val user: UserEntity) : AuthState
    data class LoggedInAdmin(val admin: UserEntity) : AuthState
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = AppRepository(db)

    // UI States
    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminConfig = repository.adminConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSubmissions: StateFlow<List<SubmissionEntity>> = repository.allSubmissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWithdrawals: StateFlow<List<WithdrawalEntity>> = repository.allWithdrawals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val submissionDates: StateFlow<List<String>> = repository.submissionDates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Logged in user submissions
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userSubmissions: StateFlow<List<SubmissionEntity>> = authState
        .flatMapLatest { state ->
            if (state is AuthState.LoggedInUser) {
                repository.getUserSubmissions(state.user.id)
            } else if (state is AuthState.LoggedInAdmin) {
                repository.allSubmissions
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Logged in user withdrawals
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userWithdrawals: StateFlow<List<WithdrawalEntity>> = authState
        .flatMapLatest { state ->
            if (state is AuthState.LoggedInUser) {
                repository.getUserWithdrawals(state.user.id)
            } else if (state is AuthState.LoggedInAdmin) {
                repository.allWithdrawals
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Logged in user profile flow for real-time balance updates
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentProfile: StateFlow<UserEntity?> = authState
        .flatMapLatest { state ->
            when (state) {
                is AuthState.LoggedInUser -> repository.getUserFlow(state.user.id)
                is AuthState.LoggedInAdmin -> repository.getUserFlow(state.admin.id)
                else -> flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Generated Random Name for Home Page
    private val _randomName = MutableStateFlow("Tanvir Ahmed")
    val randomName: StateFlow<String> = _randomName.asStateFlow()

    private val prefs = application.getSharedPreferences("app_user_prefs", android.content.Context.MODE_PRIVATE)

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Status Message for Snackbars / Toasts
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            generateNewRandomName()

            // Auto Restore Login Session
            val savedUserId = prefs.getInt("saved_user_id", 0)
            if (savedUserId > 0) {
                var user = repository.userDao.getUserById(savedUserId)
                if (user == null) {
                    repository.syncFromRemote()
                    user = repository.userDao.getUserById(savedUserId)
                }
                if (user != null) {
                    _authState.value = if (user.isAdmin) AuthState.LoggedInAdmin(user) else AuthState.LoggedInUser(user)
                }
            }

            // Continuous background sync loop for live updates across all clients
            while (isActive) {
                try {
                    repository.syncFromRemote()
                } catch (e: Exception) {
                    // Ignore offline network errors
                }
                kotlinx.coroutines.delay(3000)
            }
        }
    }

    fun toggleTheme() {
        val next = !_isDarkTheme.value
        _isDarkTheme.value = next
        prefs.edit().putBoolean("is_dark_theme", next).apply()
    }

    fun setWithdrawPin(pin: String, onResult: (Boolean, String) -> Unit) {
        val currUser = currentProfile.value
        if (currUser == null) {
            onResult(false, "User not logged in!")
            return
        }
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            onResult(false, "অবশ্যই ৪ সংখ্যার PIN লিখুন (যেমন: 1234)।")
            return
        }
        viewModelScope.launch {
            repository.setUserWithdrawPin(currUser.id, pin)
            onResult(true, "Withdraw PIN সফলভাবে সেট করা হয়েছে!")
        }
    }

    fun resetUserPinByEmail(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.resetUserPinByEmail(email)
            res.fold(
                onSuccess = {
                    onResult(true, "User PIN সফলভাবে Reset করা হয়েছে! User এখন নতুন PIN সেট করতে পারবে।")
                },
                onFailure = { ex ->
                    onResult(false, ex.message ?: "PIN Reset ব্যর্থ হয়েছে।")
                }
            )
        }
    }

    fun refreshRemoteData() {
        viewModelScope.launch {
            repository.syncFromRemote()
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun generateNewRandomName() {
        viewModelScope.launch {
            val config = repository.adminConfigDao.getConfig()
            if (config != null) {
                val firstNames = config.randomFirstNames.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val lastNames = config.randomLastNames.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                val fn = if (firstNames.isNotEmpty()) firstNames[Random.nextInt(firstNames.size)] else "Tanvir"
                val ln = if (lastNames.isNotEmpty()) lastNames[Random.nextInt(lastNames.size)] else "Ahmed"
                _randomName.value = "$fn $ln"
            }
        }
    }

    fun login(email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = repository.loginUser(email, pass)
            if (user != null) {
                prefs.edit().putInt("saved_user_id", user.id).apply()
                if (user.isAdmin) {
                    _authState.value = AuthState.LoggedInAdmin(user)
                    onResult(true, "Admin Login successful!")
                } else {
                    _authState.value = AuthState.LoggedInUser(user)
                    onResult(true, "Login successful!")
                }
            } else {
                onResult(false, "Invalid Gmail or Password!")
            }
        }
    }

    fun register(
        name: String,
        email: String,
        pass: String,
        usedReferCode: String,
        telegram: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank() || pass.isBlank()) {
                onResult(false, "সকল আবশ্যক তথ্য দিন।")
                return@launch
            }

            val newUser = UserEntity(
                name = name.trim(),
                email = email.trim(),
                password = pass.trim(),
                referCode = "",
                usedReferCode = usedReferCode.trim(),
                telegramUsername = telegram.trim().ifBlank { "N/A" },
                balance = 0.0,
                isAdmin = false
            )

            val res = repository.registerUser(newUser, usedReferCode)
            res.fold(
                onSuccess = {
                    onResult(true, "Account তৈরি সফল হয়েছে! এখন Login করুন।")
                },
                onFailure = { ex ->
                    onResult(false, ex.message ?: "Account তৈরি ব্যর্থ হয়েছে।")
                }
            )
        }
    }

    fun logout() {
        prefs.edit().remove("saved_user_id").apply()
        _authState.value = AuthState.LoggedOut
    }

    fun submitCookieAccounts(
        category: CategoryEntity,
        assignedPassword: String,
        rawCookieText: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val state = authState.value
            if (state !is AuthState.LoggedInUser) {
                onResult(false, "অনুগ্রহ করে Login করুন।")
                return@launch
            }

            val user = state.user
            val res = repository.submitAccountCookies(
                userId = user.id,
                userName = user.name,
                userEmail = user.email,
                category = category,
                assignedPassword = assignedPassword,
                rawInput = rawCookieText
            )

            res.fold(
                onSuccess = { count ->
                    onResult(true, "$count টি account/cookie সফলভাবে submit হয়েছে।")
                },
                onFailure = { ex ->
                    onResult(false, ex.message ?: "Submission ব্যর্থ হয়েছে।")
                }
            )
        }
    }

    fun requestWithdrawal(
        method: String,
        accountDetails: String,
        amountTk: Double,
        amountUsd: Double = 0.0,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val user = currentProfile.value
            if (user == null) {
                onResult(false, "User session পাওয়া যায়নি।")
                return@launch
            }

            val res = repository.requestWithdrawal(
                userId = user.id,
                userName = user.name,
                userEmail = user.email,
                method = method,
                accountDetails = accountDetails,
                amountTk = amountTk,
                amountUsd = amountUsd
            )

            res.fold(
                onSuccess = {
                    _userMessage.value = "Withdrawal request সফলভাবে পাঠানো হয়েছে!"
                    onResult(true, "Withdrawal request সফলভাবে পাঠানো হয়েছে!")
                },
                onFailure = { ex ->
                    onResult(false, ex.message ?: "Withdrawal অনুরোধ ব্যর্থ হয়েছে।")
                }
            )
        }
    }

    // ADMIN ACTIONS
    fun toggleSubmissionAccess(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.adminConfigDao.getConfig()
            if (current != null) {
                val updated = current.copy(isSubmissionEnabled = enabled)
                repository.adminConfigDao.saveConfig(updated)
                _userMessage.value = if (enabled) "Submission ON করা হয়েছে" else "Submission OFF করা হয়েছে"
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.data.remote.SupabaseSyncService.pushAdminConfig(updated)
                }
            }
        }
    }

    fun updateAdminPassword(newPassword: String) {
        viewModelScope.launch {
            val current = repository.adminConfigDao.getConfig()
            if (current != null && newPassword.isNotBlank()) {
                val updated = current.copy(defaultPassword = newPassword.trim())
                repository.adminConfigDao.saveConfig(updated)
                _userMessage.value = "Password পরিবর্তন সফল হয়েছে"
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.data.remote.SupabaseSyncService.pushAdminConfig(updated)
                }
            }
        }
    }

    fun updateReferralBonus(bonus: Double) {
        viewModelScope.launch {
            val current = repository.adminConfigDao.getConfig()
            if (current != null && bonus >= 0) {
                val updated = current.copy(referralBonus = bonus)
                repository.adminConfigDao.saveConfig(updated)
                _userMessage.value = "রেফারেল বোনাস পরিবর্তন করা হয়েছে (৳$bonus)"
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.data.remote.SupabaseSyncService.pushAdminConfig(updated)
                }
            }
        }
    }

    fun updatePerDollarRate(rate: Double) {
        viewModelScope.launch {
            val current = repository.adminConfigDao.getConfig()
            if (current != null && rate > 0) {
                val updated = current.copy(perDollarRate = rate)
                repository.adminConfigDao.saveConfig(updated)
                _userMessage.value = "প্রতি ডলার রেট পরিবর্তন করা হয়েছে (৳$rate/$)"
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.data.remote.SupabaseSyncService.pushAdminConfig(updated)
                }
            }
        }
    }

    fun updateAdminConfig(pass: String, refBonus: Double, dollarRate: Double) {
        viewModelScope.launch {
            val current = repository.adminConfigDao.getConfig()
            if (current != null) {
                val updated = current.copy(
                    defaultPassword = pass.trim().ifBlank { current.defaultPassword },
                    referralBonus = if (refBonus >= 0) refBonus else current.referralBonus,
                    perDollarRate = if (dollarRate > 0) dollarRate else current.perDollarRate
                )
                repository.adminConfigDao.saveConfig(updated)
                _userMessage.value = "Admin settings updated!"
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.data.remote.SupabaseSyncService.pushAdminConfig(updated)
                }
            }
        }
    }

    fun processWithdrawalStatus(withdrawalId: Int, newStatus: String, onResult: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val res = repository.processWithdrawalStatus(withdrawalId, newStatus)
            res.fold(
                onSuccess = {
                    val msg = "Withdrawal Status updated to $newStatus"
                    _userMessage.value = msg
                    onResult?.invoke(msg)
                },
                onFailure = { ex ->
                    val msg = ex.message ?: "Failed to update status"
                    _userMessage.value = msg
                    onResult?.invoke(msg)
                }
            )
        }
    }

    fun addCategory(name: String, rate: Double, requiresCookie: Boolean, desc: String) {
        viewModelScope.launch {
            if (name.isNotBlank() && rate > 0) {
                val newCat = CategoryEntity(
                    name = name.trim(),
                    rate = rate,
                    requiresCookieHook = requiresCookie,
                    description = desc.trim()
                )
                val id = repository.categoryDao.insertCategory(newCat)
                _userMessage.value = "Category যোগ করা হয়েছে"
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.data.remote.SupabaseSyncService.pushCategory(newCat.copy(id = id.toInt()))
                }
            }
        }
    }

    fun updateCategoryRate(category: CategoryEntity, newRate: Double) {
        viewModelScope.launch {
            val updated = category.copy(rate = newRate)
            repository.categoryDao.updateCategory(updated)
            _userMessage.value = "${category.name} এর Rate ৳$newRate করা হয়েছে"
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.data.remote.SupabaseSyncService.pushCategory(updated)
            }
        }
    }

    fun deleteCategory(categoryId: Int) {
        viewModelScope.launch {
            repository.categoryDao.deleteCategory(categoryId)
            _userMessage.value = "Category মুছে ফেলা হয়েছে"
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.data.remote.SupabaseSyncService.deleteCategory(categoryId)
            }
        }
    }

    fun processAdminReport(
        categoryId: Int,
        dateString: String,
        successUidsText: String,
        customRate: Double? = null,
        onResult: (Int, Int) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.processAdminReport(categoryId, dateString, successUidsText, customRate)
            res.fold(
                onSuccess = { pair ->
                    _userMessage.value = "Report সম্পন্ন: Success = ${pair.first}, Rejected = ${pair.second}"
                    onResult(pair.first, pair.second)
                },
                onFailure = {
                    _userMessage.value = "Report প্রসেস ব্যর্থ হয়েছে"
                    onResult(0, 0)
                }
            )
        }
    }

    suspend fun getFormattedExportText(categoryId: Int, dateString: String): String {
        return repository.getFormattedExportText(categoryId, dateString)
    }

    suspend fun getFormattedExportCsv(categoryId: Int, dateString: String): String {
        return repository.getFormattedExportCsv(categoryId, dateString)
    }
}
