package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BinanceYellow
import com.example.ui.theme.BkashPink

@Composable
fun WithdrawModalDialog(
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
                        modifier = Modifier.fillMaxWidth().testTag("withdraw_bkash_number_input")
                    )

                    OutlinedTextField(
                        value = bkashAmountTk,
                        onValueChange = { bkashAmountTk = it },
                        label = { Text("উইথড্র করার পরিমাণ (টাকায়)") },
                        placeholder = { Text("সর্বনিম্ন ৳৫০") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("withdraw_bkash_amount_input")
                    )
                } else {
                    OutlinedTextField(
                        value = binancePayId,
                        onValueChange = { binancePayId = it },
                        label = { Text("Binance Pay ID / USDT Address") },
                        leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("withdraw_binance_id_input")
                    )

                    OutlinedTextField(
                        value = binanceAmountUsd,
                        onValueChange = { binanceAmountUsd = it },
                        label = { Text("উইথড্র করার পরিমাণ (USDT/Dollar)") },
                        placeholder = { Text("সর্বনিম্ন $20") },
                        leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("withdraw_binance_amount_input")
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
