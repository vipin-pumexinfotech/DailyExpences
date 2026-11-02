package com.example.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.R
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.CurrencyInfo
import com.example.data.model.TransactionSortMode
import com.example.data.model.TransactionType
import com.example.data.repository.UserProfile
import com.example.ui.theme.*
import com.example.ui.util.CategoryIconHelper
import java.text.SimpleDateFormat
import java.util.*

private data class DateGroupInfo(
    val dateKey: String,
    val displayDate: String,
    val dayTotalExpense: Double,
    val dayTotalIncome: Double,
    val items: List<TransactionEntity>
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile,
    currency: CurrencyInfo,
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    transactions: List<TransactionEntity>,
    recurringCount: Int = 0,
    searchQuery: String,
    selectedTypeFilter: TransactionType?,
    sortMode: TransactionSortMode = TransactionSortMode.DATE_GROUPED,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onSortModeChange: (TransactionSortMode) -> Unit = {},
    onAddTransactionClick: () -> Unit,
    onRecurringExpensesClick: () -> Unit,
    onImportExpenseSheetClick: () -> Unit = {},
    onTransactionClick: (TransactionEntity) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    var pendingEditTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showSortDropdown by remember { mutableStateOf(false) }

    val dateGroups = remember(transactions, sortMode) {
        if (sortMode != TransactionSortMode.DATE_GROUPED) {
            emptyList()
        } else {
            val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val shortDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val fullDateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())

            val todayCal = Calendar.getInstance()
            val todayKey = keyFormat.format(todayCal.time)

            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterdayKey = keyFormat.format(yesterdayCal.time)

            val map = LinkedHashMap<String, MutableList<TransactionEntity>>()
            transactions.sortedByDescending { it.timestamp }.forEach { tx ->
                val key = keyFormat.format(Date(tx.timestamp))
                map.getOrPut(key) { mutableListOf() }.add(tx)
            }

            map.map { (key, txList) ->
                val firstTime = txList.first().timestamp
                val displayDate = when (key) {
                    todayKey -> "Today • ${shortDateFormat.format(Date(firstTime))}"
                    yesterdayKey -> "Yesterday • ${shortDateFormat.format(Date(firstTime))}"
                    else -> fullDateFormat.format(Date(firstTime))
                }
                val dayExpense = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val dayIncome = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

                DateGroupInfo(
                    dateKey = key,
                    displayDate = displayDate,
                    dayTotalExpense = dayExpense,
                    dayTotalIncome = dayIncome,
                    items = txList
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (pendingEditTransaction != null) {
            val targetTx = pendingEditTransaction!!
            AlertDialog(
                onDismissRequest = { pendingEditTransaction = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text("Edit Transaction", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("Do you want to edit '${targetTx.title}'?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val txToEdit = pendingEditTransaction
                            pendingEditTransaction = null
                            if (txToEdit != null) {
                                onTransactionClick(txToEdit)
                            }
                        }
                    ) {
                        Text("Edit")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { pendingEditTransaction = null }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onProfileClick() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(IncomeEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = IncomeEmerald,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (userProfile.isLoggedIn && userProfile.name.isNotBlank()) "Welcome back 👋" else "Welcome to SpendWise 👋",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (userProfile.isLoggedIn && userProfile.name.isNotBlank()) userProfile.name else "Tap to Sign In / Sign Up",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        onClick = onProfileClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = "Notifications",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Financial Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        SlateCardBackground,
                                        Color(0xFF0F172A),
                                        IncomeEmeraldContainer.copy(alpha = 0.6f)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "MONTHLY NET BALANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = TextSecondaryDark
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${currency.symbol}${String.format("%,.2f", totalBalance)}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Income
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(IncomeEmerald.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Income",
                                            tint = IncomeEmerald,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Income", fontSize = 12.sp, color = TextSecondaryDark)
                                        Text(
                                            "+${currency.symbol}${String.format("%,.0f", totalIncome)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeEmerald
                                        )
                                    }
                                }

                                // Expense
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ExpenseCrimson.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Expense",
                                            tint = ExpenseCrimson,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Expense", fontSize = 12.sp, color = TextSecondaryDark)
                                        Text(
                                            "-${currency.symbol}${String.format("%,.0f", totalExpense)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseCrimson
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = SlateCardBorder.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Pills Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentIndigo.copy(alpha = 0.18f))
                                    .clickable { onRecurringExpensesClick() }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Autorenew,
                                        contentDescription = "Recurring",
                                        tint = AccentIndigo,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Recurring Subscriptions & Bills",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }

                                Surface(
                                    color = AccentIndigo,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = "$recurringCount Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Banner Illustration
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_finance_1785235367582),
                        contentDescription = "SpendWise Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            }

            // Search Bar & Filter Chips
            item {
                Spacer(modifier = Modifier.height(20.dp))

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search transactions...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTypeFilter == null,
                                onClick = { onTypeFilterChange(null) },
                                label = { Text("All") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedTypeFilter == TransactionType.INCOME,
                                onClick = { onTypeFilterChange(TransactionType.INCOME) },
                                label = { Text("Income (+)") },
                                leadingIcon = {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = IncomeEmerald, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedTypeFilter == TransactionType.EXPENSE,
                                onClick = { onTypeFilterChange(TransactionType.EXPENSE) },
                                label = { Text("Expense (-)") },
                                leadingIcon = {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ExpenseCrimson, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }

            // Recent Transactions Section Header & Sort Switcher
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Recent Transactions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${transactions.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Switch List Order Button & Dropdown Menu
                    Box {
                        OutlinedButton(
                            onClick = { showSortDropdown = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Switch List Order",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sortMode.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortDropdown,
                            onDismissRequest = { showSortDropdown = false }
                        ) {
                            TransactionSortMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = mode.label,
                                                fontWeight = if (mode == sortMode) FontWeight.Bold else FontWeight.Normal,
                                                color = if (mode == sortMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = mode.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        val icon = when (mode) {
                                            TransactionSortMode.DATE_GROUPED -> Icons.Default.CalendarToday
                                            TransactionSortMode.NEWEST_FIRST -> Icons.Default.ArrowDownward
                                            TransactionSortMode.OLDEST_FIRST -> Icons.Default.ArrowUpward
                                            TransactionSortMode.AMOUNT_HIGH_LOW -> Icons.Default.TrendingDown
                                            TransactionSortMode.AMOUNT_LOW_HIGH -> Icons.Default.TrendingUp
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (mode == sortMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        onSortModeChange(mode)
                                        showSortDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Horizontal Sort Mode Chip Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TransactionSortMode.values()) { mode ->
                        FilterChip(
                            selected = mode == sortMode,
                            onClick = { onSortModeChange(mode) },
                            label = {
                                Text(
                                    text = mode.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (mode == sortMode) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                val icon = when (mode) {
                                    TransactionSortMode.DATE_GROUPED -> Icons.Default.CalendarToday
                                    TransactionSortMode.NEWEST_FIRST -> Icons.Default.ArrowDownward
                                    TransactionSortMode.OLDEST_FIRST -> Icons.Default.ArrowUpward
                                    TransactionSortMode.AMOUNT_HIGH_LOW -> Icons.Default.TrendingDown
                                    TransactionSortMode.AMOUNT_LOW_HIGH -> Icons.Default.TrendingUp
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Empty State Handling
            if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Empty",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No transactions found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the + button below to add your first transaction",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else if (sortMode == TransactionSortMode.DATE_GROUPED) {
                // Grouped by Date List with Daily Expense Summary
                dateGroups.forEach { group ->
                    item(key = "group_header_${group.dateKey}") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = group.displayDate,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    color = ExpenseCrimson.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Day Expenses: ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "-${currency.symbol}${String.format("%,.2f", group.dayTotalExpense)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ExpenseCrimson
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(group.items, key = { "group_${group.dateKey}_tx_${it.id}" }) { tx ->
                        TransactionCardItem(
                            tx = tx,
                            currency = currency,
                            dateFormat = dateFormat,
                            context = context,
                            onDoubleClick = { pendingEditTransaction = tx }
                        )
                    }
                }
            } else {
                // Flat List based on selected sort order
                items(transactions, key = { "flat_tx_${it.id}" }) { tx ->
                    TransactionCardItem(
                        tx = tx,
                        currency = currency,
                        dateFormat = dateFormat,
                        context = context,
                        onDoubleClick = { pendingEditTransaction = tx }
                    )
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddTransactionClick,
            containerColor = IncomeEmerald,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(28.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionCardItem(
    tx: TransactionEntity,
    currency: CurrencyInfo,
    dateFormat: SimpleDateFormat,
    context: android.content.Context,
    onDoubleClick: () -> Unit
) {
    val catColor = CategoryIconHelper.parseColor(tx.categoryColorHex)
    val iconVector = CategoryIconHelper.getIcon(tx.categoryIconName)
    val isIncome = tx.type == TransactionType.INCOME

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = {
                    Toast.makeText(context, "Double tap transaction to edit", Toast.LENGTH_SHORT).show()
                },
                onDoubleClick = onDoubleClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = tx.categoryName,
                        tint = catColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = tx.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${tx.categoryName} • ${dateFormat.format(Date(tx.timestamp))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${if (isIncome) "+" else "-"}${currency.symbol}${String.format("%,.2f", tx.amount)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) IncomeEmerald else ExpenseCrimson
            )
        }
    }
}
