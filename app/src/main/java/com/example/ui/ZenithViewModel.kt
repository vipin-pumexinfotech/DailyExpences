package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ZenithDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.CurrencyInfo
import com.example.data.model.TransactionSortMode
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.UserProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CategorySummary(
    val categoryName: String,
    val iconName: String,
    val colorHex: String,
    val totalAmount: Double,
    val percentage: Float
)

data class MonthlyTrend(
    val monthName: String,
    val income: Double,
    val expense: Double,
    val balance: Double
)

class ZenithViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ZenithDatabase.getDatabase(application)
    private val repository = FinanceRepository(db.transactionDao(), db.categoryDao())
    private val userPrefs = UserPreferencesRepository(application)

    val currency: StateFlow<CurrencyInfo> = userPrefs.currency
    val isDarkTheme: StateFlow<Boolean> = userPrefs.isDarkTheme
    val userProfile: StateFlow<UserProfile> = userPrefs.userProfile
    val isAppLocked: StateFlow<Boolean> = userPrefs.isAppLocked

    val userAccounts: StateFlow<List<com.example.data.repository.UserAccount>> = userPrefs.userAccounts
    val selectedAccountId: StateFlow<String?> = userPrefs.selectedAccountId

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringTransactions: StateFlow<List<TransactionEntity>> = repository.recurringTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeCategories: StateFlow<List<CategoryEntity>> = repository.getCategoriesByType(TransactionType.INCOME)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseCategories: StateFlow<List<CategoryEntity>> = repository.getCategoriesByType(TransactionType.EXPENSE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter & Search states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedTypeFilter: StateFlow<TransactionType?> = _selectedTypeFilter.asStateFlow()

    private val _timeframeFilter = MutableStateFlow("Monthly") // "Monthly" or "Yearly"
    val timeframeFilter: StateFlow<String> = _timeframeFilter.asStateFlow()

    private val _selectedAnalyticsYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedAnalyticsYear: StateFlow<Int> = _selectedAnalyticsYear.asStateFlow()

    private val _selectedAnalyticsMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH)) // 0..11
    val selectedAnalyticsMonth: StateFlow<Int> = _selectedAnalyticsMonth.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun setTimeframeFilter(timeframe: String) {
        _timeframeFilter.value = timeframe
    }

    fun setSelectedAnalyticsMonthYear(month: Int, year: Int) {
        _selectedAnalyticsMonth.value = month
        _selectedAnalyticsYear.value = year
    }

    fun navigateAnalyticsMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedAnalyticsYear.value)
            set(Calendar.MONTH, _selectedAnalyticsMonth.value)
            add(Calendar.MONTH, delta)
        }
        _selectedAnalyticsYear.value = cal.get(Calendar.YEAR)
        _selectedAnalyticsMonth.value = cal.get(Calendar.MONTH)
    }

    fun resetAnalyticsToCurrentMonth() {
        val now = Calendar.getInstance()
        _selectedAnalyticsYear.value = now.get(Calendar.YEAR)
        _selectedAnalyticsMonth.value = now.get(Calendar.MONTH)
    }

    private val _transactionSortMode = MutableStateFlow(TransactionSortMode.DATE_GROUPED)
    val transactionSortMode: StateFlow<TransactionSortMode> = _transactionSortMode.asStateFlow()

    fun setTransactionSortMode(mode: TransactionSortMode) {
        _transactionSortMode.value = mode
    }

    // Filtered transaction list
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        allCategories,
        searchQuery,
        selectedTypeFilter,
        _transactionSortMode
    ) { txs, categories, query, typeFilter, sortMode ->
        val categoryMap = categories.associateBy { it.name.lowercase() }
        val matchedTxs = txs.map { tx ->
            val matchedCat = categoryMap[tx.categoryName.lowercase()]
            if (matchedCat != null && (matchedCat.colorHex != tx.categoryColorHex || matchedCat.iconName != tx.categoryIconName)) {
                tx.copy(
                    categoryColorHex = matchedCat.colorHex,
                    categoryIconName = matchedCat.iconName
                )
            } else {
                tx
            }
        }.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.title.contains(query, ignoreCase = true) ||
                    tx.categoryName.contains(query, ignoreCase = true) ||
                    tx.note.contains(query, ignoreCase = true)

            val matchesType = typeFilter == null || tx.type == typeFilter

            matchesQuery && matchesType
        }

        when (sortMode) {
            TransactionSortMode.NEWEST_FIRST, TransactionSortMode.DATE_GROUPED -> matchedTxs.sortedByDescending { it.timestamp }
            TransactionSortMode.OLDEST_FIRST -> matchedTxs.sortedBy { it.timestamp }
            TransactionSortMode.AMOUNT_HIGH_LOW -> matchedTxs.sortedByDescending { it.amount }
            TransactionSortMode.AMOUNT_LOW_HIGH -> matchedTxs.sortedBy { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated Overview Stats for Current Month
    val totalIncome: StateFlow<Double> = allTransactions.map { txs ->
        txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = allTransactions.map { txs ->
        txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Expense by Category Breakdown for Analytics
    val expenseCategoryBreakdown: StateFlow<List<CategorySummary>> = combine(
        allTransactions,
        allCategories,
        timeframeFilter,
        selectedAnalyticsMonth,
        selectedAnalyticsYear
    ) { txs, categories, timeframe, selMonth, selYear ->
        val categoryMap = categories.associateBy { it.name.lowercase() }
        val filteredExpenses = txs.filter { tx ->
            if (tx.type != TransactionType.EXPENSE) return@filter false
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            val txYear = cal.get(Calendar.YEAR)
            val txMonth = cal.get(Calendar.MONTH)

            if (timeframe == "Yearly") {
                txYear == selYear
            } else {
                txYear == selYear && txMonth == selMonth
            }
        }

        val grandTotal = filteredExpenses.sumOf { it.amount }
        if (grandTotal == 0.0) return@combine emptyList()

        filteredExpenses.groupBy { it.categoryName }
            .map { (catName, items) ->
                val sum = items.sumOf { it.amount }
                val firstItem = items.first()
                val matchedCat = categoryMap[catName.lowercase()]
                CategorySummary(
                    categoryName = catName,
                    iconName = matchedCat?.iconName ?: firstItem.categoryIconName,
                    colorHex = matchedCat?.colorHex ?: firstItem.categoryColorHex,
                    totalAmount = sum,
                    percentage = (sum / grandTotal * 100).toFloat()
                )
            }.sortedByDescending { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Historical Trends based on selected timeframe & month/year
    val monthlyTrends: StateFlow<List<MonthlyTrend>> = combine(
        allTransactions,
        timeframeFilter,
        selectedAnalyticsMonth,
        selectedAnalyticsYear
    ) { txs, timeframe, selMonth, selYear ->
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val trends = mutableListOf<MonthlyTrend>()

        if (timeframe == "Yearly") {
            for (m in 0..11) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selYear)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val label = monthFormat.format(cal.time)

                val monthTxs = txs.filter { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.YEAR) == selYear && txCal.get(Calendar.MONTH) == m
                }

                val inc = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val exp = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

                trends.add(
                    MonthlyTrend(
                        monthName = label,
                        income = inc,
                        expense = exp,
                        balance = inc - exp
                    )
                )
            }
        } else {
            for (i in 5 downTo 0) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selYear)
                    set(Calendar.MONTH, selMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, -i)
                }
                val targetYear = cal.get(Calendar.YEAR)
                val targetMonth = cal.get(Calendar.MONTH)
                val label = monthFormat.format(cal.time)

                val monthTxs = txs.filter { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.YEAR) == targetYear && txCal.get(Calendar.MONTH) == targetMonth
                }

                val inc = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val exp = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

                trends.add(
                    MonthlyTrend(
                        monthName = label,
                        income = inc,
                        expense = exp,
                        balance = inc - exp
                    )
                )
            }
        }
        trends
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Repository Actions
    fun saveTransaction(
        id: Long = 0,
        title: String,
        amount: Double,
        type: TransactionType,
        category: CategoryEntity,
        timestamp: Long,
        note: String,
        isRecurring: Boolean = false,
        recurringEndDate: Long? = null,
        recurringFrequency: String = "Monthly",
        createdAt: Long? = null
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = TransactionEntity(
                id = id,
                title = title,
                amount = amount,
                type = type,
                categoryId = category.id,
                categoryName = category.name,
                categoryIconName = category.iconName,
                categoryColorHex = category.colorHex,
                timestamp = timestamp,
                note = note,
                isRecurring = isRecurring,
                recurringEndDate = recurringEndDate,
                recurringFrequency = recurringFrequency,
                createdAt = createdAt ?: now,
                updatedAt = now
            )
            if (id == 0L) {
                repository.insertTransaction(entity)
            } else {
                repository.updateTransaction(entity)
            }
        }
    }

    fun isRecurringLoggedThisMonth(recurringTx: TransactionEntity): Boolean {
        val txs = allTransactions.value
        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)

        val txCal = Calendar.getInstance()
        return txs.any { tx ->
            txCal.timeInMillis = tx.timestamp
            val isSameMonth = txCal.get(Calendar.YEAR) == currentYear && txCal.get(Calendar.MONTH) == currentMonth
            isSameMonth && tx.title.equals(recurringTx.title, ignoreCase = true) && tx.type == recurringTx.type
        }
    }

    fun logRecurringForCurrentMonth(recurringTx: TransactionEntity): Boolean {
        if (isRecurringLoggedThisMonth(recurringTx)) {
            return false
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val logEntity = TransactionEntity(
                title = recurringTx.title,
                amount = recurringTx.amount,
                type = recurringTx.type,
                categoryId = recurringTx.categoryId,
                categoryName = recurringTx.categoryName,
                categoryIconName = recurringTx.categoryIconName,
                categoryColorHex = recurringTx.categoryColorHex,
                timestamp = now,
                note = "Recurring payment logged (${recurringTx.recurringFrequency})",
                isRecurring = false,
                createdAt = now,
                updatedAt = now
            )
            repository.insertTransaction(logEntity)
        }
        return true
    }

    fun stopRecurringExpense(recurringTx: TransactionEntity) {
        viewModelScope.launch {
            val updatedEntity = recurringTx.copy(
                isRecurring = false,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateTransaction(updatedEntity)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun saveCategory(
        id: Long = 0,
        name: String,
        type: TransactionType,
        iconName: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            var oldCatName: String? = null
            if (id != 0L) {
                val existingCat = allCategories.value.find { it.id == id }
                oldCatName = existingCat?.name
            }

            val category = CategoryEntity(
                id = id,
                name = name,
                type = type,
                iconName = iconName,
                colorHex = colorHex,
                isDefault = false
            )
            if (id == 0L) {
                repository.insertCategory(category)
            } else {
                repository.updateCategory(category)
            }

            // Also update all existing transactions in database matching this category
            val targetName = oldCatName ?: name
            val currentTxs = allTransactions.value
            currentTxs.filter { it.categoryName.equals(targetName, ignoreCase = true) || (id != 0L && it.categoryId == id) }.forEach { tx ->
                val updatedTx = tx.copy(
                    categoryName = name,
                    categoryColorHex = colorHex,
                    categoryIconName = iconName
                )
                repository.updateTransaction(updatedTx)
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun setCurrency(currencyInfo: CurrencyInfo) {
        userPrefs.setCurrency(currencyInfo)
    }

    fun setDarkTheme(isDark: Boolean) {
        userPrefs.setDarkTheme(isDark)
    }

    fun saveUserProfile(
        name: String,
        email: String,
        pin: String? = null,
        enablePin: Boolean = false,
        enableBiometric: Boolean = false
    ) {
        userPrefs.saveProfile(
            name = name,
            email = email,
            isLoggedIn = true,
            pin = pin,
            enablePin = enablePin,
            enableBiometric = enableBiometric
        )
    }

    fun setSecurityPin(pin: String, enablePin: Boolean = true) {
        userPrefs.setSecurityPin(pin, enablePin)
    }

    fun setSecurityPinEnabled(enabled: Boolean) {
        userPrefs.setSecurityPinEnabled(enabled)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        userPrefs.setBiometricEnabled(enabled)
    }

    fun selectAccount(accountId: String) {
        userPrefs.selectAccount(accountId)
    }

    fun unlockAppForAccount(accountId: String) {
        userPrefs.unlockAppForAccount(accountId)
    }

    fun deleteAccount(accountId: String) {
        userPrefs.deleteAccount(accountId)
    }

    fun verifyPin(inputPin: String, accountId: String? = null): Boolean {
        return userPrefs.verifyPin(inputPin, accountId)
    }

    fun unlockApp() {
        userPrefs.unlockApp()
    }

    fun lockApp() {
        userPrefs.lockApp()
    }

    fun signOut() {
        userPrefs.signOut()
    }

    fun resetOnboardingAndProfile() {
        userPrefs.clearProfileAndResetOnboarding()
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.deleteAllTransactions()
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun exportTransactionsCSV(): String {
        val txs = allTransactions.value
        val sb = StringBuilder()
        sb.append("ID,Title,Amount,Type,Category,Date,Note,IsRecurring\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        txs.forEach { tx ->
            val dateStr = dateFormat.format(Date(tx.timestamp))
            sb.append("${tx.id},\"${tx.title.replace("\"", "\"\"")}\",${tx.amount},${tx.type},\"${tx.categoryName}\",\"$dateStr\",\"${tx.note.replace("\"", "\"\"")}\",${tx.isRecurring}\n")
        }
        return sb.toString()
    }

    fun exportBackupJSON(): String {
        val txs = allTransactions.value
        val cats = allCategories.value
        val profile = userProfile.value

        val jsonArrayTxs = StringBuilder()
        txs.forEachIndexed { idx, tx ->
            jsonArrayTxs.append("""
                {
                  "id": ${tx.id},
                  "title": "${tx.title.replace("\"", "\\\"")}",
                  "amount": ${tx.amount},
                  "type": "${tx.type}",
                  "categoryId": ${tx.categoryId},
                  "categoryName": "${tx.categoryName.replace("\"", "\\\"")}",
                  "categoryIconName": "${tx.categoryIconName}",
                  "categoryColorHex": "${tx.categoryColorHex}",
                  "timestamp": ${tx.timestamp},
                  "note": "${tx.note.replace("\"", "\\\"")}",
                  "isRecurring": ${tx.isRecurring},
                  "recurringEndDate": ${tx.recurringEndDate ?: "null"},
                  "recurringFrequency": "${tx.recurringFrequency}",
                  "createdAt": ${tx.createdAt},
                  "updatedAt": ${tx.updatedAt}
                }${if (idx < txs.size - 1) "," else ""}
            """.trimIndent())
        }

        val jsonArrayCats = StringBuilder()
        cats.forEachIndexed { idx, cat ->
            jsonArrayCats.append("""
                {
                  "id": ${cat.id},
                  "name": "${cat.name.replace("\"", "\\\"")}",
                  "type": "${cat.type}",
                  "iconName": "${cat.iconName}",
                  "colorHex": "${cat.colorHex}",
                  "isDefault": ${cat.isDefault}
                }${if (idx < cats.size - 1) "," else ""}
            """.trimIndent())
        }

        return """
            {
              "appName": "SpendWise",
              "version": 2,
              "exportDate": ${System.currentTimeMillis()},
              "user": {
                "name": "${profile.name.replace("\"", "\\\"")}",
                "email": "${profile.email.replace("\"", "\\\"")}"
              },
              "categories": [
                $jsonArrayCats
              ],
              "transactions": [
                $jsonArrayTxs
              ]
            }
        """.trimIndent()
    }

    fun importBackupJSON(jsonStr: String): Boolean {
        return try {
            val orgJson = org.json.JSONObject(jsonStr)
            if (!orgJson.has("transactions")) return false

            val txArray = orgJson.getJSONArray("transactions")
            val importedTxs = mutableListOf<TransactionEntity>()

            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                val now = System.currentTimeMillis()
                importedTxs.add(
                    TransactionEntity(
                        title = obj.getString("title"),
                        amount = obj.getDouble("amount"),
                        type = TransactionType.valueOf(obj.getString("type")),
                        categoryId = obj.optLong("categoryId", 1L),
                        categoryName = obj.optString("categoryName", "General"),
                        categoryIconName = obj.optString("categoryIconName", "Category"),
                        categoryColorHex = obj.optString("categoryColorHex", "#3B82F6"),
                        timestamp = obj.optLong("timestamp", now),
                        note = obj.optString("note", ""),
                        isRecurring = obj.optBoolean("isRecurring", false),
                        recurringEndDate = if (obj.has("recurringEndDate") && !obj.isNull("recurringEndDate")) obj.getLong("recurringEndDate") else null,
                        recurringFrequency = obj.optString("recurringFrequency", "Monthly"),
                        createdAt = obj.optLong("createdAt", now),
                        updatedAt = obj.optLong("updatedAt", now)
                    )
                )
            }

            viewModelScope.launch {
                if (importedTxs.isNotEmpty()) {
                    repository.insertTransactions(importedTxs)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importParsedTransactions(items: List<com.example.ui.components.ParsedTransactionItem>) {
        viewModelScope.launch {
            val categories = repository.allCategories.first()
            val now = System.currentTimeMillis()
            val entities = items.filter { it.isSelected && it.amount > 0 }.map { item ->
                val matchedCat = categories.find { cat ->
                    cat.name.equals(item.categoryName, ignoreCase = true) && cat.type == item.type
                } ?: categories.find { cat -> cat.type == item.type }
                  ?: categories.first()

                TransactionEntity(
                    title = item.title.ifBlank { "Expense Import" },
                    amount = item.amount,
                    type = item.type,
                    categoryId = matchedCat.id,
                    categoryName = matchedCat.name,
                    categoryIconName = matchedCat.iconName,
                    categoryColorHex = matchedCat.colorHex,
                    timestamp = item.timestamp,
                    note = item.note.ifBlank { "Imported from monthly expense sheet" },
                    isRecurring = false,
                    createdAt = now,
                    updatedAt = now
                )
            }
            if (entities.isNotEmpty()) {
                repository.insertTransactions(entities)
            }
        }
    }
}
