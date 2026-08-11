package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.CategoryEntity
import com.example.data.db.WithdrawalEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val categories by viewModel.categories.collectAsState()
    val adminConfig by viewModel.adminConfig.collectAsState()
    val dates by viewModel.submissionDates.collectAsState()
    val allSubmissions by viewModel.allSubmissions.collectAsState()
    val allWithdrawals by viewModel.allWithdrawals.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Control & Rates, 1: Withdraw Requests, 2: Export Sheet, 3: Submit Report

    // Category Creation State
    var newCatName by remember { mutableStateOf("") }
    var newCatRate by remember { mutableStateOf("") }
    var newCatHook by remember { mutableStateOf(true) }
    var newCatDesc by remember { mutableStateOf("") }

    // Config States
    var adminPassText by remember { mutableStateOf(adminConfig?.defaultPassword ?: "aponkhan21") }
    var referralBonusText by remember { mutableStateOf((adminConfig?.referralBonus ?: 10.0).toString()) }
    var perDollarRateText by remember { mutableStateOf((adminConfig?.perDollarRate ?: 120.0).toString()) }

    // Export Sheet State
    var selectedExportCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var exportCategoryExpanded by remember { mutableStateOf(false) }
    var selectedExportDate by remember { mutableStateOf("") }
    var exportDateExpanded by remember { mutableStateOf(false) }
    var exportedText by remember { mutableStateOf("") }

    // Report Submission State
    var selectedReportCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var reportCategoryExpanded by remember { mutableStateOf(false) }
    var selectedReportDate by remember { mutableStateOf("") }
    var reportDateExpanded by remember { mutableStateOf(false) }
    var successUidsInput by remember { mutableStateOf("") }
    var reportResultSummary by remember { mutableStateOf<String?>(null) }

    val isSubmitEnabled = adminConfig?.isSubmissionEnabled ?: true

    LaunchedEffect(adminConfig) {
        adminConfig?.let {
            adminPassText = it.defaultPassword
            referralBonusText = it.referralBonus.toString()
            perDollarRateText = it.perDollarRate.toString()
        }
    }

    LaunchedEffect(categories, dates) {
        if (selectedExportCategory == null && categories.isNotEmpty()) {
            selectedExportCategory = categories.first()
        }
        if (selectedReportCategory == null && categories.isNotEmpty()) {
            selectedReportCategory = categories.first()
        }
        if (selectedExportDate.isEmpty() && dates.isNotEmpty()) {
            selectedExportDate = dates.first()
        }
        if (selectedReportDate.isEmpty() && dates.isNotEmpty()) {
            selectedReportDate = dates.first()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Admin Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Admin Control Panel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "syfaff2@gmail.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Logout")
                    }
                }
            }
        }

        // Admin Tabs Row
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                modifier = Modifier.testTag("admin_tab_control")
            ) {
                Text("Control & Rates", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                modifier = Modifier.testTag("admin_tab_withdraws")
            ) {
                Text("Withdraws (${allWithdrawals.count { it.status == "PENDING" }})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                modifier = Modifier.testTag("admin_tab_export")
            ) {
                Text("Export Sheet", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(
                selected = activeTab == 3,
                onClick = { activeTab = 3 },
                modifier = Modifier.testTag("admin_tab_report")
            ) {
                Text("Submit Report", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
        }

        // TAB 0: CONTROL & RATES
        if (activeTab == 0) {
            // Global Submission ON/OFF Switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "User Submission Access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isSubmitEnabled) "বর্তমানে Users account submit করতে পারছে" else "বর্তমানে Submission বন্ধ রয়েছে",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isSubmitEnabled,
                        onCheckedChange = { viewModel.toggleSubmissionAccess(it) },
                        modifier = Modifier.testTag("submission_toggle_switch")
                    )
                }
            }

            // Global Config Settings (Password, Referral Bonus, Dollar Rate)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Global App Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = adminPassText,
                        onValueChange = { adminPassText = it },
                        label = { Text("Account Creation Password for Users") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = referralBonusText,
                            onValueChange = { referralBonusText = it },
                            label = { Text("Referral Bonus (৳)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = perDollarRateText,
                            onValueChange = { perDollarRateText = it },
                            label = { Text("Dollar Rate ($1 in ৳)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val bon = referralBonusText.toDoubleOrNull() ?: 10.0
                            val usdRate = perDollarRateText.toDoubleOrNull() ?: 120.0
                            viewModel.updateAdminConfig(
                                pass = adminPassText,
                                refBonus = bon,
                                dollarRate = usdRate
                            )
                            Toast.makeText(context, "Settings updated successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save All Settings")
                    }
                }
            }

            // Add New Category Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add New Category & Rate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name (e.g. FB Cookie Account)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newCatRate,
                        onValueChange = { newCatRate = it },
                        label = { Text("Rate in TK per Account (e.g. 35.0)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = newCatHook,
                            onCheckedChange = { newCatHook = it }
                        )
                        Column {
                            Text("Enable Cookie Hook (c_user UID Extraction)", fontWeight = FontWeight.Bold)
                            Text("Extracts c_user as UID and formats UID/Password/Cookie", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    OutlinedTextField(
                        value = newCatDesc,
                        onValueChange = { newCatDesc = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val rateVal = newCatRate.toDoubleOrNull() ?: 0.0
                            if (newCatName.isNotBlank() && rateVal > 0) {
                                viewModel.addCategory(newCatName, rateVal, newCatHook, newCatDesc)
                                newCatName = ""
                                newCatRate = ""
                                newCatDesc = ""
                                Toast.makeText(context, "Category added successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "সঠিক নাম এবং Rate প্রদান করুন", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Category", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Existing Categories List
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Active Categories & Rates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider()

                    categories.forEach { cat ->
                        var editRateText by remember(cat.rate) { mutableStateOf(cat.rate.toString()) }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cat.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if (cat.requiresCookieHook) "Hook: c_user UID Enabled" else "Hook: None",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editRateText,
                                        onValueChange = { editRateText = it },
                                        label = { Text("Rate (TK)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            val nr = editRateText.toDoubleOrNull()
                                            if (nr != null && nr > 0) {
                                                viewModel.updateCategoryRate(cat, nr)
                                                Toast.makeText(context, "Rate updated", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Text("Save Rate")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB 1: WITHDRAW REQUESTS MANAGEMENT
        if (activeTab == 1) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "User Withdraw Requests (${allWithdrawals.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (allWithdrawals.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "কোনো Withdraw Request পাওয়া যায়নি",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    allWithdrawals.forEach { item ->
                        AdminWithdrawalCard(
                            item = item,
                            onApprove = {
                                viewModel.processWithdrawalStatus(item.id, "APPROVED") { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onReject = {
                                viewModel.processWithdrawalStatus(item.id, "REJECTED") { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }

        // TAB 2: EXPORT SHEET (Sequential User Grouping)
        if (activeTab == 2) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Export Submitted Accounts Sheet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "* প্রতিটি User এর submitted accounts পর পর সাজানো হয়ে আসবে (Grouped by User)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Select Category
                    ExposedDropdownMenuBox(
                        expanded = exportCategoryExpanded,
                        onExpandedChange = { exportCategoryExpanded = !exportCategoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedExportCategory?.name ?: "Select Category",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exportCategoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = exportCategoryExpanded,
                            onDismissRequest = { exportCategoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedExportCategory = cat
                                        exportCategoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Select Date
                    ExposedDropdownMenuBox(
                        expanded = exportDateExpanded,
                        onExpandedChange = { exportDateExpanded = !exportDateExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedExportDate.ifEmpty { "Select Date" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exportDateExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = exportDateExpanded,
                            onDismissRequest = { exportDateExpanded = false }
                        ) {
                            if (dates.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No Submissions Date") },
                                    onClick = { exportDateExpanded = false }
                                )
                            } else {
                                dates.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d) },
                                        onClick = {
                                            selectedExportDate = d
                                            exportDateExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val cat = selectedExportCategory
                            if (cat != null && selectedExportDate.isNotBlank()) {
                                coroutineScope.launch {
                                    exportedText = viewModel.getFormattedExportText(cat.id, selectedExportDate)
                                    if (exportedText.isBlank()) {
                                        Toast.makeText(context, "এই তারিখে কোনো submission পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.FindInPage, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Export Sheet", fontWeight = FontWeight.Bold)
                    }

                    if (exportedText.isNotBlank()) {
                        OutlinedTextField(
                            value = exportedText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Formatted Output (UID/Password/Cookie)") },
                            minLines = 8,
                            maxLines = 16,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(exportedText))
                                    Toast.makeText(context, "Export text copied to Clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Sheet", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    try {
                                        val cleanCatName = selectedExportCategory?.name?.replace(" ", "_") ?: "Export"
                                        val cleanDate = selectedExportDate.replace("-", "_")
                                        val fileName = "Sheet_${cleanCatName}_$cleanDate.xlsx"
                                        val file = java.io.File(context.cacheDir, fileName)

                                        // Formatted Excel content
                                        val excelContent = exportedText.lines().joinToString("\n") { line ->
                                            if (line.contains("/")) {
                                                line.replace("/", "\t")
                                            } else line
                                        }

                                        file.writeText(excelContent)

                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )

                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, fileName)
                                            putExtra(android.content.Intent.EXTRA_TEXT, excelContent)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Download / Save Excel Sheet (.xlsx)"))
                                        Toast.makeText(context, "Downloading Excel Sheet: $fileName", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_TEXT, exportedText)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(fallbackIntent, "Download / Share Sheet"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download Sheet", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // TAB 3: SUBMIT REPORT (UID Matching)
        if (activeTab == 3) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Submit Success Report (UID Matcher)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "এখানে Success UIDs পেস্ট করুন। যেগুলোর UID ম্যাচ করবে সেগুলো Green (Success) হবে এবং User এর balance এ টাকা যোগ হবে। বাকিগুলো Red (Rejected) হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Select Category
                    ExposedDropdownMenuBox(
                        expanded = reportCategoryExpanded,
                        onExpandedChange = { reportCategoryExpanded = !reportCategoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedReportCategory?.name ?: "Select Category",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reportCategoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = reportCategoryExpanded,
                            onDismissRequest = { reportCategoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedReportCategory = cat
                                        reportCategoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Select Date
                    ExposedDropdownMenuBox(
                        expanded = reportDateExpanded,
                        onExpandedChange = { reportDateExpanded = !reportDateExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedReportDate.ifEmpty { "Select Date" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Submission File Date") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reportDateExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = reportDateExpanded,
                            onDismissRequest = { reportDateExpanded = false }
                        ) {
                            dates.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d) },
                                    onClick = {
                                        selectedReportDate = d
                                        reportDateExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = successUidsInput,
                        onValueChange = { successUidsInput = it },
                        label = { Text("Paste Success UIDs List (1 per line)") },
                        placeholder = { Text("e.g. 61593203065886\n61593203065887\n61593203065888") },
                        minLines = 8,
                        maxLines = 16,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val cat = selectedReportCategory
                            if (cat == null || selectedReportDate.isBlank()) {
                                Toast.makeText(context, "Category এবং Date সিলেক্ট করুন", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (successUidsInput.isBlank()) {
                                Toast.makeText(context, "Success UIDs পেস্ট করুন", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.processAdminReport(cat.id, selectedReportDate, successUidsInput) { succ, rej ->
                                reportResultSummary = "Report Completed: $succ Success (Green), $rej Rejected (Red)"
                                successUidsInput = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Process & Match Report", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    reportResultSummary?.let { summary ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminWithdrawalCard(
    item: WithdrawalEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val methodColor = if (item.method == "Bkash") BkashPink else BinanceYellow
    val (statusColor, containerColor) = when (item.status) {
        "APPROVED" -> Pair(SuccessGreen, SuccessGreenContainer)
        "REJECTED" -> Pair(RejectedRed, RejectedRedContainer)
        else -> Pair(PendingYellow, PendingYellowContainer)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Badge(containerColor = methodColor.copy(alpha = 0.2f)) {
                        Text(
                            text = item.method,
                            color = methodColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "User ID: ${item.userId}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = containerColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.status,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "A/C: ${item.accountDetails}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Date: ${item.dateString}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (item.method == "Binance") "$${"%.2f".format(item.amountUsd)}\n(৳${"%.0f".format(item.amountTk)})" else "৳${"%.2f".format(item.amountTk)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (item.status == "PENDING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve")
                    }

                    Button(
                        onClick = onReject,
                        colors = ButtonDefaults.buttonColors(containerColor = RejectedRed),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject & Refund")
                    }
                }
            }
        }
    }
}
