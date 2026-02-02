package com.paliapp.ecommerce.ui.customer

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paliapp.ecommerce.viewmodel.AuthViewModel
import com.paliapp.ecommerce.viewmodel.CartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    onLogout: () -> Unit, 
    cartVm: CartViewModel = viewModel(),
    authVm: AuthViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCart by remember { mutableStateOf(false) }
    var showOrders by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    val cartItems by cartVm.cartItems
    val cartItemsCount = cartItems.sumOf { it.qty }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCart) {
        CartScreen(
            onBack = { showCart = false },
            onOrderPlaced = { 
                showCart = false
                showOrders = true
            }
        )
    } else if (showOrders) {
        CustomerOrdersScreen(onBack = { showOrders = false })
    } else if (showSupport) {
        CustomerSupportScreen(onBack = { showSupport = false })
    } else if (showProfile) {
        ProfileScreen(onBack = { showProfile = false }, authVm = authVm)
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (cartItemsCount > 0) {
                                        Badge {
                                            Text(text = cartItemsCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                            }
                        },
                        label = { Text("Shop") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { 
                            showOrders = true
                        },
                        icon = { Icon(Icons.Default.List, contentDescription = null) },
                        label = { Text("My Orders") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { 
                            showProfile = true
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Profile") }
                    )
                }
            }
        ) { paddingValues ->
            Surface(modifier = Modifier.padding(paddingValues)) {
                ProductListScreen(
                    onViewCart = { showCart = true },
                    onLogout = { showLogoutDialog = true },
                    onSupport = { showSupport = true },
                    cartVm = cartVm
                )
            }
        }
    }
}
