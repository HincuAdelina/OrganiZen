package com.organizen.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.organizen.app.auth.data.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val repo: AuthRepository = AuthRepository()) : ViewModel() {
    val currentUser get() = repo.currentUser

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(repo.login(email, password).isSuccess)
        }
    }

    fun register(name: String, email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(repo.register(name, email, password).isSuccess)
        }
    }

    fun logout() = repo.logout()
}
