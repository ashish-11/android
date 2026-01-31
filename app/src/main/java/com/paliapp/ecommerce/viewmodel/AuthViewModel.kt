package com.paliapp.ecommerce.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.paliapp.ecommerce.data.model.User
import com.paliapp.ecommerce.data.repository.AuthRepository

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    var uiState by mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    var pendingUsers by mutableStateOf<List<User>>(emptyList())
        private set

    var allCustomers by mutableStateOf<List<User>>(emptyList())
        private set

    fun checkSession(onResult: (String?) -> Unit) {
        val currentUser = repo.auth.currentUser
        if (currentUser != null) {
            repo.getUserRole(currentUser.uid) { result ->
                result.onSuccess { role ->
                    uiState = AuthUiState.LoggedIn(currentUser.uid, role)
                    onResult(role)
                }
                result.onFailure {
                    uiState = AuthUiState.Idle
                    onResult(null)
                }
            }
        } else {
            onResult(null)
        }
    }

    fun login(email: String, password: String) {
        uiState = AuthUiState.Loading
        repo.login(email, password) { result ->
            uiState = result.fold(
                onSuccess = { user ->
                    if (user.role == "ADMIN" || user.isApproved) {
                        AuthUiState.LoggedIn(user.uid, user.role)
                    } else {
                        repo.logout()
                        AuthUiState.Error("Your account is pending approval from Admin.")
                    }
                },
                onFailure = { AuthUiState.Error(it.message ?: "Login failed") }
            )
        }
    }


    fun register(name: String, email: String, mobile: String, password: String) {
        uiState = AuthUiState.Loading
        repo.register(name, email, mobile, password) { result ->
            uiState = result.fold(
                onSuccess = {
                    repo.logout() // Logout after registration to prevent auto-login
                    AuthUiState.Error("Registration successful! Please wait for Admin approval.")
                },
                onFailure = {
                    AuthUiState.Error(it.message ?: "Register failed")
                }
            )
        }
    }

    fun resetPassword(email: String) {
        uiState = AuthUiState.Loading
        repo.resetPassword(email) { result ->
            uiState = result.fold(
                onSuccess = {
                    AuthUiState.Error("Password reset email sent to $email")
                },
                onFailure = {
                    AuthUiState.Error(it.message ?: "Reset failed")
                }
            )
        }
    }

    fun loadPendingUsers() {
        repo.getPendingUsers {
            pendingUsers = it
        }
    }

    fun loadAllCustomers() {
        repo.getAllCustomers {
            allCustomers = it
        }
    }

    fun approveUser(uid: String) {
        repo.approveUser(uid) { success ->
            if (success) {
                loadPendingUsers()
                loadAllCustomers()
            }
        }
    }

    fun rejectUser(uid: String) {
        repo.rejectUser(uid) { success ->
            if (success) {
                loadPendingUsers()
                loadAllCustomers()
            }
        }
    }

    fun fetchRole(uid: String, onResult: (String) -> Unit) {
        repo.getUserRole(uid) {
            onResult(it.getOrDefault("CUSTOMER"))
        }
    }

    fun logout() {
        repo.logout()
        uiState = AuthUiState.Idle
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Registered : AuthUiState()
    data class LoggedIn(val uid: String, val role: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
