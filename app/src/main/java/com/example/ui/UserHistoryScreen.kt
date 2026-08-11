package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.db.SubmissionEntity
import com.example.data.db.WithdrawalEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHistoryScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var mainTab by remember { mutableIntStateOf(0) } // 0: Submissions, 1: Withdrawals

    val submissions by viewModel.userSubmissions.collectAsState()
    val withdrawals by viewModel.userWithdrawals.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, SUCCESS/APPROVED, REJECTED

    val filteredSubmissions = remember(submissions, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> submissions.filter { it.status == "PENDING" }
            "SUCCESS" -> submissions.filter { it.status == "SUCCESS" }
            "REJECTED" -> submissions.filter { it.status == "REJECTED" }
            else -> submissions
        }
    }

    val totalEarned = remember(submissions) {
        submissions.filter { it.status == "SUCCESS" }.sumOf { it.submittedRate }
    }

    val pendingSubCount = remember(submissions) { submissions.count { it.status == "PENDING" } }
    val successSubCount = remember(submissions) { submissions.count { it.status == "SUCCESS" } }
    val rejectedSubCount = remember(submissions) { submissions.count { it.status == "REJECTED" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Main History Navigation Tabs
        PrimaryTabRow(selectedTabIndex = mainTab, modifier = Modifier.fillMaxWidth()) {
            Tab(
                selected = mainTab == 0,
                onClick = { mainTab = 0; selectedFilter = "ALL" },
                modifier = Modifier.testTag("history_sub_tab")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Submissions (${submissions.size})", fontWeight = FontWeight.Bold)
                }
            }
            Tab(
                selected = mainTab == 1,
                onClick = { mainTab = 1; selectedFilter = "ALL" },
                modifier = Modifier.testTag("history_withdraw_tab")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Withdrawals (${withdrawals.size})", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (mainTab == 0) {
            // SUBMISSIONS VIEW
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Submission Statistics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Surface(
                            color = SuccessGreenContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Earned: ৳${"%.2f".format(totalEarned)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatBadge(label = "Total", count = submissions.size.toString(), color = MaterialTheme.colorScheme.primary)
                        StatBadge(label = "Pending", count = pendingSubCount.toString(), color = PendingYellow)
                        StatBadge(label = "Success", count = successSubCount.toString(), color = SuccessGreen)
                        StatBadge(label = "Rejected", count = rejectedSubCount.toString(), color = RejectedRed)
                    }
                }
            }

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${submissions.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "PENDING",
                    onClick = { selectedFilter = "PENDING" },
                    label = { Text("Pending ($pendingSubCount)") }
                )
                FilterChip(
                    selected = selectedFilter == "SUCCESS",
                    onClick = { selectedFilter = "SUCCESS" },
                    label = { Text("Success ($successSubCount)") }
                )
                FilterChip(
                    selected = selectedFilter == "REJECTED",
                    onClick = { selectedFilter = "REJECTED" },
                    label = { Text("Rejected ($rejectedSubCount)") }
                )
            }

            // Submissions List Grouped by Submit Date
            if (filteredSubmissions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "কোন Submission history পাওয়া যায়নি",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val groupedByDate = remember(filteredSubmissions) {
                    filteredSubmissions.groupBy { it.dateString }
                }

                var selectedDateForDialog by remember { mutableStateOf<String?>(null) }

                if (selectedDateForDialog != null) {
                    val dialogDate = selectedDateForDialog!!
                    val dialogItems = groupedByDate[dialogDate] ?: emptyList()

                    AlertDialog(
                        onDismissRequest = { selectedDateForDialog = null },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("$dialogDate (${dialogItems.size} Accounts)")
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.heightIn(max = 320.dp)) {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(dialogItems, key = { it.id }) { item ->
                                            SubmissionHistoryItemCard(item)
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Quick Action & Download Buttons
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val successCount = dialogItems.count { it.status == "SUCCESS" }
                                    val rejectedCount = dialogItems.count { it.status == "REJECTED" }

                                    // Row 1: Success UID Copy & Rejected UID Copy
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val successList = dialogItems.filter { it.status == "SUCCESS" }.map { it.uid }
                                                if (successList.isEmpty()) {
                                                    Toast.makeText(context, "কোন Success UID পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    clipboardManager.setText(AnnotatedString(successList.joinToString("\n")))
                                                    Toast.makeText(context, "${successList.size} Success UIDs Copied!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Success ($successCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val rejectedList = dialogItems.filter { it.status == "REJECTED" }.map { it.uid }
                                                if (rejectedList.isEmpty()) {
                                                    Toast.makeText(context, "কোন Rejected UID পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    clipboardManager.setText(AnnotatedString(rejectedList.joinToString("\n")))
                                                    Toast.makeText(context, "${rejectedList.size} Rejected UIDs Copied!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RejectedRed)
                                        ) {
                                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Rejected ($rejectedCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                        }
                                    }

                                    // Row 2: All UID Copy & Download File
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val allList = dialogItems.map { it.uid }
                                                if (allList.isEmpty()) {
                                                    Toast.makeText(context, "কোন UID পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    clipboardManager.setText(AnnotatedString(allList.joinToString("\n")))
                                                    Toast.makeText(context, "${allList.size} All UIDs Copied!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("All UIDs (${dialogItems.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                        }

                                        Button(
                                            onClick = {
                                                try {
                                                    val sb = StringBuilder()
                                                    sb.append("=== Submitted Accounts ($dialogDate) ===\n")
                                                    sb.append("Total Accounts: ${dialogItems.size}\n")
                                                    sb.append("----------------------------------\n\n")
                                                    dialogItems.forEachIndexed { index, sub ->
                                                        sb.append("Account #${index + 1}\n")
                                                        sb.append("UID: ${sub.uid}\n")
                                                        sb.append("Category: ${sub.categoryName}\n")
                                                        sb.append("Status: ${sub.status}\n")
                                                        sb.append("Data: ${sub.rawCookie.ifBlank { sub.formattedString.ifBlank { sub.uid } }}\n\n")
                                                    }

                                                    val sendIntent = android.content.Intent().apply {
                                                        action = android.content.Intent.ACTION_SEND
                                                        putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
                                                        putExtra(android.content.Intent.EXTRA_TITLE, "Submitted_Accounts_$dialogDate.txt")
                                                        type = "text/plain"
                                                    }
                                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Download / Save File")
                                                    context.startActivity(shareIntent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Download/Share Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Download File", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { selectedDateForDialog = null }) {
                                Text("Close", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedByDate.forEach { (dateStr, itemsOnDate) ->
                        item(key = "date_header_$dateStr") {
                            val totalValueOnDate = itemsOnDate.sumOf { it.submittedRate }

                            Card(
                                onClick = { selectedDateForDialog = dateStr },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
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
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text(
                                                text = "${itemsOnDate.size} Accounts Submitted",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Earned: ৳${"%.2f".format(totalValueOnDate)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Tap to View",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "View Accounts",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // WITHDRAWALS VIEW
            if (withdrawals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "আপনি এখনো কোনো Withdraw অনুরোধ করেননি",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(withdrawals, key = { it.id }) { item ->
                        WithdrawalHistoryItemCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun StatBadge(label: String, count: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SubmissionHistoryItemCard(item: SubmissionEntity) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    val (statusColor, containerColor, statusText, icon) = when (item.status) {
        "SUCCESS" -> Quadruple(SuccessGreen, SuccessGreenContainer, "Success", Icons.Default.CheckCircle)
        "REJECTED" -> Quadruple(RejectedRed, RejectedRedContainer, "Rejected", Icons.Default.Cancel)
        else -> Quadruple(PendingYellow, PendingYellowContainer, "Pending", Icons.Default.HourglassEmpty)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Category Badge & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    color = containerColor,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Main Account Details: Big UID with 1-Click Copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACCOUNT UID",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.uid,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(item.uid))
                            android.widget.Toast.makeText(context, "UID Copied: ${item.uid}", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy UID", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy UID", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bottom Info Bar: Rate & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Rate: ৳${item.submittedRate}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WithdrawalHistoryItemCard(item: WithdrawalEntity) {
    val (statusColor, containerColor, statusText, icon) = when (item.status) {
        "APPROVED" -> Quadruple(SuccessGreen, SuccessGreenContainer, "Approved", Icons.Default.CheckCircle)
        "REJECTED" -> Quadruple(RejectedRed, RejectedRedContainer, "Rejected", Icons.Default.Cancel)
        else -> Quadruple(PendingYellow, PendingYellowContainer, "Pending", Icons.Default.HourglassEmpty)
    }

    val methodColor = if (item.method == "Bkash") BkashPink else BinanceYellow

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Badge(containerColor = methodColor.copy(alpha = 0.2f)) {
                        Text(
                            text = item.method,
                            color = methodColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = item.dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Acc: ${item.accountDetails}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (item.method == "Binance") "Amount: $${"%.2f".format(item.amountUsd)} (৳${"%.2f".format(item.amountTk)})" else "Amount: ৳${"%.2f".format(item.amountTk)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                color = containerColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
