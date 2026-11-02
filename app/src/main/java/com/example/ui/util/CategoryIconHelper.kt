package com.example.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    val AVAILABLE_ICONS = listOf(
        "ShoppingCart" to Icons.Default.ShoppingCart,
        "Restaurant" to Icons.Default.Restaurant,
        "ShoppingBag" to Icons.Default.ShoppingBag,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "Bolt" to Icons.Default.Bolt,
        "Movie" to Icons.Default.Movie,
        "LocalHospital" to Icons.Default.LocalHospital,
        "School" to Icons.Default.School,
        "Work" to Icons.Default.Work,
        "Computer" to Icons.Default.Computer,
        "TrendingUp" to Icons.AutoMirrored.Filled.TrendingUp,
        "Redeem" to Icons.Default.Redeem,
        "Home" to Icons.Default.Home,
        "Fastfood" to Icons.Default.Fastfood,
        "Flight" to Icons.Default.Flight,
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "LocalGasStation" to Icons.Default.LocalGasStation,
        "CardGiftcard" to Icons.Default.CardGiftcard,
        "Pets" to Icons.Default.Pets,
        "Subscriptions" to Icons.Default.Subscriptions,
        "PhoneIphone" to Icons.Default.PhoneIphone,
        "ReceiptLong" to Icons.Default.ReceiptLong
    )

    fun getIcon(name: String): ImageVector {
        return AVAILABLE_ICONS.find { it.first == name }?.second ?: Icons.Default.Category
    }

    val PRESET_COLORS = listOf(
        "#10B981", "#3B82F6", "#F59E0B", "#EF4444",
        "#8B5CF6", "#EC4899", "#06B6D4", "#6366F1",
        "#14B8A6", "#F97316", "#84CC16", "#A855F7",
        "#D97706", "#DC2626", "#2563EB", "#059669"
    )

    fun parseColor(hex: String, defaultColor: Color = Color(0xFF10B981)): Color {
        return try {
            val cleanHex = hex.removePrefix("#").trim()
            val colorInt = cleanHex.toLong(16)
            if (cleanHex.length == 6) {
                Color(0xFF000000 or colorInt)
            } else if (cleanHex.length == 8) {
                Color(colorInt)
            } else {
                defaultColor
            }
        } catch (e: Exception) {
            defaultColor
        }
    }

    fun isValidHex(hex: String): Boolean {
        val cleanHex = hex.removePrefix("#").trim()
        if (cleanHex.length != 6 && cleanHex.length != 8) return false
        return cleanHex.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
    }

    fun hsvToHex(hue: Float, saturation: Float = 0.85f, value: Float = 0.95f): String {
        val hsv = floatArrayOf(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
        val colorInt = android.graphics.Color.HSVToColor(hsv)
        return String.format("#%06X", 0xFFFFFF and colorInt)
    }
}
