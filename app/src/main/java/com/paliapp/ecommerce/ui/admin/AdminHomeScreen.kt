package com.paliapp.ecommerce.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.paliapp.ecommerce.R
import com.paliapp.ecommerce.viewmodel.AuthViewModel
import com.paliapp.ecommerce.viewmodel.OrderViewModel
import com.paliapp.ecommerce.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    onLogout: () -> Unit,
    authVm: AuthViewModel = viewModel(),
    orderVm: OrderViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(-1) } // -1 for Dashboard
    var showDetail by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showManageCategories by remember { mutableStateOf(false) }
    
    // Track which page to show within the Orders container
    var initialOrderPage by remember { mutableIntStateOf(0) }

    // Load orders once at the top level
    LaunchedEffect(Unit) {
        orderVm.loadAllOrders()
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("लॉगआउट (Logout)") },
            text = { Text("क्या आप लॉगआउट करना चाहते हैं?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("हाँ, लॉगआउट करें", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    if (showManageCategories) {
        AdminManageCategoriesScreen(onBack = { showManageCategories = false })
    } else if (showDetail) {
        when (selectedTab) {
            0 -> AdminManageProductsScreen(
                onBack = { showDetail = false; selectedTab = -1 },
                onManageCategories = { showManageCategories = true }
            )
            1 -> AdminOrdersContainer(
                initialPage = initialOrderPage,
                onBack = { showDetail = false; selectedTab = -1 }, 
                orderVm = orderVm
            )
            3 -> AdminUserApprovalScreen(onBack = { showDetail = false; selectedTab = -1 })
            4 -> AdminSupportSettingsScreen(onBack = { showDetail = false; selectedTab = -1 })
        }
    } else {
        Scaffold(
            topBar = {
                Surface(shadowElevation = 4.dp) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = R.mipmap.ic_launcher,
                                    contentDescription = "Logo",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "पंडित मार्ट",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "एडमिन पैनल",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { showLogoutDialog = true }) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color.Gray)
                            }
                        }
                    )
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(
                        selected = selectedTab == -1,
                        onClick = { 
                            selectedTab = -1 
                            showDetail = false
                        },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        label = { Text("डैशबोर्ड", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { 
                            selectedTab = 3 
                            showDetail = true
                        },
                        icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                        label = { Text("ग्राहक", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { 
                            selectedTab = 0 
                            showDetail = true
                        },
                        icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                        label = { Text("सामान", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { 
                            selectedTab = 1 
                            initialOrderPage = 0
                            showDetail = true
                        },
                        icon = { Icon(Icons.Default.List, contentDescription = null) },
                        label = { Text("ऑर्डर", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { 
                            selectedTab = 4 
                            showDetail = true
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("सेटिंग्स", fontSize = 10.sp) }
                    )
                }
            }
        ) { paddingValues ->
            AdminDashboard(
                modifier = Modifier.padding(paddingValues),
                onNavigateToProducts = {
                    selectedTab = 0
                    showDetail = true
                },
                onNavigateToUsers = {
                    selectedTab = 3
                    showDetail = true
                },
                onNavigateToOrders = { page ->
                    initialOrderPage = page
                    selectedTab = 1
                    showDetail = true
                },
                authVm = authVm,
                orderVm = orderVm
            )
        }
    }
}

@Composable
fun AdminOrdersContainer(initialPage: Int, onBack: () -> Unit, orderVm: OrderViewModel) {
    var selectedPage by remember { mutableIntStateOf(initialPage) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedPage, edgePadding = 16.dp) {
            Tab(
                selected = selectedPage == 0,
                onClick = { selectedPage = 0 },
                text = { Text("नए ऑर्डर") }
            )
            Tab(
                selected = selectedPage == 1,
                onClick = { selectedPage = 1 },
                text = { Text("डिलीवर हुए") }
            )
            Tab(
                selected = selectedPage == 2,
                onClick = { selectedPage = 2 },
                text = { Text("वापसी") }
            )
        }
        
        when (selectedPage) {
            0 -> AdminOrdersScreen(onBack = onBack, orderVm = orderVm)
            1 -> AdminDeliveredOrdersScreen(onBack = onBack, orderVm = orderVm)
            2 -> AdminReturnsScreen(onBack = onBack, orderVm = orderVm)
        }
    }
}

@Composable
fun AdminDashboard(
    onNavigateToProducts: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToOrders: (Int) -> Unit, // Int for tab index
    modifier: Modifier = Modifier,
    orderVm: OrderViewModel,
    productVm: ProductViewModel = viewModel(),
    authVm: AuthViewModel = viewModel()
) {
    val orders by orderVm.orders
    val products by productVm.products
    val pendingUsers = authVm.pendingUsers
    
    val pendingCount = orders.count { it.status == "PLACED" }
    val productCount = products.size
    val returnCount = orders.count { it.status in listOf("RETURN_REQUESTED", "RETURN_APPROVED", "REFUNDED") }

    LaunchedEffect(Unit) {
        productVm.loadAllProductsForAdmin()
        authVm.loadPendingUsers()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "व्यापार की जानकारी",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatCard(
                    title = "नए ग्राहक",
                    count = pendingUsers.size.toString(),
                    icon = Icons.Default.GroupAdd,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onNavigateToUsers
                )
            }
            item {
                StatCard(
                    title = "बाकी ऑर्डर",
                    count = pendingCount.toString(),
                    icon = Icons.Default.PendingActions,
                    color = MaterialTheme.colorScheme.errorContainer,
                    textColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = { onNavigateToOrders(0) }
                )
            }
            item {
                StatCard(
                    title = "वापसी की गुज़ारिश",
                    count = returnCount.toString(),
                    icon = Icons.Default.KeyboardReturn,
                    color = Color(0xFFFFF3E0),
                    textColor = Color(0xFFE65100),
                    onClick = { onNavigateToOrders(2) }
                )
            }
            item {
                StatCard(
                    title = "कुल सामान",
                    count = productCount.toString(),
                    icon = Icons.Default.Inventory,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onNavigateToProducts
                )
            }
            item {
                val revenueStatuses = listOf("DELIVERED", "RETURN_REQUESTED", "RETURN_APPROVED", "REFUNDED", "RETURNED")
                val totalRevenue = orders.filter { it.status in revenueStatuses }.sumOf { it.totalAmount }
                
                StatCard(
                    title = "कुल कमाई",
                    count = "₹${totalRevenue.toInt()}",
                    icon = Icons.Default.Payments,
                    color = Color(0xFFE8F5E9),
                    textColor = Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    textColor: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = count,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
