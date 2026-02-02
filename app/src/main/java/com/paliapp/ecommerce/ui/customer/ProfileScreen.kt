package com.paliapp.ecommerce.ui.customer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paliapp.ecommerce.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    authVm: AuthViewModel = viewModel()
) {
    val userDetails = authVm.currentUserDetails
    
    // Initialize state with whatever is currently in userDetails
    var name by remember(userDetails) { mutableStateOf(userDetails?.name ?: "") }
    var mobile by remember(userDetails) { mutableStateOf(userDetails?.mobile ?: "") }
    var address by remember(userDetails) { mutableStateOf(userDetails?.address ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = userDetails?.email ?: "No email found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = mobile,
                onValueChange = { mobile = it },
                label = { Text("Mobile Number") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Delivery Address") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    color = if (message.contains("success", true)) 
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    isLoading = true
                    authVm.updateProfile(name, mobile, address) { success ->
                        isLoading = false
                        message = if (success) "Profile updated successfully!" else "Failed to update profile."
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Update Profile")
                }
            }
        }
    }
}
