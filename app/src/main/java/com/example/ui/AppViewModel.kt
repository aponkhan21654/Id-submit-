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

    // Status Message for Snackbars / Toasts
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            generateNewRandomName()
            repository.syncFromRemote()
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
                repository.adminConfigDao.saveConfig(current.copy(isSubmissionEnabled = enabled))
                _userMessage.value = if (enabled) "Submission ON করা হয়েছে" else "Submission OFF করা হয়েছে"
            }
        }
    }

    fun updateAdminPassword(newPassword: String) {
        viewModelScope.launch {
            val current = repository.adminConfigDao.getConfig()
            if (current != null && newPassword.isNotBlank()) {
                repository.adminConfigDao.saveConfig(current.copy(defaultPassword = newPassword.trim()))
                _userMessage.value = "Password পরিবর্তন সফল হয়েছে"
            }
        }
    }

    fun updateReferralBonus(bonus: Double) {
        viewModelScope.launch {
            val current = repository.adminConfigDao.getConfig()
            if (current != null && bonus >= 0) {
                repository.adminConfigDao.saveConfig(current.copy(referralBonus = bonus))
                _userMessage.value = "রেফারেল বোনাস পরিবর্তন করা হয়েছে (৳$bonus)"
            }
        }
    }

    fun updatePerDollarRate(rate: Double) {
        viewModelScope.launch {
            val current = repository.adminConfigDao.getConfig()
            if (current != null && rate > 0) {
                repository.adminConfigDao.saveConfig(current.copy(perDollarRate = rate))
                _userMessage.value = "প্রতি ডলার রেট পরিবর্তন করা হয়েছে (৳$rate/$)"
            }
        }
    }

    fun updateAdminConfig(pass: String, refBonus: Double, dollarRate: Double) {
        viewModelScope.launch {
            val current = repository.adminConfigDao.getConfig()
            if (current != null) {
                repository.adminConfigDao.saveConfig(
                    current.copy(
                        defaultPassword = pass.trim().ifBlank { current.defaultPassword },
                        referralBonus = if (refBonus >= 0) refBonus else current.referralBonus,
                        perDollarRate = if (dollarRate > 0) dollarRate else current.perDollarRate
                    )
                )
                _userMessage.value = "Admin settings updated!"
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
                repository.categoryDao.insertCategory(
                    CategoryEntity(
                        name = name.trim(),
                        rate = rate,
                        requiresCookieHook = requiresCookie,
                        description = desc.trim()
                    )
                )
                _userMessage.value = "Category যোগ করা হয়েছে"
            }
        }
    }

    fun updateCategoryRate(category: CategoryEntity, newRate: Double) {
        viewModelScope.launch {
            repository.categoryDao.updateCategory(category.copy(rate = newRate))
            _userMessage.value = "${category.name} এর Rate ৳$newRate করা হয়েছে"
        }
    }

    fun deleteCategory(categoryId: Int) {
        viewModelScope.launch {
            repository.categoryDao.deleteCategory(categoryId)
            _userMessage.value = "Category মুছে ফেলা হয়েছে"
        }
    }

    fun processAdminReport(
        categoryId: Int,
        dateString: String,
        successUidsText: String,
        onResult: (Int, Int) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.processAdminReport(categoryId, dateString, successUidsText)
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
