package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.model.CurrencyInfo
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeEmerald
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class ParsedTransactionItem(
    var id: String = UUID.randomUUID().toString(),
    var title: String,
    var amount: Double,
    var type: TransactionType,
    var categoryName: String,
    var timestamp: Long = System.currentTimeMillis(),
    var note: String = "",
    var isSelected: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExpenseSheetModal(
    categories: List<CategoryEntity>,
    currency: CurrencyInfo,
    onDismiss: () -> Unit,
    onImportConfirmed: (List<ParsedTransactionItem>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var rawText by remember { mutableStateOf("") }
    var driveUrl by remember { mutableStateOf("") }
    var isFetchingDrive by remember { mutableStateOf(false) }
    var inputMode by remember { mutableIntStateOf(0) } // 0 = File/Text, 1 = Google Drive Link
    var parsedItems by remember { mutableStateOf<List<ParsedTransactionItem>>(emptyList()) }
    var step by remember { mutableIntStateOf(1) } // 1 = Input, 2 = Preview
    var parseError by remember { mutableStateOf<String?>(null) }
    var importedFileName by remember { mutableStateOf<String?>(null) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // File picker launcher for CSV, Excel CSV exports, TSV, TXT files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().use { it.readText() }
                    if (content.isNotBlank()) {
                        rawText = content
                        importedFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_file.csv"
                        parseError = null
                        val items = parseInputText(content, categories)
                        if (items.isNotEmpty()) {
                            parsedItems = items
                            step = 2
                        } else {
                            parseError = "No valid transaction rows found in selected file."
                        }
                    } else {
                        parseError = "The selected file is empty."
                    }
                }
            } catch (e: Exception) {
                parseError = "Error reading file: ${e.localizedMessage}"
            }
        }
    }

    val sampleTemplate = """Date, Title, Amount, Category, Type
2026-07-01, House Rent Payment, 1250.00, Housing & Rent, Expense
2026-07-03, Monthly Salary, 4500.00, Salary, Income
2026-07-05, Supermarket Grocery, 185.50, Groceries, Expense
2026-07-10, Electricity & Water, 142.00, Utilities, Expense
2026-07-12, Restaurant Dinner, 78.00, Dining & Food, Expense
2026-07-15, Freelance UI Project, 850.00, Freelance, Income
2026-07-18, Fuel Station, 65.00, Transport, Expense"""

    val sampleDriveLink = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit?usp=sharing"

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
            // Modal Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(IncomeEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Import Excel / CSV",
                            tint = IncomeEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (step == 1) "Import Expense Sheet" else "Review & Confirm Import",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (step == 1) "Import via Google Drive link, file, or text" else "${parsedItems.count { it.isSelected }} of ${parsedItems.size} items selected",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (step == 1) {
                // Input Mode Segmented Control (Tabs)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = inputMode == 0,
                        onClick = {
                            inputMode = 0
                            parseError = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("File / Text", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    SegmentedButton(
                        selected = inputMode == 1,
                        onClick = {
                            inputMode = 1
                            parseError = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Drive Link", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (inputMode == 1) {
                    // MODE 1: GOOGLE DRIVE LINK IMPORT
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = "Google Drive",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Google Drive / Sheets Share Link",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = driveUrl,
                                onValueChange = {
                                    driveUrl = it
                                    parseError = null
                                },
                                placeholder = {
                                    Text("Paste Google Sheets or Drive link (e.g. https://docs.google.com/spreadsheets/d/...)")
                                },
                                singleLine = false,
                                maxLines = 3,
                                trailingIcon = {
                                    if (driveUrl.isNotEmpty()) {
                                        IconButton(onClick = { driveUrl = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Help guide
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Ensure link sharing is set to 'Anyone with the link can view' in Google Sheets.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    if (parseError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = parseError!!,
                            color = ExpenseCrimson,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                driveUrl = sampleDriveLink
                                parseError = null
                            }
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Try Sample Link")
                        }

                        Button(
                            onClick = {
                                if (driveUrl.isBlank()) {
                                    parseError = "Please paste a Google Drive or Google Sheets link first."
                                    return@Button
                                }
                                val fileId = extractGoogleDriveFileId(driveUrl)
                                if (fileId == null) {
                                    parseError = "Invalid Google Drive link format. Please check the URL."
                                    return@Button
                                }

                                isFetchingDrive = true
                                parseError = null

                                coroutineScope.launch {
                                    try {
                                        val fetchedContent = fetchFromGoogleDrive(fileId, driveUrl)
                                        val items = parseInputText(fetchedContent, categories)
                                        if (items.isNotEmpty()) {
                                            parsedItems = items
                                            importedFileName = "Google Drive Spreadsheet"
                                            step = 2
                                        } else {
                                            parseError = "Downloaded Google Sheet contained no readable transactions. Check columns."
                                        }
                                    } catch (e: Exception) {
                                        // Fallback attempt: if public fetching failed, check if user pasted raw text in rawText or use sample fallback
                                        parseError = e.localizedMessage ?: "Could not fetch Google Drive file. Ensure link is public."
                                    } finally {
                                        isFetchingDrive = false
                                    }
                                }
                            },
                            enabled = !isFetchingDrive,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IncomeEmerald)
                        ) {
                            if (isFetchingDrive) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fetching...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fetch & Import", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // MODE 0: FILE PICKER OR PASTE TEXT
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { filePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = IncomeEmerald.copy(alpha = 0.12f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(IncomeEmerald.copy(alpha = 0.4f)))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(IncomeEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = "Choose File",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Choose Excel / CSV File",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = importedFileName ?: "Supports .csv, .tsv, .txt, or exported sheets",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = IncomeEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = " OR PASTE SHEET TEXT ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = rawText,
                        onValueChange = {
                            rawText = it
                            importedFileName = null
                            parseError = null
                        },
                        placeholder = {
                            Text("Paste rows copied from Excel, Google Sheets, or CSV:\n\n2026-07-01, House Rent, 1200.00, Housing & Rent\n2026-07-05, Supermarket, 150.50, Groceries\n2026-07-10, Salary, 4500.00, Salary, Income")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (parseError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = parseError!!,
                            color = ExpenseCrimson,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                rawText = sampleTemplate
                                importedFileName = "sample_sheet.csv"
                                parseError = null
                            }
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fill Sample Template")
                        }

                        Button(
                            onClick = {
                                if (rawText.isBlank()) {
                                    parseError = "Please select a file or paste expense sheet text"
                                    return@Button
                                }
                                val items = parseInputText(rawText, categories)
                                if (items.isEmpty()) {
                                    parseError = "Could not parse valid transactions. Check date or amount formats."
                                } else {
                                    parsedItems = items
                                    step = 2
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IncomeEmerald)
                        ) {
                            Text("Parse & Preview Data", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else {
                // Step 2: Preview checklist with summary stats
                val selectedItems = parsedItems.filter { it.isSelected }
                val totalIncome = selectedItems.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val totalExpense = selectedItems.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val netBalance = totalIncome - totalExpense

                // Summary Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Income", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("+${currency.symbol}${String.format("%.2f", totalIncome)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IncomeEmerald)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Expense", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("-${currency.symbol}${String.format("%.2f", totalExpense)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ExpenseCrimson)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Net Impact", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                (if (netBalance >= 0) "+" else "") + "${currency.symbol}${String.format("%.2f", netBalance)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (netBalance >= 0) IncomeEmerald else ExpenseCrimson
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { step = 1 }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Change Source File / Text")
                    }

                    TextButton(onClick = {
                        val allSel = parsedItems.all { it.isSelected }
                        parsedItems = parsedItems.map { it.copy(isSelected = !allSel) }
                    }) {
                        Text(if (parsedItems.all { it.isSelected }) "Deselect All" else "Select All")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    parsedItems.forEachIndexed { idx, item ->
                        val isExpense = item.type == TransactionType.EXPENSE
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isSelected,
                                    onCheckedChange = { checked ->
                                        parsedItems = parsedItems.toMutableList().also {
                                            it[idx] = it[idx].copy(isSelected = checked)
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = if (isExpense) ExpenseCrimson.copy(alpha = 0.15f) else IncomeEmerald.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = item.categoryName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isExpense) ExpenseCrimson else IncomeEmerald,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = dateFormat.format(Date(item.timestamp)),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = (if (isExpense) "-" else "+") + "${currency.symbol}${String.format("%.2f", item.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isExpense) ExpenseCrimson else IncomeEmerald
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val selected = parsedItems.filter { it.isSelected }
                        if (selected.isNotEmpty()) {
                            onImportConfirmed(selected)
                            onDismiss()
                        }
                    },
                    enabled = parsedItems.any { it.isSelected },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeEmerald)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Import ${selectedItems.size} Transactions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * Intelligent CSV / Sheet / Text Parser
 */
private fun parseInputText(text: String, categories: List<CategoryEntity>): List<ParsedTransactionItem> {
    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
    val result = mutableListOf<ParsedTransactionItem>()

    if (lines.isEmpty()) return emptyList()

    var dateColIdx = -1
    var titleColIdx = -1
    var amountColIdx = -1
    var categoryColIdx = -1
    var typeColIdx = -1
    var hasHeader = false

    // Check line 0 for headers
    val firstLine = lines[0]
    val firstLineTokens = splitLineTokens(firstLine)

    if (firstLineTokens.size >= 2) {
        firstLineTokens.forEachIndexed { idx, token ->
            val lower = token.lowercase().replace("\"", "").trim()
            if (lower.contains("date") || lower.contains("time") || lower.contains("day")) dateColIdx = idx
            if (lower.contains("title") || lower.contains("desc") || lower.contains("name") || lower.contains("item") || lower.contains("payee")) titleColIdx = idx
            if (lower.contains("amount") || lower.contains("cost") || lower.contains("price") || lower.contains("value") || lower.contains("sum") || lower.contains("total")) amountColIdx = idx
            if (lower.contains("category") || lower.contains("cat")) categoryColIdx = idx
            if (lower.contains("type") || lower.contains("flow") || lower.contains("mode")) typeColIdx = idx
        }
        if (amountColIdx != -1 || titleColIdx != -1 || dateColIdx != -1) {
            hasHeader = true
        }
    }

    val startIndex = if (hasHeader) 1 else 0

    val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()),
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    )

    for (i in startIndex until lines.size) {
        val line = lines[i]
        val tokens = splitLineTokens(line)

        var title = ""
        var amount = 0.0
        var type = TransactionType.EXPENSE
        var categoryName = "General"
        var timestamp = System.currentTimeMillis()
        var note = "Imported from Expense Sheet"

        if (hasHeader && tokens.size > 1) {
            // Header-mapped parsing
            if (dateColIdx in tokens.indices) {
                val parsedTs = parseDateToken(tokens[dateColIdx], dateFormats)
                if (parsedTs != null) timestamp = parsedTs
            }
            if (titleColIdx in tokens.indices) {
                title = tokens[titleColIdx]
            }
            if (amountColIdx in tokens.indices) {
                val (parsedAmount, parsedType) = parseAmountToken(tokens[amountColIdx])
                amount = parsedAmount
                if (parsedType != null) type = parsedType
            }
            if (categoryColIdx in tokens.indices && tokens[categoryColIdx].isNotBlank()) {
                val catStr = tokens[categoryColIdx]
                val matched = categories.find { it.name.equals(catStr, ignoreCase = true) }
                if (matched != null) {
                    categoryName = matched.name
                    type = matched.type
                } else {
                    categoryName = catStr
                }
            }
            if (typeColIdx in tokens.indices) {
                val tStr = tokens[typeColIdx].lowercase()
                if (tStr.contains("income") || tStr.contains("credit") || tStr.contains("deposit")) {
                    type = TransactionType.INCOME
                } else if (tStr.contains("expense") || tStr.contains("debit") || tStr.contains("payment")) {
                    type = TransactionType.EXPENSE
                }
            }
        }

        // Fallback / token-search if not fully parsed
        if (amount == 0.0 || title.isBlank()) {
            for (token in tokens) {
                val clean = token.trim()

                // Date check
                if (timestamp == System.currentTimeMillis()) {
                    val parsedTs = parseDateToken(clean, dateFormats)
                    if (parsedTs != null) {
                        timestamp = parsedTs
                        continue
                    }
                }

                // Amount check
                if (amount == 0.0) {
                    val (parsedAmount, parsedType) = parseAmountToken(clean)
                    if (parsedAmount > 0.0) {
                        amount = parsedAmount
                        if (parsedType != null) type = parsedType
                        continue
                    }
                }

                // Type check
                if (clean.equals("income", ignoreCase = true) || clean.equals("credit", ignoreCase = true)) {
                    type = TransactionType.INCOME
                    continue
                } else if (clean.equals("expense", ignoreCase = true) || clean.equals("debit", ignoreCase = true)) {
                    type = TransactionType.EXPENSE
                    continue
                }

                // Category check
                val matchedCat = categories.find { it.name.equals(clean, ignoreCase = true) }
                if (matchedCat != null) {
                    categoryName = matchedCat.name
                    type = matchedCat.type
                    continue
                }

                // Title assignment
                if (title.isBlank() && clean.length > 1) {
                    title = clean
                } else if (note == "Imported from Expense Sheet") {
                    note = clean
                }
            }
        }

        // Single string unstructured fallback
        if (amount == 0.0) {
            val numRegex = Regex("([0-9]+(?:\\.[0-9]{1,2})?)")
            val match = numRegex.find(line)
            if (match != null) {
                amount = match.value.toDoubleOrNull() ?: 0.0
                if (title.isBlank()) {
                    title = line.replace(match.value, "").replace("$", "").replace("₹", "").replace("€", "").replace("£", "").trim()
                }
            }
        }

        if (title.isBlank()) title = "Expense Item"

        // Smart Category inference
        if (categoryName == "General" || categoryName.isBlank()) {
            val lowerTitle = title.lowercase()
            categoryName = when {
                lowerTitle.contains("salary") || lowerTitle.contains("payday") || lowerTitle.contains("wage") -> {
                    type = TransactionType.INCOME
                    "Salary"
                }
                lowerTitle.contains("freelance") || lowerTitle.contains("client") || lowerTitle.contains("project") -> {
                    type = TransactionType.INCOME
                    "Freelance"
                }
                lowerTitle.contains("rent") || lowerTitle.contains("house") || lowerTitle.contains("apartment") -> "Housing & Rent"
                lowerTitle.contains("grocery") || lowerTitle.contains("market") || lowerTitle.contains("supermarket") || lowerTitle.contains("store") -> "Groceries"
                lowerTitle.contains("dinner") || lowerTitle.contains("lunch") || lowerTitle.contains("restaurant") || lowerTitle.contains("cafe") || lowerTitle.contains("coffee") -> "Dining & Food"
                lowerTitle.contains("fuel") || lowerTitle.contains("gas") || lowerTitle.contains("uber") || lowerTitle.contains("taxi") || lowerTitle.contains("bus") -> "Transport"
                lowerTitle.contains("electric") || lowerTitle.contains("water") || lowerTitle.contains("power") || lowerTitle.contains("internet") || lowerTitle.contains("wifi") || lowerTitle.contains("bill") -> "Utilities"
                lowerTitle.contains("movie") || lowerTitle.contains("game") || lowerTitle.contains("netflix") || lowerTitle.contains("spotify") -> "Entertainment"
                lowerTitle.contains("pharmacy") || lowerTitle.contains("doctor") || lowerTitle.contains("hospital") || lowerTitle.contains("health") -> "Health & Medical"
                lowerTitle.contains("shop") || lowerTitle.contains("clothes") || lowerTitle.contains("amazon") -> "Shopping"
                else -> if (type == TransactionType.INCOME) "Other Income" else "General"
            }
        }

        if (amount > 0.0) {
            result.add(
                ParsedTransactionItem(
                    title = title,
                    amount = amount,
                    type = type,
                    categoryName = categoryName,
                    timestamp = timestamp,
                    note = note,
                    isSelected = true
                )
            )
        }
    }

    return result
}

private fun splitLineTokens(line: String): List<String> {
    return when {
        line.contains(",") -> line.split(",").map { it.trim().removeSurrounding("\"") }
        line.contains("\t") -> line.split("\t").map { it.trim().removeSurrounding("\"") }
        line.contains(";") -> line.split(";").map { it.trim().removeSurrounding("\"") }
        line.contains("|") -> line.split("|").map { it.trim().removeSurrounding("\"") }
        else -> listOf(line)
    }
}

private fun parseDateToken(token: String, formats: List<SimpleDateFormat>): Long? {
    val clean = token.trim()
    for (sdf in formats) {
        try {
            val date = sdf.parse(clean)
            if (date != null) return date.time
        } catch (e: Exception) {
            // continue
        }
    }
    return null
}

private fun parseAmountToken(token: String): Pair<Double, TransactionType?> {
    val clean = token.replace("$", "").replace("€", "").replace("£", "").replace("₹", "").replace(",", "").trim()
    var isNegative = false
    var typeOverride: TransactionType? = null

    var cleanedStr = clean
    if (clean.startsWith("-") || (clean.startsWith("(") && clean.endsWith(")"))) {
        isNegative = true
        typeOverride = TransactionType.EXPENSE
        cleanedStr = clean.removePrefix("-").removePrefix("(").removeSuffix(")").trim()
    } else if (clean.startsWith("+")) {
        typeOverride = TransactionType.INCOME
        cleanedStr = clean.removePrefix("+").trim()
    }

    val valDouble = cleanedStr.toDoubleOrNull() ?: 0.0
    return Pair(valDouble, if (isNegative) TransactionType.EXPENSE else typeOverride)
}

private fun extractGoogleDriveFileId(url: String): String? {
    val cleanUrl = url.trim()
    val sheetMatch = Regex("spreadsheets/d/([a-zA-Z0-9_-]+)").find(cleanUrl)
    if (sheetMatch != null) return sheetMatch.groupValues[1]

    val fileMatch = Regex("file/d/([a-zA-Z0-9_-]+)").find(cleanUrl)
    if (fileMatch != null) return fileMatch.groupValues[1]

    val idMatch = Regex("[?&]id=([a-zA-Z0-9_-]+)").find(cleanUrl)
    if (idMatch != null) return idMatch.groupValues[1]

    if (cleanUrl.length >= 20 && cleanUrl.matches(Regex("^[a-zA-Z0-9_-]+$"))) return cleanUrl

    return null
}

private suspend fun fetchFromGoogleDrive(fileId: String, originalUrl: String): String {
    return withContext(Dispatchers.IO) {
        val urlsToTry = if (originalUrl.lowercase().contains("spreadsheets")) {
            listOf(
                "https://docs.google.com/spreadsheets/d/$fileId/export?format=csv",
                "https://drive.google.com/uc?export=download&id=$fileId"
            )
        } else {
            listOf(
                "https://drive.google.com/uc?export=download&id=$fileId",
                "https://docs.google.com/spreadsheets/d/$fileId/export?format=csv"
            )
        }

        var lastEx: Exception? = null
        for (downloadUrl in urlsToTry) {
            try {
                val conn = java.net.URL(downloadUrl).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.instanceFollowRedirects = true
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; SpendWise)")

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val content = conn.inputStream.bufferedReader().use { it.readText() }
                    if (content.isNotBlank() && !content.contains("<!DOCTYPE html>") && !content.contains("<html")) {
                        return@withContext content
                    }
                }
            } catch (e: Exception) {
                lastEx = e
            }
        }
        throw lastEx ?: Exception("Could not fetch CSV from Google Drive link. Ensure link access is set to 'Anyone with the link can view'.")
    }
}

