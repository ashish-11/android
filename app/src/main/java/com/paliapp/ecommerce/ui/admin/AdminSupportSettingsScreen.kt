package com.paliapp.ecommerce.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paliapp.ecommerce.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSupportSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    var whatsappNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val currentContact by viewModel.adminContact
    
    LaunchedEffect(currentContact) {
        whatsappNumber = currentContact
        if (currentContact.isNotEmpty() || !isLoading) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("कस्टमर सपोर्ट सेटिंग") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "पीछे")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "अपने ग्राहकों के लिए सहायता विकल्प सेट करें।",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = whatsappNumber,
                    onValueChange = { whatsappNumber = it },
                    label = { Text("वॉट्सऐप सहायता नंबर (WhatsApp)") },
                    placeholder = { Text("जैसे: 919876543210") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    helperText = { Text("कंट्री कोड के साथ लिखें (बिना + के)") }
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        isSaving = true
                        viewModel.updateSupportSettings(whatsappNumber, "")
                            .addOnSuccessListener {
                                isSaving = false
                                Toast.makeText(context, "सेटिंग्स सुरक्षित हो गई", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                isSaving = false
                                Toast.makeText(context, "सेटिंग्स सेव करने में विफल: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("सेटिंग्स सुरक्षित करें")
                    }
                }
            }
        }
    }
}

@Composable
private fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    placeholder: @Composable () -> Unit,
    modifier: Modifier,
    shape: RoundedCornerShape,
    helperText: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            modifier = Modifier.fillMaxWidth(),
            shape = shape
        )
        Box(modifier = Modifier.padding(start = 12.dp, top = 4.dp)) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                    helperText()
                }
            }
        }
    }
}
