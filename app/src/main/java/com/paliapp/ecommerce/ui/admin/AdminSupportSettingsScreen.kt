package com.paliapp.ecommerce.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paliapp.ecommerce.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSupportSettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    var contact by remember { mutableStateOf(vm.adminContact.value) }
    var message by remember { mutableStateOf(vm.supportMessage.value) }

    LaunchedEffect(vm.adminContact.value, vm.supportMessage.value) {
        contact = vm.adminContact.value
        message = vm.supportMessage.value
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                vm.updateSupportSettings(contact, message)
                onBack()
            }) {
                Icon(Icons.Default.Save, contentDescription = "Save")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Configure the support information visible to customers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Admin Contact Number") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. +91 9876543210") }
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Support Message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text("Instructions for the customer...") }
            )
        }
    }
}
