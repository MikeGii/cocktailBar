package com.example.cocktailbar.ui.gallery

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter

    private val viewModel: GalleryViewModel by viewModels { GalleryViewModel.Factory() }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadImage(uri)
            }
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
        setupTabs()
        setupButtons()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = GalleryAdapter(
            images = emptyList(),
            onDeleteClick = { image -> showDeleteConfirmation(image) }
        )
        binding.rvImages.layoutManager = GridLayoutManager(this, 2)
        binding.rvImages.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabBackgrounds.setOnClickListener {
            viewModel.switchBucket(GalleryViewModel.BUCKET_BACKGROUNDS)
        }

        binding.tabLogos.setOnClickListener {
            viewModel.switchBucket(GalleryViewModel.BUCKET_LOGOS)
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        binding.fabAdd.setOnClickListener {
            openImagePicker()
        }
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
                    viewModel.currentBucket.collect { bucket ->
                        updateTabSelection(bucket == GalleryViewModel.BUCKET_BACKGROUNDS)
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

    private fun updateTabSelection(isBackgrounds: Boolean) {
        binding.tabBackgrounds.setBackgroundResource(
            if (isBackgrounds) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected
        )
        binding.tabBackgrounds.setTextColor(
            getColor(if (isBackgrounds) R.color.text_on_orange else R.color.orange_primary)
        )

        binding.tabLogos.setBackgroundResource(
            if (!isBackgrounds) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected
        )
        binding.tabLogos.setTextColor(
            getColor(if (!isBackgrounds) R.color.text_on_orange else R.color.orange_primary)
        )

        binding.tvTitle.text = getString(
            if (isBackgrounds) R.string.backgrounds else R.string.logos
        )
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
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw Exception("Cannot read file")

            val bytes = inputStream.readBytes()
            inputStream.close()

            val extension = getFileExtension(uri)
            val fileName = "${UUID.randomUUID()}.$extension"

            viewModel.uploadImage(fileName, bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, R.string.upload_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileExtension(uri: Uri): String {
        val mimeType = contentResolver.getType(uri)
        return when (mimeType) {
            "image/jpeg" -> "jpg"
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
        binding.tvEmpty.visibility = if (isEmpty && binding.progressBar.visibility != View.VISIBLE)
            View.VISIBLE else View.GONE
    }
}