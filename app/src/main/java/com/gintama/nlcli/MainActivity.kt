package com.gintama.nlcli

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gintama.nlcli.ui.CommandBarScreen
import com.gintama.nlcli.ui.HistoryScreen
import com.gintama.nlcli.ui.theme.NLCLITheme
import com.gintama.nlcli.ui.theme.TerminalBackground
import com.gintama.nlcli.ui.viewmodel.CliViewModel
import com.gintama.nlcli.ui.viewmodel.HistoryViewModel

class MainActivity : ComponentActivity() {

    private val cliViewModel: CliViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NLCLITheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    cliViewModel.refreshPermissions()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TerminalBackground
                ) {
                    AppNavigation(
                        cliViewModel = cliViewModel,
                        historyViewModel = historyViewModel,
                        onRequestPermissions = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.SEND_SMS,
                                    Manifest.permission.CALL_PHONE
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cliViewModel.refreshPermissions()
    }
}

@Composable
fun AppNavigation(
    cliViewModel: CliViewModel,
    historyViewModel: HistoryViewModel,
    onRequestPermissions: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "terminal"
    ) {
        composable("terminal") {
            CommandBarScreen(
                viewModel = cliViewModel,
                onNavigateToHistory = { navController.navigate("history") },
                onRequestContactsPermission = onRequestPermissions
            )
        }

        composable("history") {
            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateBack = { navController.popBackStack() },
                onReplayCommand = { command ->
                    cliViewModel.executeCommand(command)
                    navController.popBackStack()
                }
            )
        }
    }
}
