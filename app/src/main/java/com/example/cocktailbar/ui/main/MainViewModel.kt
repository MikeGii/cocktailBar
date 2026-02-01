package com.example.cocktailbar.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cocktailbar.R
import com.example.cocktailbar.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _toastMessage = MutableSharedFlow<Int>()
    val toastMessage = _toastMessage.asSharedFlow()

    private val _loginResult = MutableSharedFlow<Boolean>()
    val loginResult = _loginResult.asSharedFlow()

    init {
        _isAdminLoggedIn.value = authRepository.isAdminLoggedIn
    }

    fun checkConnection() {
        viewModelScope.launch {
            _isConnected.value = authRepository.checkConnection()
        }
    }

    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }

    fun syncData() {
        if (!_isConnected.value) {
            viewModelScope.launch {
                _toastMessage.emit(R.string.sync_failed)
            }
            return
        }

        viewModelScope.launch {
            _toastMessage.emit(R.string.syncing)

            val success = authRepository.checkConnection()
            _isConnected.value = success

            if (success) {
                _toastMessage.emit(R.string.sync_success)
            } else {
                _toastMessage.emit(R.string.sync_failed)
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            authRepository.login(username, password)
                .onSuccess { isValid ->
                    if (isValid) {
                        _isAdminLoggedIn.value = true
                        _toastMessage.emit(R.string.login_success)
                    }
                    _loginResult.emit(isValid)
                }
                .onFailure {
                    _loginResult.emit(false)
                }
        }
    }

    fun logout() {
        authRepository.logout()
        _isAdminLoggedIn.value = false
        viewModelScope.launch {
            _toastMessage.emit(R.string.logout)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(AuthRepository(context)) as T
        }
    }
}