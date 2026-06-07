package ee.giidev.menuud.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ee.giidev.menuud.R
import ee.giidev.menuud.data.model.GalleryImage
import ee.giidev.menuud.data.repository.GalleryRepository
import ee.giidev.menuud.ui.common.UiState
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

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _toastMessage = MutableSharedFlow<Int>()
    val toastMessage = _toastMessage.asSharedFlow()

    init {
        loadImages()
    }

    fun loadImages() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            repository.getImages(BUCKET)
                .onSuccess { images ->
                    _isConnected.value = true
                    _uiState.value = if (images.isEmpty()) UiState.Empty else UiState.Success(images)
                }
                .onFailure {
                    _isConnected.value = false
                    _uiState.value = UiState.Error(it.message ?: "Unknown error")
                }
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            _isConnected.value = repository.checkConnection(BUCKET)
        }
    }

    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }

    fun syncData() {
        viewModelScope.launch {
            _toastMessage.emit(R.string.syncing)
            val connected = repository.checkConnection(BUCKET)
            _isConnected.value = connected
            if (connected) {
                loadImages()
                _toastMessage.emit(R.string.sync_success)
            } else {
                _toastMessage.emit(R.string.sync_failed)
            }
        }
    }

    fun uploadImage(fileName: String, bytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            repository.uploadImage(BUCKET, fileName, bytes)
                .onSuccess {
                    _toastMessage.emit(R.string.upload_success)
                    loadImages()
                }
                .onFailure {
                    _toastMessage.emit(R.string.upload_failed)
                    loadImages()
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

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GalleryViewModel() as T
        }
    }

    companion object {
        const val BUCKET = "backgrounds"
    }
}
