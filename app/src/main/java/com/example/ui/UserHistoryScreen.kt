package com.example.ui

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SubmissionEntity
import com.example.data.db.WithdrawalEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHistoryScreen(viewModel: AppViewModel) {
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

            // Submissions List
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
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "কোন Submission history পাওয়া যায়নি",
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
                    items(filteredSubmissions, key = { it.id }) { item ->
                        SubmissionHistoryItemCard(item)
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
    val (statusColor, containerColor, statusText, icon) = when (item.status) {
        "SUCCESS" -> Quadruple(SuccessGreen, SuccessGreenContainer, "Success", Icons.Default.CheckCircle)
        "REJECTED" -> Quadruple(RejectedRed, RejectedRedContainer, "Rejected", Icons.Default.Cancel)
        else -> Quadruple(PendingYellow, PendingYellowContainer, "Pending", Icons.Default.HourglassEmpty)
    }

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
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "UID: ${item.uid}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Date: ${item.dateString} | Rate: ৳${item.submittedRate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
