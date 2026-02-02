package com.paliapp.ecommerce.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class SettingsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _adminContact = mutableStateOf("")
    val adminContact: State<String> = _adminContact

    private val _supportMessage = mutableStateOf("")
    val supportMessage: State<String> = _supportMessage

    init {
        loadSettings()
    }

    private fun loadSettings() {
        db.collection("settings").document("support")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("SettingsViewModel", "Error listening to settings: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    _adminContact.value = snapshot.getString("contact") ?: ""
                    _supportMessage.value = snapshot.getString("message") ?: ""
                }
            }
    }

    fun updateSupportSettings(contact: String, message: String) {
        val data = mapOf(
            "contact" to contact,
            "message" to message
        )
        db.collection("settings").document("support").set(data)
    }
}
