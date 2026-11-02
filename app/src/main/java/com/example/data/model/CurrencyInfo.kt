package com.example.data.model

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String
) {
    companion object {
        val ALL = listOf(
            CurrencyInfo("INR", "₹", "Indian Rupee"),
            CurrencyInfo("USD", "$", "US Dollar"),
            CurrencyInfo("EUR", "€", "Euro"),
            CurrencyInfo("GBP", "£", "British Pound"),
            CurrencyInfo("JPY", "¥", "Japanese Yen"),
            CurrencyInfo("AUD", "A$", "Australian Dollar"),
            CurrencyInfo("CAD", "C$", "Canadian Dollar")
        )

        val DEFAULT = ALL[0] // INR
    }
}
