package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.CategoryEntity
import com.example.data.db.UserEntity
import com.example.ui.theme.BinanceYellow
import com.example.ui.theme.BkashPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    user: UserEntity,
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val categories by viewModel.categories.collectAsState()
    val adminConfig by viewModel.adminConfig.collectAsState()
    val randomName by viewModel.randomName.collectAsState()
    val userProfile by viewModel.currentProfile.collectAsState()

    val currentBalance = userProfile?.balance ?: user.balance
    val userReferCode = userProfile?.referCode ?: user.referCode
    val userReferredCount = userProfile?.referredCount ?: user.referredCount

    val activeAdminPassword = adminConfig?.defaultPassword ?: "aponkhan21"
    val isSubmissionEnabled = adminConfig?.isSubmissionEnabled ?: true
    val referralBonus = adminConfig?.referralBonus ?: 10.0
    val perDollarRate = adminConfig?.perDollarRate ?: 120.0

    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var rawCookieText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Withdrawal Dialog State
    var showWithdrawDialog by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (selectedCategory == null && categories.isNotEmpty()) {
            selectedCategory = categories.first()
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
        // Balance & Welcome Card with Withdraw Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_user_banner"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "স্বাগতম, ${userProfile?.name ?: user.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = userProfile?.email ?: user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Balance",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "৳ ${"%.2f".format(currentBalance)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ডলার রেট: $1 = ৳${"%.1f".format(perDollarRate)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { showWithdrawDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("withdraw_trigger_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Withdraw",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Withdraw (উইথড্র)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Referral Code Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "আপনার রেফারেল কোড",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            text = "Total Referrals: $userReferredCount",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Refer Code",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = userReferCode.ifBlank { "N/A" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (userReferCode.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(userReferCode))
                                    Toast.makeText(context, "রেফারেল কোড কপি হয়েছে: $userReferCode", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Refer Code",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Code", fontSize = 12.sp)
                        }
                    }
                }

                Text(
                    text = "💡 আপনার এই রেফারেল কোড ব্যবহার করে কেউ নতুন account খুললে আপনি সাথে সাথে ৳${"%.0f".format(referralBonus)} বোনাস পাবেন!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Random First Name & Last Name Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Random Name Generator",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = { viewModel.generateNewRandomName() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "New Random Name",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "First & Last Name",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = randomName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(randomName))
                            Toast.makeText(context, "Name copied: $randomName", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Name",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp)
                    }
                }
            }
        }

        // Copy Password & Critical Warning Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Account Creation Password",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Admin Set Password",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = activeAdminPassword,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(activeAdminPassword))
                                Toast.makeText(context, "Password Copied: $activeAdminPassword", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("copy_password_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Password",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Pass", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // CRITICAL WARNING NOTE DISPLAYED UNDER PASSWORD
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "এই password দিয়ে account না করলে report আসবে না এবং আপনার account ban ও হয়ে যেতে পারে।",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Account / Cookie Submission Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Submit Account Cookie",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isSubmissionEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = "Admin বর্তমানে Submission বন্ধ রেখেছেন। পরে চেষ্টা করুন।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Category Dropdown
                Text(
                    text = "Select Account Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.let { "${it.name} — ৳${it.rate}/acc" } ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("category_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cat.name, fontWeight = FontWeight.SemiBold)
                                        Text("৳${cat.rate}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Cookie input box
                OutlinedTextField(
                    value = rawCookieText,
                    onValueChange = { rawCookieText = it },
                    label = { Text("Paste Raw Cookie String") },
                    placeholder = {
                        Text(
                            "e.g. user datr=Ebl3...; c_user=61593203065886; xs=17... \n\n(একাধিক cookie দিলে প্রতি লাইনে ১টি করে দিন)"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 220.dp)
                        .testTag("cookie_text_input"),
                    enabled = isSubmissionEnabled
                )

                Button(
                    onClick = {
                        val cat = selectedCategory
                        if (cat == null) {
                            Toast.makeText(context, "Category নির্বাচন করুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (rawCookieText.isBlank()) {
                            Toast.makeText(context, "Cookie টেক্সট পেস্ট করুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSubmitting = true
                        viewModel.submitCookieAccounts(
                            category = cat,
                            assignedPassword = activeAdminPassword,
                            rawCookieText = rawCookieText
                        ) { success, msg ->
                            isSubmitting = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (success) {
                                rawCookieText = ""
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_cookie_btn"),
                    enabled = isSubmissionEnabled && !isSubmitting,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Text("Submit Cookie Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // WITHDRAWAL MODAL DIALOG
    if (showWithdrawDialog) {
        WithdrawalDialog(
            userBalance = currentBalance,
            perDollarRate = perDollarRate,
            onDismiss = { showWithdrawDialog = false },
            onConfirmWithdraw = { method, accountDetails, amountTk, amountUsd ->
                viewModel.requestWithdrawal(
                    method = method,
                    accountDetails = accountDetails,
                    amountTk = amountTk,
                    amountUsd = amountUsd
                ) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    if (success) {
                        showWithdrawDialog = false
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalDialog(
    userBalance: Double,
    perDollarRate: Double,
    onDismiss: () -> Unit,
    onConfirmWithdraw: (method: String, accountDetails: String, amountTk: Double, amountUsd: Double) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("Bkash") } // Bkash or Binance
    var bkashNumber by remember { mutableStateOf("") }
    var bkashAmountTk by remember { mutableStateOf("") }

    var binancePayId by remember { mutableStateOf("") }
    var binanceAmountUsd by remember { mutableStateOf("") }

    val computedBinanceTk = (binanceAmountUsd.toDoubleOrNull() ?: 0.0) * perDollarRate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Withdraw Request (টাকা উত্তোলন)")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("আপনার বর্তমান ব্যালেন্স:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "৳${"%.2f".format(userBalance)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text("উইথড্র মাধ্যম নির্বাচন করুন:", fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Bkash Option Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMethod = "Bkash" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedMethod == "Bkash") BkashPink.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (selectedMethod == "Bkash") androidx.compose.foundation.BorderStroke(2.dp, BkashPink) else null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = BkashPink)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Bkash", fontWeight = FontWeight.Bold, color = BkashPink)
                            Text("Min: ৳৫০", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Binance Option Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMethod = "Binance" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedMethod == "Binance") BinanceYellow.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (selectedMethod == "Binance") androidx.compose.foundation.BorderStroke(2.dp, BinanceYellow) else null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = BinanceYellow)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Binance", fontWeight = FontWeight.Bold, color = Color(0xFFB78103))
                            Text("Min: 20$ (USDT)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (selectedMethod == "Bkash") {
                    OutlinedTextField(
                        value = bkashNumber,
                        onValueChange = { bkashNumber = it },
                        label = { Text("বিকাশ মোবাইল নম্বর") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bkashAmountTk,
                        onValueChange = { bkashAmountTk = it },
                        label = { Text("উইথড্র করার পরিমাণ (টাকায়)") },
                        placeholder = { Text("সর্বনিম্ন ৳৫০") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = binancePayId,
                        onValueChange = { binancePayId = it },
                        label = { Text("Binance Pay ID / USDT Address") },
                        leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = binanceAmountUsd,
                        onValueChange = { binanceAmountUsd = it },
                        label = { Text("উইথড্র করার পরিমাণ (USDT/Dollar)") },
                        placeholder = { Text("সর্বনিম্ন $20") },
                        leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "প্রতি ডলার রেট: ৳${"%.1f".format(perDollarRate)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                "মোট কাটা যাবে: ৳${"%.2f".format(computedBinanceTk)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedMethod == "Bkash") {
                        val amount = bkashAmountTk.toDoubleOrNull() ?: 0.0
                        onConfirmWithdraw("Bkash", bkashNumber, amount, 0.0)
                    } else {
                        val amountUsd = binanceAmountUsd.toDoubleOrNull() ?: 0.0
                        onConfirmWithdraw("Binance", binancePayId, computedBinanceTk, amountUsd)
                    }
                },
                modifier = Modifier.testTag("confirm_withdraw_btn")
            ) {
                Text("Confirm Withdraw")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
