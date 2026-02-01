package com.example.cocktailbar

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.cocktailbar.databinding.ActivityTemplateVisualEditorBinding
import com.example.cocktailbar.databinding.DialogDrinksSettingsBinding
import com.example.cocktailbar.databinding.DialogImagePickerBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch

class TemplateVisualEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTemplateVisualEditorBinding

    private var templateId: String? = null
    private var templateName: String = ""
    private var selectedBackgroundUrl: String? = null
    private var selectedLogoUrl: String? = null
    private var allDrinks: List<Drink> = emptyList()
    private var selectedDrinks: List<Drink> = emptyList()
    private var selectedDrinkIds: MutableSet<String> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTemplateVisualEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        templateId = intent.getStringExtra(EXTRA_TEMPLATE_ID)

        setupViews()
        setupEditModeButtons()
        loadData()
    }

    private fun setupViews() {
        binding.tvTitle.text = getString(
            if (templateId == null) R.string.add_template else R.string.edit_template
        )

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveTemplate() }

        binding.previewView.onLayoutChanged = {
            // Layout changed, can update UI if needed
        }
    }

    private fun setupEditModeButtons() {
        binding.btnEditBackground.setOnClickListener {
            setEditMode(TemplatePreviewView.EditMode.BACKGROUND)
        }

        binding.btnEditLogo.setOnClickListener {
            setEditMode(TemplatePreviewView.EditMode.LOGO)
        }

        binding.btnEditDrinks.setOnClickListener {
            setEditMode(TemplatePreviewView.EditMode.DRINKS)
        }

        binding.btnSelectBackground.setOnClickListener {
            showImagePickerDialog(GalleryActivity.BUCKET_BACKGROUNDS) { url ->
                selectedBackgroundUrl = url
                loadBackgroundImage(url)
            }
        }

        binding.btnSelectLogo.setOnClickListener {
            showImagePickerDialog(GalleryActivity.BUCKET_LOGOS) { url ->
                selectedLogoUrl = url
                loadLogoImage(url)
            }
        }

        binding.btnSelectDrinks.setOnClickListener {
            showDrinksSelectionDialog()
        }

        binding.btnDrinksSettings.setOnClickListener {
            showDrinksSettingsDialog()
        }
    }

    private fun setEditMode(mode: TemplatePreviewView.EditMode) {
        binding.previewView.editMode = mode

        // Update button states
        binding.btnEditBackground.alpha = if (mode == TemplatePreviewView.EditMode.BACKGROUND) 1f else 0.5f
        binding.btnEditLogo.alpha = if (mode == TemplatePreviewView.EditMode.LOGO) 1f else 0.5f
        binding.btnEditDrinks.alpha = if (mode == TemplatePreviewView.EditMode.DRINKS) 1f else 0.5f

        // Show relevant controls
        binding.backgroundControls.visibility =
            if (mode == TemplatePreviewView.EditMode.BACKGROUND) View.VISIBLE else View.GONE
        binding.logoControls.visibility =
            if (mode == TemplatePreviewView.EditMode.LOGO) View.VISIBLE else View.GONE
        binding.drinksControls.visibility =
            if (mode == TemplatePreviewView.EditMode.DRINKS) View.VISIBLE else View.GONE

        // Update hint text
        binding.tvEditHint.text = when (mode) {
            TemplatePreviewView.EditMode.BACKGROUND -> getString(R.string.hint_edit_background)
            TemplatePreviewView.EditMode.LOGO -> getString(R.string.hint_edit_logo)
            TemplatePreviewView.EditMode.DRINKS -> getString(R.string.hint_edit_drinks)
            else -> ""
        }
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

                    templateName = template.name
                    selectedBackgroundUrl = template.backgroundUrl
                    selectedLogoUrl = template.logoUrl
                    selectedDrinkIds.addAll(templateDrinks.map { it.drinkId })

                    runOnUiThread {
                        binding.etTemplateName.setText(template.name)

                        // Set preview view properties
                        binding.previewView.apply {
                            backgroundScale = template.backgroundScale
                            backgroundOffsetX = template.backgroundOffsetX
                            backgroundOffsetY = template.backgroundOffsetY
                            logoX = template.logoX
                            logoY = template.logoY
                            logoScale = template.logoScale
                            drinksX = template.drinksX
                            drinksY = template.drinksY
                            drinksWidth = template.drinksWidth
                            drinksHeight = template.drinksHeight
                            drinksFontSize = template.drinksFontSize
                            drinksColumns = template.drinksColumns
                        }

                        // Load images
                        template.backgroundUrl?.let { loadBackgroundImage(it) }
                        template.logoUrl?.let { loadLogoImage(it) }
                    }
                }

                runOnUiThread {
                    updateSelectedDrinks()
                    showLoading(false)
                    setEditMode(TemplatePreviewView.EditMode.NONE)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@TemplateVisualEditorActivity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadBackgroundImage(url: String) {
        lifecycleScope.launch {
            try {
                val loader = ImageLoader(this@TemplateVisualEditorActivity)
                val request = ImageRequest.Builder(this@TemplateVisualEditorActivity)
                    .data(url)
                    .allowHardware(false)
                    .build()

                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap

                runOnUiThread {
                    binding.previewView.setBackgroundBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadLogoImage(url: String) {
        lifecycleScope.launch {
            try {
                val loader = ImageLoader(this@TemplateVisualEditorActivity)
                val request = ImageRequest.Builder(this@TemplateVisualEditorActivity)
                    .data(url)
                    .allowHardware(false)
                    .build()

                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap

                runOnUiThread {
                    binding.previewView.setLogoBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateSelectedDrinks() {
        selectedDrinks = allDrinks.filter { selectedDrinkIds.contains(it.id) }
        binding.previewView.drinks = selectedDrinks
        binding.tvSelectedDrinksCount.text = getString(R.string.drinks_selected_count, selectedDrinks.size)
    }

    private fun showImagePickerDialog(bucket: String, onSelected: (String) -> Unit) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding = DialogImagePickerBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.tvTitle.text = getString(
            if (bucket == GalleryActivity.BUCKET_BACKGROUNDS) R.string.select_background
            else R.string.select_logo
        )

        val imageAdapter = ImagePickerAdapter { imageUrl ->
            onSelected(imageUrl)
            dialog.dismiss()
        }

        dialogBinding.rvImages.layoutManager = GridLayoutManager(this, 2)
        dialogBinding.rvImages.adapter = imageAdapter

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

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

    private fun showDrinksSelectionDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding = DialogImagePickerBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.tvTitle.text = getString(R.string.select_drinks)
        dialogBinding.progressBar.visibility = View.GONE

        val tempSelectedIds = selectedDrinkIds.toMutableSet()

        val drinksAdapter = SelectableDrinksAdapter(
            drinks = allDrinks,
            selectedIds = tempSelectedIds,
            onSelectionChanged = { drinkId, isSelected ->
                if (isSelected) tempSelectedIds.add(drinkId)
                else tempSelectedIds.remove(drinkId)
            }
        )

        dialogBinding.rvImages.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvImages.adapter = drinksAdapter

        dialogBinding.btnCancel.text = getString(R.string.ok)
        dialogBinding.btnCancel.setOnClickListener {
            selectedDrinkIds.clear()
            selectedDrinkIds.addAll(tempSelectedIds)
            updateSelectedDrinks()
            dialog.dismiss()
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

    private fun showDrinksSettingsDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding = DialogDrinksSettingsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set current values
        dialogBinding.seekbarFontSize.progress = (binding.previewView.drinksFontSize - 10).toInt()
        dialogBinding.tvFontSizeValue.text = "${binding.previewView.drinksFontSize.toInt()}sp"

        dialogBinding.seekbarColumns.progress = binding.previewView.drinksColumns - 1
        dialogBinding.tvColumnsValue.text = "${binding.previewView.drinksColumns}"

        dialogBinding.seekbarFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fontSize = progress + 10f
                dialogBinding.tvFontSizeValue.text = "${fontSize.toInt()}sp"
                binding.previewView.drinksFontSize = fontSize
                binding.previewView.invalidate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        dialogBinding.seekbarColumns.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val columns = progress + 1
                dialogBinding.tvColumnsValue.text = "$columns"
                binding.previewView.drinksColumns = columns
                binding.previewView.invalidate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()

        val window = dialog.window
        if (window != null) {
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
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
                val preview = binding.previewView

                val templateRequest = TemplateRequest(
                    name = name,
                    backgroundUrl = selectedBackgroundUrl,
                    logoUrl = selectedLogoUrl,
                    backgroundScale = preview.backgroundScale,
                    backgroundOffsetX = preview.backgroundOffsetX,
                    backgroundOffsetY = preview.backgroundOffsetY,
                    logoX = preview.logoX,
                    logoY = preview.logoY,
                    logoScale = preview.logoScale,
                    drinksX = preview.drinksX,
                    drinksY = preview.drinksY,
                    drinksWidth = preview.drinksWidth,
                    drinksHeight = preview.drinksHeight,
                    drinksFontSize = preview.drinksFontSize,
                    drinksColumns = preview.drinksColumns
                )

                val savedTemplateId: String

                if (templateId != null) {
                    SupabaseClient.client
                        .from("templates")
                        .update(templateRequest) {
                            filter { eq("id", templateId!!) }
                        }
                    savedTemplateId = templateId!!

                    SupabaseClient.client
                        .from("template_drinks")
                        .delete {
                            filter { eq("template_id", savedTemplateId) }
                        }
                } else {
                    val result = SupabaseClient.client
                        .from("templates")
                        .insert(templateRequest) { select() }
                        .decodeSingle<Template>()
                    savedTemplateId = result.id
                }

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
                    Toast.makeText(this@TemplateVisualEditorActivity, R.string.save, Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@TemplateVisualEditorActivity, R.string.error, Toast.LENGTH_SHORT).show()
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