package com.example.cocktailbar.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cocktailbar.R
import com.example.cocktailbar.data.model.Template
import com.example.cocktailbar.data.repository.TemplatesRepository
import com.example.cocktailbar.ui.common.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TemplatesViewModel(
    private val repository: TemplatesRepository = TemplatesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Template>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Template>>> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<Int>()
    val toastMessage = _toastMessage.asSharedFlow()

    init {
        loadTemplates()
    }

    fun loadTemplates() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            repository.getTemplates()
                .onSuccess { templates ->
                    _uiState.value = if (templates.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(templates)
                    }
                }
                .onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Unknown error")
                }
        }
    }

    fun deleteTemplate(template: Template) {
        viewModelScope.launch {
            repository.deleteTemplate(template.id)
                .onSuccess {
                    _toastMessage.emit(R.string.delete)
                    loadTemplates()
                }
                .onFailure {
                    _toastMessage.emit(R.string.error)
                }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TemplatesViewModel() as T
        }
    }
}