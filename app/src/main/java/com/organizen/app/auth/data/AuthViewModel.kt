package com.organizen.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.organizen.app.auth.data.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val repo: AuthRepository = AuthRepository()) : ViewModel() {
    val currentUser get() = repo.currentUser

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repo.login(email, password)
            onResult(result.isSuccess, result.exceptionOrNull()?.message)
        }
    }

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repo.register(email, password)
            onResult(result.isSuccess, result.exceptionOrNull()?.message)
        }
    }

    fun logout() {
        repo.logout()
    }
}
