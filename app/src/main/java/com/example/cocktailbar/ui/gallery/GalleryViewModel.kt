package com.example.cocktailbar.ui.gallery

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cocktailbar.R
import com.example.cocktailbar.data.model.GalleryImage
import com.example.cocktailbar.data.repository.GalleryRepository
import com.example.cocktailbar.ui.common.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val repository: GalleryRepository = GalleryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<GalleryImage>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<GalleryImage>>> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<Int>()
    val toastMessage = _toastMessage.asSharedFlow()

    private val _currentBucket = MutableStateFlow(BUCKET_BACKGROUNDS)
    val currentBucket: StateFlow<String> = _currentBucket.asStateFlow()

    init {
        loadImages()
    }

    fun loadImages() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            repository.getImages(_currentBucket.value)
                .onSuccess { images ->
                    _uiState.value = if (images.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(images)
                    }
                }
                .onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Unknown error")
                }
        }
    }

    fun switchBucket(bucket: String) {
        if (_currentBucket.value != bucket) {
            _currentBucket.value = bucket
            loadImages()
        }
    }

    fun uploadImage(fileName: String, bytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            repository.uploadImage(_currentBucket.value, fileName, bytes)
                .onSuccess {
                    _toastMessage.emit(R.string.upload_success)
                    loadImages()
                }
                .onFailure {
                    _toastMessage.emit(R.string.upload_failed)
                    loadImages() // Reload to show current state
                }
        }
    }

    fun deleteImage(image: GalleryImage) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            repository.deleteImage(image.bucket, image.name)
                .onSuccess {
                    _toastMessage.emit(R.string.delete)
                    loadImages()
                }
                .onFailure {
                    _toastMessage.emit(R.string.error)
                    loadImages()
                }
        }
    }

    fun isBackgroundsSelected(): Boolean = _currentBucket.value == BUCKET_BACKGROUNDS

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GalleryViewModel() as T
        }
    }

    companion object {
        const val BUCKET_BACKGROUNDS = "backgrounds"
        const val BUCKET_LOGOS = "logos"
    }
}