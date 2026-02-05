package com.paliapp.ecommerce.ui.customer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    // Modern Navigation Logic
    Box(modifier = Modifier.fillMaxSize()) {
        if (showCart) {
            CartScreen(
                onBack = { showCart = false },
                onOrderPlaced = { 
                    showCart = false
                    showOrders = true
                    selectedTab = 1
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
                    PremiumBottomNavigation(
                        selectedTab = selectedTab,
                        onTabSelected = { index ->
                            selectedTab = index
                            when(index) {
                                1 -> showOrders = true
                                2 -> showProfile = true
                            }
                        },
                        cartCount = cartItemsCount
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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
}

@Composable
fun PremiumBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    cartCount: Int
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding().height(72.dp)
    ) {
        val items = listOf(
            NavigationItemData("Shop", Icons.Default.LocalMall, 0),
            NavigationItemData("Orders", Icons.Default.List, 1),
            NavigationItemData("Profile", Icons.Default.Person, 2)
        )

        items.forEach { item ->
            val isSelected = selectedTab == item.index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.index) },
                icon = {
                    AnimatedNavigationIcon(
                        icon = item.icon,
                        isSelected = isSelected,
                        badgeCount = if (item.index == 0) cartCount else 0
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun AnimatedNavigationIcon(
    icon: ImageVector,
    isSelected: Boolean,
    badgeCount: Int
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = ""
    )

    BadgedBox(
        badge = {
            if (badgeCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                ) {
                    Text(badgeCount.toString())
                }
            }
        },
        modifier = Modifier.scale(scale)
    ) {
        Icon(icon, contentDescription = null)
    }
}

data class NavigationItemData(
    val label: String,
    val icon: ImageVector,
    val index: Int
)
