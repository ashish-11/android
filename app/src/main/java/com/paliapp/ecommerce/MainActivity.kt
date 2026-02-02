package com.paliapp.ecommerce

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.paliapp.ecommerce.ui.SplashScreen
import com.paliapp.ecommerce.ui.admin.AdminHomeScreen
import com.paliapp.ecommerce.ui.customer.CustomerHomeScreen
import com.paliapp.ecommerce.ui.auth.LoginScreen
import com.paliapp.ecommerce.ui.theme.WholeSaleShopTheme
import com.paliapp.ecommerce.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WholeSaleShopTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                var showSplash by remember { mutableStateOf(true) }
                var startDestination by remember { mutableStateOf("login") }
                var isCheckingSession by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    try {
                        authViewModel.checkSession { role ->
                            if (role != null) {
                                startDestination = if (role == "ADMIN") "admin" else "customer"
                            }
                            isCheckingSession = false
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error checking session", e)
                        isCheckingSession = false
                    }
                }

                if (showSplash || isCheckingSession) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
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
                                    authVm = authViewModel,
                                    onLogout = {
                                        authViewModel.logout()
                                        navController.navigate("login") {
                                            popUpTo("admin") { inclusive = true }
                                        }
                                    }
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
}
