package com.paliapp.ecommerce.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.paliapp.ecommerce.data.model.User
import com.paliapp.ecommerce.data.repository.AuthRepository

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()
    private var userListener: ListenerRegistration? = null

    var uiState by mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    var currentUserDetails by mutableStateOf<User?>(null)
        private set

    var pendingUsers by mutableStateOf<List<User>>(emptyList())
        private set

    var allCustomers by mutableStateOf<List<User>>(emptyList())
        private set

    init {
        try {
            val currentUser = repo.auth.currentUser
            if (currentUser != null) {
                startObservingUser(currentUser.uid)
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error in init", e)
        }
    }

    fun checkSession(onResult: (String?) -> Unit) {
        try {
            val currentUser = repo.auth.currentUser
            if (currentUser != null) {
                startObservingUser(currentUser.uid)
                repo.getUserRole(currentUser.uid) { result ->
                    result.onSuccess { role ->
                        uiState = AuthUiState.LoggedIn(currentUser.uid, role)
                        onResult(role)
                    }
                    result.onFailure {
                        logout()
                        onResult(null)
                    }
                }
            } else {
                onResult(null)
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error in checkSession", e)
            onResult(null)
        }
    }

    private fun startObservingUser(uid: String) {
        if (uid.isBlank()) {
            Log.e("AuthViewModel", "startObservingUser called with blank UID")
            return
        }
        try {
            userListener?.remove()
            userListener = repo.observeUserDetails(uid) { result ->
                result.onSuccess { user ->
                    currentUserDetails = user
                    if (user.role == "CUSTOMER" && !user.isApproved) {
                        logout()
                    }
                }
                result.onFailure { e ->
                    // Catch the permission denied or not found error when user is deleted
                    Log.e("AuthViewModel", "User document error: ${e.message}")
                    logout()
                }
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error in startObservingUser", e)
        }
    }

    fun loadUserDetails(uid: String) {
        repo.getUserDetails(uid) { result ->
            result.onSuccess { user ->
                currentUserDetails = user
            }
            result.onFailure {
                Log.e("AuthViewModel", "Failed to load user details", it)
            }
        }
    }

    fun updateProfile(name: String, mobile: String, address: String, onResult: (Boolean) -> Unit) {
        val uid = repo.auth.currentUser?.uid ?: return
        repo.updateUserDetails(uid, name, mobile, address) { success ->
            onResult(success)
        }
    }

    fun login(email: String, password: String) {
        uiState = AuthUiState.Loading
        repo.login(email, password) { result ->
            uiState = result.fold(
                onSuccess = { user ->
                    if (user.role == "ADMIN" || user.isApproved) {
                        startObservingUser(user.uid)
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
                    repo.logout()
                    AuthUiState.Registered
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
        try {
            userListener?.remove()
            userListener = null
            repo.logout()
            currentUserDetails = null
            uiState = AuthUiState.Idle
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error in logout", e)
        }
    }

    fun clearState() {
        uiState = AuthUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Registered : AuthUiState()
    data class LoggedIn(val uid: String, val role: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
