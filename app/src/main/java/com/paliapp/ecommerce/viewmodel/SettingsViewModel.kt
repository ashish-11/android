package com.paliapp.ecommerce.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration

class SettingsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private var settingsListener: ListenerRegistration? = null

    private val _adminContact = mutableStateOf("")
    val adminContact: State<String> = _adminContact

    private val _supportMessage = mutableStateOf("")
    val supportMessage: State<String> = _supportMessage

    init {
        loadSettings()
    }

    private fun loadSettings() {
        settingsListener?.remove()
        settingsListener = db.collection("settings").document("support")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.d("SettingsViewModel", "Access denied to support settings (likely due to logout).")
                    } else {
                        Log.w("SettingsViewModel", "Error listening to settings: ${error.message}")
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    _adminContact.value = snapshot.getString("contact") ?: ""
                    _supportMessage.value = snapshot.getString("message") ?: ""
                }
            }
    }

    fun updateSupportSettings(contact: String, message: String): Task<Void> {
        val data = mapOf(
            "contact" to contact,
            "message" to message
        )
        return db.collection("settings").document("support").set(data)
    }

    override fun onCleared() {
        super.onCleared()
        settingsListener?.remove()
    }
}
