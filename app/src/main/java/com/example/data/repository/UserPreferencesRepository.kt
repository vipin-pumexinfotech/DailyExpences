package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CurrencyInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class UserAccount(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val pin: String = "",
    val isSecurityPinEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val currencyCode: String = "INR",
    val avatarColorHex: String = "#3F51B5"
)

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val isLoggedIn: Boolean = false,
    val token: String? = null,
    val isFirstLaunch: Boolean = true,
    val isSecurityPinEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val hasPinSet: Boolean = false,
    val avatarColorHex: String = "#3F51B5"
)

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("zenith_user_prefs", Context.MODE_PRIVATE)

    private val _userAccounts = MutableStateFlow(getSavedAccounts())
    val userAccounts: StateFlow<List<UserAccount>> = _userAccounts.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<String?>(getInitialSelectedAccountId())
    val selectedAccountId: StateFlow<String?> = _selectedAccountId.asStateFlow()

    private val _currency = MutableStateFlow(getSavedCurrency())
    val currency: StateFlow<CurrencyInfo> = _currency.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(getSavedTheme())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _userProfile = MutableStateFlow(getSavedProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _isAppLocked = MutableStateFlow(checkInitialAppLockedState())
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private fun getSavedAccounts(): List<UserAccount> {
        val jsonStr = prefs.getString("all_user_accounts_json", "") ?: ""
        val accounts = deserializeAccounts(jsonStr)

        if (accounts.isEmpty()) {
            // Check legacy profile migration
            val legacyName = prefs.getString("user_name", "") ?: ""
            val legacyEmail = prefs.getString("user_email", "") ?: ""
            val legacyPin = prefs.getString("security_pin", "") ?: ""
            val legacyPinEnabled = prefs.getBoolean("is_security_pin_enabled", false)
            val legacyBioEnabled = prefs.getBoolean("is_biometric_enabled", false)
            val legacyCurrency = prefs.getString("currency_code", "INR") ?: "INR"

            val defaultAccount = UserAccount(
                id = "usr_primary",
                name = if (legacyName.isNotEmpty()) legacyName else "Primary User",
                email = if (legacyEmail.isNotEmpty()) legacyEmail else "user@spendwise.app",
                pin = legacyPin,
                isSecurityPinEnabled = legacyPinEnabled,
                isBiometricEnabled = legacyBioEnabled,
                currencyCode = legacyCurrency
            )
            val migrated = listOf(defaultAccount)
            saveAccountsToPrefs(migrated)
            return migrated
        }
        return accounts
    }

    private fun saveAccountsToPrefs(accounts: List<UserAccount>) {
        prefs.edit().putString("all_user_accounts_json", serializeAccounts(accounts)).apply()
        _userAccounts.value = accounts
    }

    private fun getInitialSelectedAccountId(): String? {
        val accounts = getSavedAccounts()
        val activeId = prefs.getString("active_account_id", null)
        if (!activeId.isNull_or_empty_safe() && accounts.any { it.id == activeId }) {
            return activeId
        }
        return accounts.firstOrNull()?.id
    }

    private fun getSavedCurrency(): CurrencyInfo {
        val selectedAcc = getSelectedAccountObj()
        val code = selectedAcc?.currencyCode ?: prefs.getString("currency_code", "INR") ?: "INR"
        return CurrencyInfo.ALL.find { it.code == code } ?: CurrencyInfo.DEFAULT
    }

    fun setCurrency(currencyInfo: CurrencyInfo) {
        prefs.edit().putString("currency_code", currencyInfo.code).apply()
        _currency.value = currencyInfo

        // Update selected account's currency
        val currentSelectedId = _selectedAccountId.value ?: return
        val accounts = _userAccounts.value.map { acc ->
            if (acc.id == currentSelectedId) acc.copy(currencyCode = currencyInfo.code) else acc
        }
        saveAccountsToPrefs(accounts)
    }

    private fun getSavedTheme(): Boolean {
        return prefs.getBoolean("is_dark_theme", true)
    }

    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean("is_dark_theme", isDark).apply()
        _isDarkTheme.value = isDark
    }

    fun getSelectedAccountObj(): UserAccount? {
        val accounts = _userAccounts.value
        val selectedId = _selectedAccountId.value
        return accounts.find { it.id == selectedId } ?: accounts.firstOrNull()
    }

    private fun getSavedProfile(): UserProfile {
        val accounts = getSavedAccounts()
        val activeId = prefs.getString("active_account_id", null)
        val selectedAcc = accounts.find { it.id == activeId } ?: accounts.find { it.id == _selectedAccountId.value } ?: accounts.firstOrNull()

        val isLoggedIn = prefs.getBoolean("user_logged_in", true)
        val token = prefs.getString("jwt_token", null)

        if (selectedAcc == null) {
            return UserProfile(isFirstLaunch = false, isLoggedIn = true)
        }

        return UserProfile(
            id = selectedAcc.id,
            name = selectedAcc.name,
            email = selectedAcc.email,
            isLoggedIn = isLoggedIn,
            token = token,
            isFirstLaunch = false,
            isSecurityPinEnabled = selectedAcc.isSecurityPinEnabled,
            isBiometricEnabled = selectedAcc.isBiometricEnabled,
            hasPinSet = selectedAcc.pin.isNotEmpty(),
            avatarColorHex = selectedAcc.avatarColorHex
        )
    }

    private fun checkInitialAppLockedState(): Boolean {
        val profile = getSavedProfile()
        return profile.isSecurityPinEnabled && profile.hasPinSet
    }

    fun selectAccount(accountId: String) {
        _selectedAccountId.value = accountId
        prefs.edit().putString("active_account_id", accountId).apply()
        _userProfile.value = getSavedProfile()
        _currency.value = getSavedCurrency()
    }

    fun saveProfile(
        name: String,
        email: String,
        isLoggedIn: Boolean,
        token: String? = null,
        pin: String? = null,
        enablePin: Boolean = false,
        enableBiometric: Boolean = false
    ) {
        val genToken = token ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.user_${System.currentTimeMillis()}"
        val accounts = _userAccounts.value.toMutableList()

        val existingAccIndex = accounts.indexOfFirst { it.email.equals(email.trim(), ignoreCase = true) }
        val avatarColors = listOf("#3F51B5", "#009688", "#E91E63", "#9C27B0", "#FF9800", "#4CAF50")
        val randomColor = avatarColors[accounts.size % avatarColors.size]

        val accountId = if (existingAccIndex >= 0) accounts[existingAccIndex].id else UUID.randomUUID().toString()
        val updatedPin = if (!pin.isNull_or_empty_safe()) pin!! else if (existingAccIndex >= 0) accounts[existingAccIndex].pin else ""

        val newAccount = UserAccount(
            id = accountId,
            name = name,
            email = email,
            pin = updatedPin,
            isSecurityPinEnabled = enablePin,
            isBiometricEnabled = enableBiometric,
            currencyCode = _currency.value.code,
            avatarColorHex = if (existingAccIndex >= 0) accounts[existingAccIndex].avatarColorHex else randomColor
        )

        if (existingAccIndex >= 0) {
            accounts[existingAccIndex] = newAccount
        } else {
            accounts.add(newAccount)
        }

        saveAccountsToPrefs(accounts)
        _selectedAccountId.value = accountId

        prefs.edit()
            .putString("active_account_id", accountId)
            .putString("user_name", name)
            .putString("user_email", email)
            .putBoolean("user_logged_in", isLoggedIn)
            .putBoolean("is_first_launch", false)
            .putString("jwt_token", if (isLoggedIn) genToken else null)
            .putBoolean("is_security_pin_enabled", enablePin)
            .putBoolean("is_biometric_enabled", enableBiometric)
            .apply()

        _userProfile.value = getSavedProfile()
        _isAppLocked.value = !isLoggedIn
    }

    fun setSecurityPin(pin: String, enablePin: Boolean = true) {
        val selectedAcc = getSelectedAccountObj() ?: return
        val updatedAccounts = _userAccounts.value.map { acc ->
            if (acc.id == selectedAcc.id) {
                acc.copy(pin = pin, isSecurityPinEnabled = enablePin)
            } else acc
        }
        saveAccountsToPrefs(updatedAccounts)
        prefs.edit().putString("security_pin", pin).putBoolean("is_security_pin_enabled", enablePin).apply()
        _userProfile.value = getSavedProfile()
    }

    fun setSecurityPinEnabled(enabled: Boolean) {
        val selectedAcc = getSelectedAccountObj() ?: return
        val updatedAccounts = _userAccounts.value.map { acc ->
            if (acc.id == selectedAcc.id) {
                acc.copy(isSecurityPinEnabled = enabled)
            } else acc
        }
        saveAccountsToPrefs(updatedAccounts)
        prefs.edit().putBoolean("is_security_pin_enabled", enabled).apply()
        _userProfile.value = getSavedProfile()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        val selectedAcc = getSelectedAccountObj() ?: return
        val updatedAccounts = _userAccounts.value.map { acc ->
            if (acc.id == selectedAcc.id) {
                acc.copy(isBiometricEnabled = enabled)
            } else acc
        }
        saveAccountsToPrefs(updatedAccounts)
        prefs.edit().putBoolean("is_biometric_enabled", enabled).apply()
        _userProfile.value = getSavedProfile()
    }

    fun verifyPin(inputPin: String, accountId: String? = null): Boolean {
        val targetId = accountId ?: _selectedAccountId.value ?: return false
        val account = _userAccounts.value.find { it.id == targetId } ?: return false
        return account.pin.isNotEmpty() && account.pin == inputPin
    }

    fun unlockAppForAccount(accountId: String) {
        selectAccount(accountId)
        prefs.edit().putBoolean("user_logged_in", true).apply()
        _userProfile.value = getSavedProfile()
        _isAppLocked.value = false
    }

    fun unlockApp() {
        prefs.edit().putBoolean("user_logged_in", true).apply()
        _userProfile.value = getSavedProfile()
        _isAppLocked.value = false
    }

    fun lockApp() {
        _isAppLocked.value = true
    }

    fun signOut() {
        prefs.edit()
            .putBoolean("user_logged_in", false)
            .remove("jwt_token")
            .apply()

        _userProfile.value = getSavedProfile()
        _isAppLocked.value = true
    }

    fun deleteAccount(accountId: String) {
        val currentAccounts = _userAccounts.value.filter { it.id != accountId }
        saveAccountsToPrefs(currentAccounts)

        if (currentAccounts.isEmpty()) {
            clearProfileAndResetOnboarding()
        } else {
            selectAccount(currentAccounts.first().id)
            signOut()
        }
    }

    fun clearProfileAndResetOnboarding() {
        prefs.edit()
            .remove("all_user_accounts_json")
            .remove("active_account_id")
            .remove("user_name")
            .remove("user_email")
            .putBoolean("user_logged_in", false)
            .putBoolean("is_first_launch", true)
            .remove("security_pin")
            .putBoolean("is_security_pin_enabled", false)
            .putBoolean("is_biometric_enabled", false)
            .remove("jwt_token")
            .apply()

        _userAccounts.value = emptyList()
        _selectedAccountId.value = null
        _userProfile.value = UserProfile(isFirstLaunch = true)
        _isAppLocked.value = true
    }

    private fun serializeAccounts(accounts: List<UserAccount>): String {
        val array = JSONArray()
        accounts.forEach { acc ->
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("email", acc.email)
                put("pin", acc.pin)
                put("isSecurityPinEnabled", acc.isSecurityPinEnabled)
                put("isBiometricEnabled", acc.isBiometricEnabled)
                put("currencyCode", acc.currencyCode)
                put("avatarColorHex", acc.avatarColorHex)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeAccounts(jsonStr: String): List<UserAccount> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<UserAccount>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    UserAccount(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "User"),
                        email = obj.optString("email", ""),
                        pin = obj.optString("pin", ""),
                        isSecurityPinEnabled = obj.optBoolean("isSecurityPinEnabled", false),
                        isBiometricEnabled = obj.optBoolean("isBiometricEnabled", false),
                        currencyCode = obj.optString("currencyCode", "INR"),
                        avatarColorHex = obj.optString("avatarColorHex", "#3F51B5")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}

private fun String?.isNull_or_empty_safe(): Boolean = this == null || this.trim().isEmpty()

