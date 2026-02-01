package com.example.cocktailbar

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.cocktailbar.data.model.Drink
import com.example.cocktailbar.data.model.DrinkVariant
import com.example.cocktailbar.data.model.Template
import com.example.cocktailbar.data.model.TemplateDrink
import com.example.cocktailbar.data.model.TemplateDrinkRequest
import com.example.cocktailbar.data.model.TemplateRequest
import com.example.cocktailbar.databinding.ActivityTemplateVisualEditorBinding
import com.example.cocktailbar.dialog.DrinksSelectionDialog
import com.example.cocktailbar.dialog.DrinksSettingsDialog
import com.example.cocktailbar.dialog.ImagePickerDialog
import com.example.cocktailbar.util.ImageLoaderHelper
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class TemplateVisualEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTemplateVisualEditorBinding

    private var templateId: String? = null
    private var selectedBackgroundUrl: String? = null
    private var selectedLogoUrl: String? = null
    private var allDrinks: List<Drink> = emptyList()
    private var selectedDrinkIds: MutableSet<String> = mutableSetOf()

    // Dialogs
    private val imagePickerDialog by lazy { ImagePickerDialog(this, lifecycleScope) }
    private val drinksSelectionDialog by lazy { DrinksSelectionDialog(this) }
    private val drinksSettingsDialog by lazy { DrinksSettingsDialog(this, binding.previewView) }

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
        binding.previewView.onLayoutChanged = { /* Layout changed callback */ }

        setPreviewAspectRatio()
    }

    private fun setPreviewAspectRatio() {
        val displayMetrics = resources.displayMetrics
        (binding.previewCard.layoutParams as? ConstraintLayout.LayoutParams)?.let {
            it.dimensionRatio = "W,${displayMetrics.widthPixels}:${displayMetrics.heightPixels}"
            binding.previewCard.layoutParams = it
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
            imagePickerDialog.show(GalleryActivity.BUCKET_BACKGROUNDS) { url ->
                selectedBackgroundUrl = url
                loadBackgroundImage(url)
            }
        }

        binding.btnSelectLogo.setOnClickListener {
            imagePickerDialog.show(GalleryActivity.BUCKET_LOGOS) { url ->
                selectedLogoUrl = url
                loadLogoImage(url)
            }
        }

        binding.btnSelectDrinks.setOnClickListener {
            drinksSelectionDialog.show(allDrinks, selectedDrinkIds) { newSelection ->
                selectedDrinkIds.clear()
                selectedDrinkIds.addAll(newSelection)
                updateSelectedDrinks()
            }
        }

        binding.btnDrinksSettings.setOnClickListener {
            drinksSettingsDialog.show()
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
                loadDrinksWithVariants()
                templateId?.let { loadTemplateData(it) }

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

    private suspend fun loadDrinksWithVariants() {
        val drinksResult = SupabaseClient.client.from("drinks").select().decodeList<Drink>()
        val variantsResult = SupabaseClient.client.from("drink_variants").select().decodeList<DrinkVariant>()
        val variantsByDrink = variantsResult.groupBy { it.drinkId }

        allDrinks = drinksResult.map { drink ->
            drink.copy().apply {
                variants = variantsByDrink[drink.id]?.sortedBy { it.sortOrder ?: 0 } ?: emptyList()
            }
        }
    }

    private suspend fun loadTemplateData(id: String) {
        val template = SupabaseClient.client
            .from("templates")
            .select { filter { eq("id", id) } }
            .decodeSingle<Template>()

        val templateDrinks = SupabaseClient.client
            .from("template_drinks")
            .select { filter { eq("template_id", id) } }
            .decodeList<TemplateDrink>()

        selectedBackgroundUrl = template.backgroundUrl
        selectedLogoUrl = template.logoUrl
        selectedDrinkIds.addAll(templateDrinks.map { it.drinkId })

        runOnUiThread {
            binding.etTemplateName.setText(template.name)
            applyTemplateToPreview(template)
            template.backgroundUrl?.let { loadBackgroundImage(it) }
            template.logoUrl?.let { loadLogoImage(it) }
        }
    }

    private fun applyTemplateToPreview(template: Template) {
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
            drinksNameFontSize = template.drinksNameFontSize
            drinksPriceFontSize = template.drinksPriceFontSize
            drinksDescriptionFontSize = template.drinksDescriptionFontSize
            drinksNameColor = Color.parseColor(template.drinksNameColor)
            drinksPriceColor = Color.parseColor(template.drinksPriceColor)
            drinksDescriptionColor = Color.parseColor(template.drinksDescriptionColor)
            drinksFont = template.drinksFont
        }
    }

    private fun loadBackgroundImage(url: String) {
        lifecycleScope.launch {
            val bitmap = ImageLoaderHelper.loadBitmap(this@TemplateVisualEditorActivity, url)
            runOnUiThread { binding.previewView.setBackgroundBitmap(bitmap) }
        }
    }

    private fun loadLogoImage(url: String) {
        lifecycleScope.launch {
            val bitmap = ImageLoaderHelper.loadBitmap(this@TemplateVisualEditorActivity, url)
            runOnUiThread { binding.previewView.setLogoBitmap(bitmap) }
        }
    }

    private fun updateSelectedDrinks() {
        val selectedDrinks = allDrinks.filter { selectedDrinkIds.contains(it.id) }
        binding.previewView.drinks = selectedDrinks
        binding.tvSelectedDrinksCount.text = getString(R.string.drinks_selected_count, selectedDrinks.size)
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
                val savedTemplateId = saveTemplateToDatabase(name)
                saveDrinkAssociations(savedTemplateId)

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

    private suspend fun saveTemplateToDatabase(name: String): String {
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
            drinksNameFontSize = preview.drinksNameFontSize,
            drinksPriceFontSize = preview.drinksPriceFontSize,
            drinksDescriptionFontSize = preview.drinksDescriptionFontSize,
            drinksNameColor = String.format("#%06X", 0xFFFFFF and preview.drinksNameColor),
            drinksPriceColor = String.format("#%06X", 0xFFFFFF and preview.drinksPriceColor),
            drinksDescriptionColor = String.format(
                "#%06X",
                0xFFFFFF and preview.drinksDescriptionColor
            ),
            drinksFont = preview.drinksFont
        )

        return if (templateId != null) {
            SupabaseClient.client.from("templates")
                .update(templateRequest) { filter { eq("id", templateId!!) } }
            templateId!!
        } else {
            val result = SupabaseClient.client.from("templates")
                .insert(templateRequest) { select() }
                .decodeSingle<Template>()
            result.id
        }
    }

    private suspend fun saveDrinkAssociations(templateId: String) {
        // Delete old associations
        SupabaseClient.client.from("template_drinks")
            .delete { filter { eq("template_id", templateId) } }

        // Insert new associations
        val drinkAssociations = selectedDrinkIds.mapIndexed { index, drinkId ->
            TemplateDrinkRequest(templateId = templateId, drinkId = drinkId, sortOrder = index)
        }

        if (drinkAssociations.isNotEmpty()) {
            SupabaseClient.client.from("template_drinks").insert(drinkAssociations)
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