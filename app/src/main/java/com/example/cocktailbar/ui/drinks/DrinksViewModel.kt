package com.example.cocktailbar.ui.drinks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cocktailbar.data.model.Drink
import com.example.cocktailbar.data.model.DrinkVariant
import com.example.cocktailbar.data.repository.DrinksRepository
import com.example.cocktailbar.ui.common.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DrinksViewModel(
    private val repository: DrinksRepository = DrinksRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Drink>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Drink>>> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<Int>()
    val toastMessage = _toastMessage.asSharedFlow()

    private var allDrinks: List<Drink> = emptyList()
    private var currentQuery: String = ""

    init {
        loadDrinks()
    }

    fun loadDrinks() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            repository.getDrinksWithVariants()
                .onSuccess { drinks ->
                    allDrinks = drinks
                    applyFilter()
                }
                .onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Unknown error")
                }
        }
    }

    fun setSearchQuery(query: String) {
        currentQuery = query
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = if (currentQuery.isEmpty()) {
            allDrinks
        } else {
            allDrinks.filter { drink ->
                drink.name.contains(currentQuery, ignoreCase = true) ||
                        drink.description?.contains(currentQuery, ignoreCase = true) == true
            }
        }

        _uiState.value = if (filtered.isEmpty() && allDrinks.isEmpty()) {
            UiState.Empty
        } else {
            UiState.Success(filtered)
        }
    }

    fun addDrink(name: String, description: String, variants: List<DrinkVariant>) {
        viewModelScope.launch {
            repository.addDrink(name, description, variants)
                .onSuccess {
                    _toastMessage.emit(com.example.cocktailbar.R.string.save)
                    loadDrinks()
                }
                .onFailure {
                    _toastMessage.emit(com.example.cocktailbar.R.string.error)
                }
        }
    }

    fun updateDrink(id: String, name: String, description: String, variants: List<DrinkVariant>) {
        viewModelScope.launch {
            repository.updateDrink(id, name, description, variants)
                .onSuccess {
                    _toastMessage.emit(com.example.cocktailbar.R.string.save)
                    loadDrinks()
                }
                .onFailure {
                    _toastMessage.emit(com.example.cocktailbar.R.string.error)
                }
        }
    }

    fun deleteDrink(drink: Drink) {
        viewModelScope.launch {
            repository.deleteDrink(drink.id)
                .onSuccess {
                    _toastMessage.emit(com.example.cocktailbar.R.string.delete)
                    loadDrinks()
                }
                .onFailure {
                    _toastMessage.emit(com.example.cocktailbar.R.string.error)
                }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DrinksViewModel() as T
        }
    }
}