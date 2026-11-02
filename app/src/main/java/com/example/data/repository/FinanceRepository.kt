package com.example.data.repository

import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val recurringTransactions: Flow<List<TransactionEntity>> = transactionDao.getRecurringTransactions()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>> {
        return categoryDao.getCategoriesByType(type)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun insertTransactions(transactions: List<TransactionEntity>) {
        transactionDao.insertTransactions(transactions)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    suspend fun insertCategory(category: CategoryEntity): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun seedInitialDataIfEmpty() {
        if (categoryDao.getCategoryCount() == 0) {
            val defaultCategories = listOf(
                // Expense categories
                CategoryEntity(name = "Groceries", type = TransactionType.EXPENSE, iconName = "ShoppingCart", colorHex = "#3B82F6", isDefault = true),
                CategoryEntity(name = "Dining & Food", type = TransactionType.EXPENSE, iconName = "Restaurant", colorHex = "#F59E0B", isDefault = true),
                CategoryEntity(name = "Shopping", type = TransactionType.EXPENSE, iconName = "ShoppingBag", colorHex = "#EC4899", isDefault = true),
                CategoryEntity(name = "Transport", type = TransactionType.EXPENSE, iconName = "DirectionsCar", colorHex = "#10B981", isDefault = true),
                CategoryEntity(name = "Utilities", type = TransactionType.EXPENSE, iconName = "Bolt", colorHex = "#8B5CF6", isDefault = true),
                CategoryEntity(name = "Entertainment", type = TransactionType.EXPENSE, iconName = "Movie", colorHex = "#6366F1", isDefault = true),
                CategoryEntity(name = "Healthcare", type = TransactionType.EXPENSE, iconName = "LocalHospital", colorHex = "#EF4444", isDefault = true),
                CategoryEntity(name = "Education", type = TransactionType.EXPENSE, iconName = "School", colorHex = "#06B6D4", isDefault = true),

                // Income categories
                CategoryEntity(name = "Salary", type = TransactionType.INCOME, iconName = "Work", colorHex = "#10B981", isDefault = true),
                CategoryEntity(name = "Freelance", type = TransactionType.INCOME, iconName = "Computer", colorHex = "#3B82F6", isDefault = true),
                CategoryEntity(name = "Investment", type = TransactionType.INCOME, iconName = "TrendingUp", colorHex = "#8B5CF6", isDefault = true),
                CategoryEntity(name = "Bonus", type = TransactionType.INCOME, iconName = "Redeem", colorHex = "#F59E0B", isDefault = true),
                CategoryEntity(name = "Rental", type = TransactionType.INCOME, iconName = "Home", colorHex = "#06B6D4", isDefault = true)
            )
            categoryDao.insertCategories(defaultCategories)
        }
    }
}
