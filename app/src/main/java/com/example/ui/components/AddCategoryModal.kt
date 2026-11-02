package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeEmerald
import com.example.ui.util.CategoryIconHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryModal(
    initialCategory: CategoryEntity? = null,
    defaultType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, type: TransactionType, iconName: String, colorHex: String) -> Unit,
    onDelete: ((CategoryEntity) -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var type by remember { mutableStateOf(initialCategory?.type ?: defaultType) }
    var selectedIconName by remember { mutableStateOf(initialCategory?.iconName ?: CategoryIconHelper.AVAILABLE_ICONS.first().first) }
    var selectedColorHex by remember { mutableStateOf(initialCategory?.colorHex ?: CategoryIconHelper.PRESET_COLORS.first()) }
    var showCustomColorPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialCategory == null) "New Category" else "Edit Category",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Type Segment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (type == TransactionType.EXPENSE) ExpenseCrimson else Color.Transparent)
                        .clickable { type = TransactionType.EXPENSE },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Expense", color = if (type == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (type == TransactionType.INCOME) IncomeEmerald else Color.Transparent)
                        .clickable { type = TransactionType.INCOME },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Income", color = if (type == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category Name") },
                placeholder = { Text("e.g. Subscriptions") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Color Selector Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Color",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { showCustomColorPicker = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Custom Color Picker",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Picker (${selectedColorHex.uppercase()})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CategoryIconHelper.PRESET_COLORS) { hex ->
                    val color = CategoryIconHelper.parseColor(hex)
                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                item {
                    val isCustomSelected = CategoryIconHelper.PRESET_COLORS.none { it.equals(selectedColorHex, ignoreCase = true) }
                    val customColor = CategoryIconHelper.parseColor(selectedColorHex)

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isCustomSelected) customColor else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isCustomSelected) 3.dp else 1.dp,
                                color = if (isCustomSelected) Color.White else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { showCustomColorPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Custom Color",
                            tint = if (isCustomSelected) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Icon Grid Selector
            Text("Select Icon", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CategoryIconHelper.AVAILABLE_ICONS) { (iconName, imageVector) ->
                    val isSelected = selectedIconName == iconName
                    val selectedColor = CategoryIconHelper.parseColor(selectedColorHex)

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) selectedColor else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedIconName = iconName },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = imageVector,
                            contentDescription = iconName,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (initialCategory != null && onDelete != null && !initialCategory.isDefault) {
                    IconButton(
                        onClick = {
                            onDelete(initialCategory)
                            onDismiss()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = ExpenseCrimson.copy(alpha = 0.15f),
                            contentColor = ExpenseCrimson
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(
                                initialCategory?.id ?: 0L,
                                name,
                                type,
                                selectedIconName,
                                selectedColorHex
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeEmerald)
                ) {
                    Text("Save Category", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // Custom Color Picker Dialog
    if (showCustomColorPicker) {
        CustomColorPickerDialog(
            initialColorHex = selectedColorHex,
            selectedIconName = selectedIconName,
            onDismiss = { showCustomColorPicker = false },
            onColorSelected = { newHex ->
                selectedColorHex = newHex
            }
        )
    }
}

@Composable
fun CustomColorPickerDialog(
    initialColorHex: String,
    selectedIconName: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    var hue by remember { mutableFloatStateOf(160f) }
    var saturation by remember { mutableFloatStateOf(0.85f) }
    var value by remember { mutableFloatStateOf(0.95f) }
    var hexInput by remember { mutableStateOf(initialColorHex) }

    LaunchedEffect(initialColorHex) {
        if (CategoryIconHelper.isValidHex(initialColorHex)) {
            try {
                val cleanHex = initialColorHex.removePrefix("#").trim()
                val colorInt = cleanHex.toLong(16).toInt()
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(colorInt or 0xFF000000.toInt(), hsv)
                hue = hsv[0]
                saturation = if (hsv[1] > 0f) hsv[1] else 0.85f
                value = if (hsv[2] > 0f) hsv[2] else 0.95f
            } catch (e: Exception) {
                // keep defaults
            }
        }
    }

    val currentPickedHex = CategoryIconHelper.hsvToHex(hue, saturation, value)
    val activeHex = if (CategoryIconHelper.isValidHex(hexInput)) hexInput else currentPickedHex
    val currentPickedColor = CategoryIconHelper.parseColor(activeHex)
    val categoryIcon = CategoryIconHelper.getIcon(selectedIconName)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Category Color Picker", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Live Icon Preview Box with Picked Color
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(currentPickedColor)
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = "Category Icon",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = activeHex.uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hue Spectrum Slider
                Text("Color Hue Spectrum", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        hexInput = CategoryIconHelper.hsvToHex(hue, saturation, value)
                    },
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Saturation / Intensity Slider
                Text("Color Intensity", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                Slider(
                    value = saturation,
                    onValueChange = {
                        saturation = it
                        hexInput = CategoryIconHelper.hsvToHex(hue, saturation, value)
                    },
                    valueRange = 0.2f..1f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // HEX Text Input
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input
                        if (CategoryIconHelper.isValidHex(input)) {
                            try {
                                val cleanHex = input.removePrefix("#").trim()
                                val colorInt = cleanHex.toLong(16).toInt()
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(colorInt or 0xFF000000.toInt(), hsv)
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                            } catch (e: Exception) {}
                        }
                    },
                    label = { Text("Hex Color Code") },
                    placeholder = { Text("#10B981") },
                    singleLine = true,
                    isError = hexInput.isNotBlank() && !CategoryIconHelper.isValidHex(hexInput),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalHex = if (CategoryIconHelper.isValidHex(hexInput)) hexInput else currentPickedHex
                    onColorSelected(finalHex)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = IncomeEmerald)
            ) {
                Text("Apply Color", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

