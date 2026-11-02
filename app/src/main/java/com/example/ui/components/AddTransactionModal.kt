package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.CurrencyInfo
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeEmerald
import com.example.ui.util.CategoryIconHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionModal(
    initialTransaction: TransactionEntity? = null,
    categories: List<CategoryEntity>,
    currency: CurrencyInfo,
    onDismiss: () -> Unit,
    onSave: (id: Long, title: String, amount: Double, type: TransactionType, category: CategoryEntity, timestamp: Long, note: String, isRecurring: Boolean, recurringEndDate: Long?, recurringFrequency: String, createdAt: Long?) -> Unit,
    onDelete: ((TransactionEntity) -> Unit)? = null
) {
    var type by remember { mutableStateOf(initialTransaction?.type ?: TransactionType.EXPENSE) }
    var title by remember { mutableStateOf(initialTransaction?.title ?: "") }
    var amountText by remember { mutableStateOf(initialTransaction?.let { String.format("%.2f", it.amount) } ?: "") }
    var note by remember { mutableStateOf(initialTransaction?.note ?: "") }
    var selectedTimestamp by remember { mutableLongStateOf(initialTransaction?.timestamp ?: System.currentTimeMillis()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    
    // Recurring state
    var isRecurring by remember { mutableStateOf(initialTransaction?.isRecurring ?: false) }
    var recurringFrequency by remember { mutableStateOf(initialTransaction?.recurringFrequency ?: "Monthly") }
    var hasEndDate by remember { mutableStateOf(initialTransaction?.recurringEndDate != null) }
    var recurringEndDate by remember { mutableStateOf<Long?>(initialTransaction?.recurringEndDate) }

    val filteredCategories = categories.filter { it.type == type }
    var selectedCategory by remember(type) {
        mutableStateOf(
            initialTransaction?.let { tx -> categories.find { it.id == tx.categoryId } }
                ?: filteredCategories.firstOrNull()
        )
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedTimestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance()
                            val pickedCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            cal.set(
                                pickedCal.get(Calendar.YEAR),
                                pickedCal.get(Calendar.MONTH),
                                pickedCal.get(Calendar.DAY_OF_MONTH),
                                Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                                Calendar.getInstance().get(Calendar.MINUTE)
                            )
                            selectedTimestamp = cal.timeInMillis
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Select", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                    text = if (initialTransaction == null) "New Transaction" else "Edit Transaction",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Income / Expense Segmented Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
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
                        .clickable {
                            type = TransactionType.EXPENSE
                            selectedCategory = categories.firstOrNull { it.type == TransactionType.EXPENSE }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Expense",
                        color = if (type == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (type == TransactionType.INCOME) IncomeEmerald else Color.Transparent)
                        .clickable {
                            type = TransactionType.INCOME
                            selectedCategory = categories.firstOrNull { it.type == TransactionType.INCOME }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Income",
                        color = if (type == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Field
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (${currency.symbol})") },
                prefix = { Text("${currency.symbol} ", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Description") },
                placeholder = { Text("e.g. Groceries at Whole Foods") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date Picker Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDatePickerDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Transaction Date",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val isToday = isSameDay(selectedTimestamp, System.currentTimeMillis())
                            Text(
                                text = if (isToday) "Today (${dateFormat.format(Date(selectedTimestamp))})" else dateFormat.format(Date(selectedTimestamp)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isToday = isSameDay(selectedTimestamp, System.currentTimeMillis())
                        if (!isToday) {
                            TextButton(
                                onClick = { selectedTimestamp = System.currentTimeMillis() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Set Today", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(
                                onClick = { showDatePickerDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Change", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Picker Header
            Text(
                text = "Select Category",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCategories) { cat ->
                    val isSelected = selectedCategory?.id == cat.id
                    val catColor = CategoryIconHelper.parseColor(cat.colorHex)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) catColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) catColor else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(catColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                contentDescription = cat.name,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cat.name,
                            fontSize = 10.sp,
                            maxLines = 1,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note Optional
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Recurring / Repeat Toggle Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Is it repeat?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Set as automated recurring payment",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it }
                        )
                    }

                    if (isRecurring) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Frequency pills
                        Text(
                            text = "Frequency",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Monthly", "Weekly", "Yearly").forEach { freq ->
                                val isSelected = recurringFrequency == freq
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { recurringFrequency = freq },
                                    label = { Text(freq) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Repeat End Date option
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (hasEndDate && recurringEndDate != null) 
                                    "Ends on: ${dateFormat.format(Date(recurringEndDate!!))}" 
                                else 
                                    "Repeat Indefinitely (No End Date)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(
                                onClick = {
                                    if (hasEndDate) {
                                        hasEndDate = false
                                        recurringEndDate = null
                                    } else {
                                        hasEndDate = true
                                        // Default to 1 year from now
                                        val cal = Calendar.getInstance()
                                        cal.add(Calendar.YEAR, 1)
                                        recurringEndDate = cal.timeInMillis
                                    }
                                }
                            ) {
                                Text(if (hasEndDate) "Clear End Date" else "Set End Date")
                            }
                        }

                        if (initialTransaction?.isRecurring == true) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { isRecurring = false },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseCrimson)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PauseCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop Repeating This Expense", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (initialTransaction != null && onDelete != null) {
                    IconButton(
                        onClick = {
                            onDelete(initialTransaction)
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
                        val amountVal = amountText.toDoubleOrNull() ?: 0.0
                        val cat = selectedCategory
                        if (title.isNotBlank() && amountVal > 0 && cat != null) {
                            onSave(
                                initialTransaction?.id ?: 0L,
                                title,
                                amountVal,
                                type,
                                cat,
                                selectedTimestamp,
                                note,
                                isRecurring,
                                if (isRecurring && hasEndDate) recurringEndDate else null,
                                recurringFrequency,
                                initialTransaction?.createdAt
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == TransactionType.EXPENSE) ExpenseCrimson else IncomeEmerald
                    )
                ) {
                    Text(
                        text = if (initialTransaction == null) "Add Transaction" else "Save Changes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

