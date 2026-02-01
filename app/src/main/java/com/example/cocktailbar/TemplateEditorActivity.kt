package com.example.cocktailbar

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.cocktailbar.data.model.Drink
import com.example.cocktailbar.data.model.DrinkVariant
import com.example.cocktailbar.data.model.Template
import com.example.cocktailbar.data.model.TemplateDrink
import com.example.cocktailbar.data.model.TemplateDrinkRequest
import com.example.cocktailbar.data.model.TemplateRequest
import com.example.cocktailbar.databinding.ActivityTemplateEditorBinding
import com.example.cocktailbar.databinding.DialogImagePickerBinding
import com.example.cocktailbar.ui.gallery.GalleryViewModel
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch

class TemplateEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTemplateEditorBinding
    private lateinit var drinksAdapter: SelectableDrinksAdapter

    private var templateId: String? = null
    private var selectedBackgroundUrl: String? = null
    private var selectedLogoUrl: String? = null
    private var allDrinks: List<Drink> = emptyList()
    private var selectedDrinkIds: MutableSet<String> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTemplateEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        templateId = intent.getStringExtra(EXTRA_TEMPLATE_ID)

        setupViews()
        setupDrinksRecyclerView()
        loadData()
    }

    private fun setupViews() {
        binding.tvTitle.text = getString(
            if (templateId == null) R.string.add_template else R.string.edit_template
        )

        binding.btnBack.setOnClickListener { finish() }

        binding.cardBackground.setOnClickListener {
            showImagePickerDialog(GalleryViewModel.BUCKET_BACKGROUNDS) { url ->
                selectedBackgroundUrl = url
                updateBackgroundPreview()
            }
        }

        binding.cardLogo.setOnClickListener {
            showImagePickerDialog(GalleryViewModel.BUCKET_LOGOS) { url ->
                selectedLogoUrl = url
                updateLogoPreview()
            }
        }

        binding.btnSave.setOnClickListener {
            saveTemplate()
        }
    }

    private fun setupDrinksRecyclerView() {
        drinksAdapter = SelectableDrinksAdapter(
            drinks = emptyList(),
            selectedIds = selectedDrinkIds,
            onSelectionChanged = { drinkId, isSelected ->
                if (isSelected) {
                    selectedDrinkIds.add(drinkId)
                } else {
                    selectedDrinkIds.remove(drinkId)
                }
                updateSelectedCount()
            }
        )
        binding.rvDrinks.layoutManager = LinearLayoutManager(this)
        binding.rvDrinks.adapter = drinksAdapter
    }

    private fun loadData() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Load all drinks with variants
                val drinksResult = SupabaseClient.client
                    .from("drinks")
                    .select()
                    .decodeList<Drink>()

                val variantsResult = SupabaseClient.client
                    .from("drink_variants")
                    .select()
                    .decodeList<DrinkVariant>()

                val variantsByDrink = variantsResult.groupBy { it.drinkId }

                allDrinks = drinksResult.map { drink ->
                    drink.copy().apply {
                        variants = variantsByDrink[drink.id]?.sortedBy { it.sortOrder ?: 0 } ?: emptyList()
                    }
                }

                // If editing, load template data
                templateId?.let { id ->
                    val template = SupabaseClient.client
                        .from("templates")
                        .select {
                            filter { eq("id", id) }
                        }
                        .decodeSingle<Template>()

                    val templateDrinks = SupabaseClient.client
                        .from("template_drinks")
                        .select {
                            filter { eq("template_id", id) }
                        }
                        .decodeList<TemplateDrink>()

                    selectedBackgroundUrl = template.backgroundUrl
                    selectedLogoUrl = template.logoUrl
                    selectedDrinkIds.addAll(templateDrinks.map { it.drinkId })

                    runOnUiThread {
                        binding.etTemplateName.setText(template.name)
                        updateBackgroundPreview()
                        updateLogoPreview()
                    }
                }

                runOnUiThread {
                    drinksAdapter.updateDrinks(allDrinks, selectedDrinkIds)
                    updateSelectedCount()
                    showLoading(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@TemplateEditorActivity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateBackgroundPreview() {
        if (!selectedBackgroundUrl.isNullOrEmpty()) {
            binding.ivBackgroundPreview.load(selectedBackgroundUrl) {
                crossfade(true)
                transformations(RoundedCornersTransformation(24f))
            }
            binding.tvBackgroundHint.visibility = View.GONE
        } else {
            binding.ivBackgroundPreview.setImageDrawable(null)
            binding.tvBackgroundHint.visibility = View.VISIBLE
        }
    }

    private fun updateLogoPreview() {
        if (!selectedLogoUrl.isNullOrEmpty()) {
            binding.ivLogoPreview.load(selectedLogoUrl) {
                crossfade(true)
                transformations(RoundedCornersTransformation(24f))
            }
            binding.tvLogoHint.visibility = View.GONE
        } else {
            binding.ivLogoPreview.setImageDrawable(null)
            binding.tvLogoHint.visibility = View.VISIBLE
        }
    }

    private fun updateSelectedCount() {
        binding.tvDrinksHeader.text = getString(R.string.select_drinks_count, selectedDrinkIds.size)
    }

    private fun showImagePickerDialog(bucket: String, onSelected: (String) -> Unit) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding = DialogImagePickerBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.tvTitle.text = getString(
            if (bucket == GalleryViewModel.BUCKET_BACKGROUNDS) R.string.select_background
            else R.string.select_logo
        )

        val imageAdapter = ImagePickerAdapter { imageUrl ->
            onSelected(imageUrl)
            dialog.dismiss()
        }

        dialogBinding.rvImages.layoutManager = GridLayoutManager(this, 2)
        dialogBinding.rvImages.adapter = imageAdapter

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        // Load images from bucket
        lifecycleScope.launch {
            try {
                val storageBucket = SupabaseClient.client.storage.from(bucket)
                val files = storageBucket.list()

                val images = files
                    .filter { it.name.isNotEmpty() && !it.name.startsWith(".") }
                    .map { file -> storageBucket.publicUrl(file.name) }

                runOnUiThread {
                    dialogBinding.progressBar.visibility = View.GONE
                    if (images.isEmpty()) {
                        dialogBinding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        imageAdapter.updateImages(images)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    dialogBinding.progressBar.visibility = View.GONE
                    dialogBinding.tvEmpty.visibility = View.VISIBLE
                }
            }
        }

        dialog.show()

        val window = dialog.window
        if (window != null) {
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            val height = (displayMetrics.heightPixels * 0.70).toInt()
            window.setLayout(width, height)
        }
    }

    private fun saveTemplate() {
        val name = binding.etTemplateName.text.toString().trim()

        if (name.isEmpty()) {
            binding.etTemplateName.error = getString(R.string.field_required)
            return
        }

        if (selectedDrinkIds.isEmpty()) {
            Toast.makeText(this, R.string.select_at_least_one_drink, Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val templateRequest = TemplateRequest(
                    name = name,
                    backgroundUrl = selectedBackgroundUrl,
                    logoUrl = selectedLogoUrl
                )

                val savedTemplateId: String

                if (templateId != null) {
                    // Update existing template
                    SupabaseClient.client
                        .from("templates")
                        .update(templateRequest) {
                            filter { eq("id", templateId!!) }
                        }
                    savedTemplateId = templateId!!

                    // Delete old drink associations
                    SupabaseClient.client
                        .from("template_drinks")
                        .delete {
                            filter { eq("template_id", savedTemplateId) }
                        }
                } else {
                    // Insert new template
                    val result = SupabaseClient.client
                        .from("templates")
                        .insert(templateRequest) { select() }
                        .decodeSingle<Template>()
                    savedTemplateId = result.id
                }

                // Insert drink associations
                val drinkAssociations = selectedDrinkIds.mapIndexed { index, drinkId ->
                    TemplateDrinkRequest(
                        templateId = savedTemplateId,
                        drinkId = drinkId,
                        sortOrder = index
                    )
                }

                if (drinkAssociations.isNotEmpty()) {
                    SupabaseClient.client
                        .from("template_drinks")
                        .insert(drinkAssociations)
                }

                runOnUiThread {
                    Toast.makeText(this@TemplateEditorActivity, R.string.save, Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@TemplateEditorActivity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !show
    }

    companion object {
        const val EXTRA_TEMPLATE_ID = "template_id"
    }
}