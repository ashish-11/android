package com.paliapp.ecommerce.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paliapp.ecommerce.viewmodel.AuthUiState
import com.paliapp.ecommerce.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onSuccess: (String, String) -> Unit,
    vm: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }
    
    var showForgotDialog by remember { mutableStateOf(false) }

    val uiState = vm.uiState

    // Handle successful login
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.LoggedIn) {
            onSuccess(uiState.uid, uiState.role)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRegister) "Create Account" else "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = if (isRegister) "Sign up to start shopping" else "Login to continue shopping",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error Message Display
                AnimatedVisibility(visible = uiState is AuthUiState.Error) {
                    if (uiState is AuthUiState.Error) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = uiState.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (isRegister) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!isRegister) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = { showForgotDialog = true }) {
                            Text("Forgot Password?", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Create Password") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Button(
                    onClick = {
                        if (isRegister) {
                            vm.register(name, email, mobile, password)
                        } else {
                            vm.login(email, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = uiState !is AuthUiState.Loading
                ) {
                    if (uiState is AuthUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(if (isRegister) "Register" else "Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = { 
                    isRegister = !isRegister
                }) {
                    Text(
                        if (isRegister)
                            "Already have an account? Login"
                        else
                            "New user? Create an Account",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }

    if (showForgotDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("We will send a password reset link to your email address.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.resetPassword(resetEmail)
                    showForgotDialog = false
                }) {
                    Text("Send Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
