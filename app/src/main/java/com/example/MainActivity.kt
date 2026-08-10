package com.example.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppEntry(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppEntry(viewModel: AppViewModel) {
    val authState by viewModel.authState.collectAsState()
    var userNavTab by remember { mutableIntStateOf(0) } // 0: Home, 1: History, 2: Profile

    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    when (val state = authState) {
        is AuthState.LoggedOut -> {
            AuthScreen(viewModel = viewModel)
        }

        is AuthState.LoggedInAdmin -> {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    AdminDashboardScreen(viewModel = viewModel)
                }
            }
        }

        is AuthState.LoggedInUser -> {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = userNavTab == 0,
                            onClick = { userNavTab = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontWeight = if (userNavTab == 0) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_home")
                        )

                        NavigationBarItem(
                            selected = userNavTab == 1,
                            onClick = { userNavTab = 1 },
                            icon = { Icon(Icons.Default.History, contentDescription = "History") },
                            label = { Text("History", fontWeight = if (userNavTab == 1) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_history")
                        )

                        NavigationBarItem(
                            selected = userNavTab == 2,
                            onClick = { userNavTab = 2 },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile", fontWeight = if (userNavTab == 2) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_profile")
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (userNavTab) {
                        0 -> UserHomeScreen(user = state.user, viewModel = viewModel)
                        1 -> UserHistoryScreen(viewModel = viewModel)
                        2 -> UserProfileScreen(user = state.user, viewModel = viewModel)
                    }
                }
            }
        }
    }
}
