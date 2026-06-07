package com.example.cocktailbar.ui.gallery

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cocktailbar.R
import com.example.cocktailbar.data.model.GalleryImage
import com.example.cocktailbar.databinding.ActivityGalleryBinding
import com.example.cocktailbar.ui.common.UiState
import com.example.cocktailbar.ui.display.DisplayActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Home screen: a grid of images synced from Supabase Storage. Tap an image to
 * display it full-screen and lock the device; use the FAB to add an image from
 * the device (including Google Drive via the system picker).
 */
class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private val viewModel: GalleryViewModel by viewModels { GalleryViewModel.Factory() }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> uploadImage(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupButtons()
        setupNetworkMonitoring()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        registerNetworkCallback()
    }

    override fun onPause() {
        super.onPause()
        unregisterNetworkCallback()
    }

    private fun setupRecyclerView() {
        adapter = GalleryAdapter(
            images = emptyList(),
            onImageClick = { image -> openDisplay(image) },
            onDeleteClick = { image -> showDeleteConfirmation(image) }
        )
        binding.rvImages.layoutManager = GridLayoutManager(this, 2)
        binding.rvImages.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnSyncData.setOnClickListener { viewModel.syncData() }
        binding.fabAdd.setOnClickListener { openImagePicker() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Loading -> showLoading(true)
                            is UiState.Success -> {
                                showLoading(false)
                                adapter.updateImages(state.data)
                                updateEmptyState(false)
                            }
                            is UiState.Empty -> {
                                showLoading(false)
                                adapter.updateImages(emptyList())
                                updateEmptyState(true)
                            }
                            is UiState.Error -> {
                                showLoading(false)
                                Toast.makeText(this@GalleryActivity, state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.isConnected.collect { connected ->
                        binding.connectionIndicator.setImageResource(
                            if (connected) R.drawable.ic_connected else R.drawable.ic_disconnected
                        )
                    }
                }

                launch {
                    viewModel.toastMessage.collectLatest { messageResId ->
                        Toast.makeText(this@GalleryActivity, messageResId, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // region Network monitoring

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                viewModel.checkConnection()
            }

            override fun onLost(network: Network) {
                viewModel.setConnected(false)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    viewModel.checkConnection()
                } else {
                    viewModel.setConnected(false)
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unregisterNetworkCallback() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // endregion

    private fun openDisplay(image: GalleryImage) {
        val intent = Intent(this, DisplayActivity::class.java).apply {
            putExtra(DisplayActivity.EXTRA_IMAGE_URL, image.url)
        }
        startActivity(intent)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        imagePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.select_image)))
    }

    private fun uploadImage(uri: Uri) {
        try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw Exception("Cannot read file")

            val extension = getFileExtension(uri)
            val fileName = "${UUID.randomUUID()}.$extension"

            viewModel.uploadImage(fileName, bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, R.string.upload_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileExtension(uri: Uri): String {
        return when (contentResolver.getType(uri)) {
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }

    private fun showDeleteConfirmation(image: GalleryImage) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_image)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.yes) { _, _ -> viewModel.deleteImage(image) }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvImages.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmpty.visibility =
            if (isEmpty && binding.progressBar.visibility != View.VISIBLE) View.VISIBLE else View.GONE
    }
}
