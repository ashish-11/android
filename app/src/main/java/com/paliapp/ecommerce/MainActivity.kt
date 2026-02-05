package com.paliapp.ecommerce

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.paliapp.ecommerce.ui.admin.AdminHomeScreen
import com.paliapp.ecommerce.ui.customer.CustomerHomeScreen
import com.paliapp.ecommerce.ui.auth.LoginScreen
import com.paliapp.ecommerce.ui.theme.WholeSaleShopTheme
import com.paliapp.ecommerce.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askNotificationPermission()

        setContent {
            WholeSaleShopTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    try {
                        authViewModel.checkSession { role ->
                            startDestination = if (role != null) {
                                if (role == "ADMIN") "admin" else "customer"
                            } else {
                                "login"
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error checking session", e)
                        startDestination = "login"
                    }
                }

                if (startDestination != null) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = startDestination!!,
                            modifier = Modifier.padding(innerPadding)
                        ) {

                            composable("login") {
                                LoginScreen(
                                    vm = authViewModel,
                                    onSuccess = { _, role ->
                                        if (role == "ADMIN") {
                                            navController.navigate("admin") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        } else {
                                            navController.navigate("customer") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }

                            composable("admin") {
                                AdminHomeScreen(
                                    onLogout = {
                                        authViewModel.logout()
                                        navController.navigate("login") {
                                            popUpTo("admin") { inclusive = true }
                                        }
                                    },
                                    authVm = authViewModel
                                )
                            }

                            composable("customer") {
                                CustomerHomeScreen(
                                    authVm = authViewModel,
                                    onLogout = {
                                        authViewModel.logout()
                                        navController.navigate("login") {
                                            popUpTo("customer") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
