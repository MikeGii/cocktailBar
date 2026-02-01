package com.example.cocktailbar

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cocktailbar.databinding.ActivityGalleryBinding
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.util.UUID

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter

    private var currentBucket = BUCKET_BACKGROUNDS
    private var images: MutableList<GalleryImage> = mutableListOf()

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
        loadImages()
    }

    private fun setupRecyclerView() {
        adapter = GalleryAdapter(
            images = images,
            onDeleteClick = { image -> showDeleteConfirmation(image) }
        )
        binding.rvImages.layoutManager = GridLayoutManager(this, 2)
        binding.rvImages.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabBackgrounds.setOnClickListener {
            if (currentBucket != BUCKET_BACKGROUNDS) {
                currentBucket = BUCKET_BACKGROUNDS
                updateTabSelection()
                loadImages()
            }
        }

        binding.tabLogos.setOnClickListener {
            if (currentBucket != BUCKET_LOGOS) {
                currentBucket = BUCKET_LOGOS
                updateTabSelection()
                loadImages()
            }
        }

        updateTabSelection()
    }

    private fun updateTabSelection() {
        val isBackgrounds = currentBucket == BUCKET_BACKGROUNDS

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

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        binding.fabAdd.setOnClickListener {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        imagePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.select_image)))
    }

    private fun loadImages() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val bucket = SupabaseClient.client.storage.from(currentBucket)
                val files = bucket.list()

                images.clear()
                images.addAll(files
                    .filter { it.name.isNotEmpty() && !it.name.startsWith(".") }
                    .map { file ->
                        val publicUrl = bucket.publicUrl(file.name)
                        GalleryImage(
                            name = file.name,
                            url = publicUrl,
                            bucket = currentBucket
                        )
                    }
                )

                runOnUiThread {
                    adapter.updateImages(images)
                    showLoading(false)
                    updateEmptyState()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showLoading(false)
                    updateEmptyState()
                    Toast.makeText(this@GalleryActivity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadImage(uri: Uri) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw Exception("Cannot read file")

                val bytes = inputStream.readBytes()
                inputStream.close()

                // Generate unique filename
                val extension = getFileExtension(uri) ?: "jpg"
                val fileName = "${UUID.randomUUID()}.$extension"

                val bucket = SupabaseClient.client.storage.from(currentBucket)
                bucket.upload(fileName, bytes)

                runOnUiThread {
                    Toast.makeText(this@GalleryActivity, R.string.upload_success, Toast.LENGTH_SHORT).show()
                    loadImages()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@GalleryActivity, R.string.upload_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileExtension(uri: Uri): String? {
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
            .setPositiveButton(R.string.yes) { _, _ -> deleteImage(image) }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun deleteImage(image: GalleryImage) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val bucket = SupabaseClient.client.storage.from(image.bucket)
                bucket.delete(image.name)

                runOnUiThread {
                    Toast.makeText(this@GalleryActivity, R.string.delete, Toast.LENGTH_SHORT).show()
                    loadImages()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@GalleryActivity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvImages.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState() {
        val isEmpty = adapter.itemCount == 0
        binding.tvEmpty.visibility = if (isEmpty && binding.progressBar.visibility != View.VISIBLE)
            View.VISIBLE else View.GONE
    }

    companion object {
        const val BUCKET_BACKGROUNDS = "backgrounds"
        const val BUCKET_LOGOS = "logos"
    }
}