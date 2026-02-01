package com.example.cocktailbar

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.cocktailbar.databinding.ActivityTemplateVisualEditorBinding
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

        setPreviewAspectRatio()
    }

    private fun setPreviewAspectRatio() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        (binding.previewCard.layoutParams as? ConstraintLayout.LayoutParams)?.let {
            it.dimensionRatio = "W,$screenWidth:$screenHeight"
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
                            drinksNameFontSize = template.drinksNameFontSize
                            drinksPriceFontSize = template.drinksPriceFontSize
                            drinksDescriptionFontSize = template.drinksDescriptionFontSize
                            drinksNameColor = Color.parseColor(template.drinksNameColor)
                            drinksPriceColor = Color.parseColor(template.drinksPriceColor)
                            drinksDescriptionColor = Color.parseColor(template.drinksDescriptionColor)
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
        dialog.setContentView(R.layout.dialog_drinks_settings)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val seekbarNameFontSize = dialog.findViewById<SeekBar>(R.id.seekbarNameFontSize)
        val tvNameFontSizeValue = dialog.findViewById<TextView>(R.id.tvNameFontSizeValue)
        val nameColorContainer = dialog.findViewById<LinearLayout>(R.id.nameColorContainer)

        val seekbarPriceFontSize = dialog.findViewById<SeekBar>(R.id.seekbarPriceFontSize)
        val tvPriceFontSizeValue = dialog.findViewById<TextView>(R.id.tvPriceFontSizeValue)
        val priceColorContainer = dialog.findViewById<LinearLayout>(R.id.priceColorContainer)

        val seekbarDescFontSize = dialog.findViewById<SeekBar>(R.id.seekbarDescFontSize)
        val tvDescFontSizeValue = dialog.findViewById<TextView>(R.id.tvDescFontSizeValue)
        val descColorContainer = dialog.findViewById<LinearLayout>(R.id.descColorContainer)

        val btnClose = dialog.findViewById<Button>(R.id.btnClose)

        // Available colors
        val colors = listOf(
            "#FFFFFF", "#000000", "#FF6B35", "#FFA366",
            "#CCCCCC", "#888888", "#FFD700", "#90EE90"
        )

        // Set current values
        seekbarNameFontSize.progress = (binding.previewView.drinksNameFontSize - 10).toInt()
        tvNameFontSizeValue.text = "${binding.previewView.drinksNameFontSize.toInt()}sp"

        seekbarPriceFontSize.progress = (binding.previewView.drinksPriceFontSize - 10).toInt()
        tvPriceFontSizeValue.text = "${binding.previewView.drinksPriceFontSize.toInt()}sp"

        seekbarDescFontSize.progress = (binding.previewView.drinksDescriptionFontSize - 10).toInt()
        tvDescFontSizeValue.text = "${binding.previewView.drinksDescriptionFontSize.toInt()}sp"

        // Setup color buttons
        fun setupColorButtons(container: LinearLayout, currentColor: Int, onColorSelected: (Int) -> Unit) {
            container.removeAllViews()
            colors.forEach { colorHex ->
                val color = Color.parseColor(colorHex)
                val button = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(40.dp, 40.dp).apply {
                        marginEnd = 8.dp
                    }
                    background = createColorButtonBackground(color, color == currentColor)
                    setOnClickListener {
                        onColorSelected(color)
                        // Update selection state
                        for (i in 0 until container.childCount) {
                            val child = container.getChildAt(i)
                            val childColor = Color.parseColor(colors[i])
                            child.background = createColorButtonBackground(childColor, childColor == color)
                        }
                    }
                }
                container.addView(button)
            }
        }

        setupColorButtons(nameColorContainer, binding.previewView.drinksNameColor) { color ->
            binding.previewView.drinksNameColor = color
            binding.previewView.invalidate()
        }

        setupColorButtons(priceColorContainer, binding.previewView.drinksPriceColor) { color ->
            binding.previewView.drinksPriceColor = color
            binding.previewView.invalidate()
        }

        setupColorButtons(descColorContainer, binding.previewView.drinksDescriptionColor) { color ->
            binding.previewView.drinksDescriptionColor = color
            binding.previewView.invalidate()
        }

        // Font size listeners
        seekbarNameFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fontSize = progress + 10f
                tvNameFontSizeValue.text = "${fontSize.toInt()}sp"
                binding.previewView.drinksNameFontSize = fontSize
                binding.previewView.invalidate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekbarPriceFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fontSize = progress + 10f
                tvPriceFontSizeValue.text = "${fontSize.toInt()}sp"
                binding.previewView.drinksPriceFontSize = fontSize
                binding.previewView.invalidate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekbarDescFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fontSize = progress + 10f
                tvDescFontSizeValue.text = "${fontSize.toInt()}sp"
                binding.previewView.drinksDescriptionFontSize = fontSize
                binding.previewView.invalidate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()

        val window = dialog.window
        if (window != null) {
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun createColorButtonBackground(color: Int, isSelected: Boolean): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = 8f
        drawable.setColor(color)
        if (isSelected) {
            drawable.setStroke(4, getColor(R.color.orange_primary))
        } else {
            drawable.setStroke(2, Color.GRAY)
        }
        return drawable
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

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
                    drinksNameFontSize = preview.drinksNameFontSize,
                    drinksPriceFontSize = preview.drinksPriceFontSize,
                    drinksDescriptionFontSize = preview.drinksDescriptionFontSize,
                    drinksNameColor = String.format("#%06X", 0xFFFFFF and preview.drinksNameColor),
                    drinksPriceColor = String.format("#%06X", 0xFFFFFF and preview.drinksPriceColor),
                    drinksDescriptionColor = String.format("#%06X", 0xFFFFFF and preview.drinksDescriptionColor)
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