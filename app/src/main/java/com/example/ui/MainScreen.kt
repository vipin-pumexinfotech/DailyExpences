package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.about.AboutScreen
import com.example.ui.analytics.AnalyticsScreen
import com.example.ui.auth.LockScreen
import com.example.ui.auth.OnboardingScreen
import com.example.ui.categories.CategoriesScreen
import com.example.ui.components.AddCategoryModal
import com.example.ui.components.AddTransactionModal
import com.example.ui.components.AuthModal
import com.example.ui.components.ImportExpenseSheetModal
import com.example.ui.components.RecurringExpensesModal
import com.example.ui.home.HomeScreen
import com.example.ui.settings.SettingsScreen

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    ANALYTICS("Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    CATEGORIES("Categories", Icons.Filled.Category, Icons.Outlined.Category),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun MainScreen(viewModel: ZenithViewModel) {
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }

    // Modal state controllers
    var showAddTransactionModal by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    var showAddCategoryModal by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var newCategoryType by remember { mutableStateOf(TransactionType.EXPENSE) }

    var showAuthModal by remember { mutableStateOf(false) }
    var showRecurringModal by remember { mutableStateOf(false) }
    var showImportSheetModal by remember { mutableStateOf(false) }
    var showAboutPage by remember { mutableStateOf(false) }

    var isCreatingNewUser by remember { mutableStateOf(false) }

    // State flows from ViewModel
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val userAccounts by viewModel.userAccounts.collectAsStateWithLifecycle()
    val selectedAccountId by viewModel.selectedAccountId.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val recurringTransactions by viewModel.recurringTransactions.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()

    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val transactionSortMode by viewModel.transactionSortMode.collectAsStateWithLifecycle()

    val timeframeFilter by viewModel.timeframeFilter.collectAsStateWithLifecycle()
    val selectedAnalyticsMonth by viewModel.selectedAnalyticsMonth.collectAsStateWithLifecycle()
    val selectedAnalyticsYear by viewModel.selectedAnalyticsYear.collectAsStateWithLifecycle()
    val expenseBreakdown by viewModel.expenseCategoryBreakdown.collectAsStateWithLifecycle()
    val monthlyTrends by viewModel.monthlyTrends.collectAsStateWithLifecycle()

    // Show Onboarding Screen only when explicitly requested (e.g., Create New Account)
    if (isCreatingNewUser) {
        OnboardingScreen(
            onCompleteOnboarding = { name, email, selectedCurrency, pin, enablePin, enableBiometric ->
                viewModel.setCurrency(selectedCurrency)
                viewModel.saveUserProfile(
                    name = name,
                    email = email,
                    pin = pin,
                    enablePin = enablePin,
                    enableBiometric = enableBiometric
                )
                isCreatingNewUser = false
            },
            onCancelOnboarding = { isCreatingNewUser = false }
        )
        return
    }

    // Lock screen gate (Only active if security PIN is enabled and app is locked)
    if (userProfile.isSecurityPinEnabled && userProfile.hasPinSet && isAppLocked) {
        LockScreen(
            userAccounts = userAccounts,
            selectedAccountId = selectedAccountId,
            onSelectAccount = { id -> viewModel.selectAccount(id) },
            onVerifyPin = { pin, accId -> viewModel.verifyPin(pin, accId) },
            onUnlockSuccess = { accId -> viewModel.unlockAppForAccount(accId) },
            onCreateNewAccount = { isCreatingNewUser = true },
            onDeleteAccount = { accId -> viewModel.deleteAccount(accId) }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.HOME -> {
                    HomeScreen(
                        userProfile = userProfile,
                        currency = currency,
                        totalBalance = totalBalance,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        transactions = filteredTransactions,
                        recurringCount = recurringTransactions.size,
                        searchQuery = searchQuery,
                        selectedTypeFilter = selectedTypeFilter,
                        sortMode = transactionSortMode,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onTypeFilterChange = { viewModel.setTypeFilter(it) },
                        onSortModeChange = { viewModel.setTransactionSortMode(it) },
                        onAddTransactionClick = {
                            editingTransaction = null
                            showAddTransactionModal = true
                        },
                        onRecurringExpensesClick = {
                            showRecurringModal = true
                        },
                        onImportExpenseSheetClick = {
                            showImportSheetModal = true
                        },
                        onTransactionClick = { tx ->
                            editingTransaction = tx
                            showAddTransactionModal = true
                        },
                        onProfileClick = { showAuthModal = true }
                    )
                }

                MainTab.ANALYTICS -> {
                    AnalyticsScreen(
                        timeframe = timeframeFilter,
                        onTimeframeChange = { viewModel.setTimeframeFilter(it) },
                        selectedMonth = selectedAnalyticsMonth,
                        selectedYear = selectedAnalyticsYear,
                        onMonthYearChange = { month, year -> viewModel.setSelectedAnalyticsMonthYear(month, year) },
                        onNavigateMonth = { delta -> viewModel.navigateAnalyticsMonth(delta) },
                        onResetToCurrentMonth = { viewModel.resetAnalyticsToCurrentMonth() },
                        currency = currency,
                        expenseBreakdown = expenseBreakdown,
                        monthlyTrends = monthlyTrends
                    )
                }

                MainTab.CATEGORIES -> {
                    CategoriesScreen(
                        expenseCategories = expenseCategories,
                        incomeCategories = incomeCategories,
                        onAddCategoryClick = { type ->
                            editingCategory = null
                            newCategoryType = type
                            showAddCategoryModal = true
                        },
                        onCategoryClick = { cat ->
                            editingCategory = cat
                            showAddCategoryModal = true
                        }
                    )
                }

                MainTab.SETTINGS -> {
                    SettingsScreen(
                        userProfile = userProfile,
                        currency = currency,
                        isDarkTheme = isDarkTheme,
                        onCurrencySelect = { viewModel.setCurrency(it) },
                        onDarkThemeToggle = { viewModel.setDarkTheme(it) },
                        onProfileClick = { showAuthModal = true },
                        onExportCSV = { viewModel.exportTransactionsCSV() },
                        onExportJSON = { viewModel.exportBackupJSON() },
                        onImportJSON = { viewModel.importBackupJSON(it) },
                        onImportExpenseSheetClick = { showImportSheetModal = true },
                        onAboutClick = { showAboutPage = true },
                        onTogglePinLock = { viewModel.setSecurityPinEnabled(it) },
                        onSetPin = { viewModel.setSecurityPin(it) },
                        onVerifyPin = { viewModel.verifyPin(it) },
                        onLockApp = { viewModel.lockApp() },
                        onResetData = { viewModel.resetAllData() },
                        onLoadSampleData = { viewModel.loadSampleData() },
                        onLogout = { viewModel.signOut() }
                    )
                }
            }
        }
    }

    if (showAboutPage) {
        AboutScreen(
            onBackClick = { showAboutPage = false }
        )
    }

    // Modal Bottom Sheets
    if (showAddTransactionModal) {
        AddTransactionModal(
            initialTransaction = editingTransaction,
            categories = allCategories,
            currency = currency,
            onDismiss = {
                showAddTransactionModal = false
                editingTransaction = null
            },
            onSave = { id, title, amount, type, category, timestamp, note, isRecurring, recurringEndDate, recurringFrequency, createdAt ->
                viewModel.saveTransaction(
                    id = id,
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    timestamp = timestamp,
                    note = note,
                    isRecurring = isRecurring,
                    recurringEndDate = recurringEndDate,
                    recurringFrequency = recurringFrequency,
                    createdAt = createdAt
                )
            },
            onDelete = { tx ->
                viewModel.deleteTransaction(tx)
            }
        )
    }

    if (showAddCategoryModal) {
        AddCategoryModal(
            initialCategory = editingCategory,
            defaultType = newCategoryType,
            onDismiss = {
                showAddCategoryModal = false
                editingCategory = null
            },
            onSave = { id, name, type, iconName, colorHex ->
                viewModel.saveCategory(id, name, type, iconName, colorHex)
            },
            onDelete = { cat ->
                viewModel.deleteCategory(cat)
            }
        )
    }

    if (showRecurringModal) {
        RecurringExpensesModal(
            recurringList = recurringTransactions,
            currency = currency,
            onDismiss = { showRecurringModal = false },
            isLoggedThisMonth = { tx -> viewModel.isRecurringLoggedThisMonth(tx) },
            onLogCurrentMonth = { tx -> viewModel.logRecurringForCurrentMonth(tx) },
            onStopRepeating = { tx -> viewModel.stopRecurringExpense(tx) },
            onEdit = { tx ->
                editingTransaction = tx
                showRecurringModal = false
                showAddTransactionModal = true
            },
            onDelete = { tx -> viewModel.deleteTransaction(tx) }
        )
    }

    if (showAuthModal) {
        AuthModal(
            currentProfile = userProfile,
            onDismiss = { showAuthModal = false },
            onSaveProfile = { name, email ->
                viewModel.saveUserProfile(name, email)
            },
            onSignOut = {
                viewModel.signOut()
            }
        )
    }

    if (showImportSheetModal) {
        ImportExpenseSheetModal(
            categories = allCategories,
            currency = currency,
            onDismiss = { showImportSheetModal = false },
            onImportConfirmed = { parsedList ->
                viewModel.importParsedTransactions(parsedList)
            }
        )
    }
}
