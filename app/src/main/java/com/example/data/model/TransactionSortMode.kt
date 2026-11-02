package com.example.data.model

enum class TransactionSortMode(val label: String, val description: String) {
    DATE_GROUPED("Grouped by Date", "Groups transactions by day with daily expense totals"),
    NEWEST_FIRST("Newest First", "Sorts transactions by latest date"),
    OLDEST_FIRST("Oldest First", "Sorts transactions by earliest date"),
    AMOUNT_HIGH_LOW("Highest Amount", "Sorts by largest transaction amount"),
    AMOUNT_LOW_HIGH("Lowest Amount", "Sorts by smallest transaction amount")
}
