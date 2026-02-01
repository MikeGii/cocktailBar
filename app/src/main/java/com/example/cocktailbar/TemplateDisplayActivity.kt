package com.example.cocktailbar

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.cocktailbar.databinding.ActivityTemplateDisplayBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import android.graphics.Color
import com.example.cocktailbar.data.model.Drink
import com.example.cocktailbar.data.model.DrinkVariant
import com.example.cocktailbar.data.model.Template
import com.example.cocktailbar.data.model.TemplateDrink

class TemplateDisplayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTemplateDisplayBinding

    private var templateId: String? = null
    private var template: Template? = null
    private var isLocked = false

    // Triple tap detection
    private var tapCount = 0
    private var lastTapTime = 0L
    private val tapTimeout = 500L // ms between taps
    private val handler = Handler(Looper.getMainLooper())

    // Auto-hide unlock button
    private val hideUnlockRunnable = Runnable {
        if (isLocked) {
            binding.btnUnlock.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTemplateDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        templateId = intent.getStringExtra(EXTRA_TEMPLATE_ID)

        if (templateId == null) {
            finish()
            return
        }

        setupViews()
        loadTemplate()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener {
            if (!isLocked) finish()
        }

        binding.btnLock.setOnClickListener {
            lockScreen()
        }

        binding.btnUnlock.setOnClickListener {
            unlockScreen()
        }

        // Triple tap detection on the preview
        binding.previewView.onTapInViewMode = {
            if (isLocked) {
                detectTripleTap()
            }
        }
    }

    private fun detectTripleTap() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastTapTime < tapTimeout) {
            tapCount++
        } else {
            tapCount = 1
        }

        lastTapTime = currentTime

        if (tapCount >= 3) {
            tapCount = 0
            showUnlockButton()
        }
    }

    private fun showUnlockButton() {
        binding.btnUnlock.visibility = View.VISIBLE
        binding.btnUnlock.alpha = 0f
        binding.btnUnlock.animate()
            .alpha(0.7f)
            .setDuration(200)
            .start()

        // Auto-hide after 3 seconds
        handler.removeCallbacks(hideUnlockRunnable)
        handler.postDelayed(hideUnlockRunnable, 3000)
    }

    private fun lockScreen() {
        isLocked = true

        // Hide UI elements
        binding.topBar.visibility = View.GONE
        binding.btnLock.visibility = View.GONE
        binding.btnUnlock.visibility = View.GONE

        // Enter fullscreen immersive mode
        enterFullscreen()

        Toast.makeText(this, R.string.screen_locked, Toast.LENGTH_SHORT).show()
    }

    private fun unlockScreen() {
        isLocked = false

        // Show UI elements
        binding.topBar.visibility = View.VISIBLE
        binding.btnLock.visibility = View.VISIBLE
        binding.btnUnlock.visibility = View.GONE

        // Exit fullscreen
        exitFullscreen()

        handler.removeCallbacks(hideUnlockRunnable)
    }

    private fun enterFullscreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
        }
    }

    private fun exitFullscreen() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun loadTemplate() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Load template
                val loadedTemplate = SupabaseClient.client
                    .from("templates")
                    .select {
                        filter { eq("id", templateId!!) }
                    }
                    .decodeSingle<Template>()

                // Load template drinks
                val templateDrinks = SupabaseClient.client
                    .from("template_drinks")
                    .select {
                        filter { eq("template_id", templateId!!) }
                    }
                    .decodeList<TemplateDrink>()

                val drinkIds = templateDrinks.map { it.drinkId }

                // Load drinks
                val drinks = if (drinkIds.isNotEmpty()) {
                    val drinksResult = SupabaseClient.client
                        .from("drinks")
                        .select()
                        .decodeList<Drink>()

                    val variantsResult = SupabaseClient.client
                        .from("drink_variants")
                        .select()
                        .decodeList<DrinkVariant>()

                    val variantsByDrink = variantsResult.groupBy { it.drinkId }

                    drinksResult
                        .filter { drinkIds.contains(it.id) }
                        .map { drink ->
                            drink.copy().apply {
                                variants = variantsByDrink[drink.id]?.sortedBy { it.sortOrder ?: 0 } ?: emptyList()
                            }
                        }
                        .sortedBy { drink ->
                            templateDrinks.find { it.drinkId == drink.id }?.sortOrder ?: 0
                        }
                } else {
                    emptyList()
                }

                template = loadedTemplate.copy().apply {
                    this.drinks = drinks
                }

                runOnUiThread {
                    applyTemplate()
                    showLoading(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@TemplateDisplayActivity, R.string.error, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun applyTemplate() {
        template?.let { tmpl ->
            binding.tvTitle.text = tmpl.name

            // Set preview view properties
            binding.previewView.apply {
                backgroundScale = tmpl.backgroundScale
                backgroundOffsetX = tmpl.backgroundOffsetX
                backgroundOffsetY = tmpl.backgroundOffsetY
                logoX = tmpl.logoX
                logoY = tmpl.logoY
                logoScale = tmpl.logoScale
                drinksX = tmpl.drinksX
                drinksY = tmpl.drinksY
                drinksWidth = tmpl.drinksWidth
                drinksHeight = tmpl.drinksHeight
                drinksNameFontSize = tmpl.drinksNameFontSize
                drinksPriceFontSize = tmpl.drinksPriceFontSize
                drinksDescriptionFontSize = tmpl.drinksDescriptionFontSize
                drinksNameColor = Color.parseColor(tmpl.drinksNameColor)
                drinksPriceColor = Color.parseColor(tmpl.drinksPriceColor)
                drinksDescriptionColor = Color.parseColor(tmpl.drinksDescriptionColor)
                drinksFont = tmpl.drinksFont  // Add this line
                drinks = tmpl.drinks
                editMode = TemplatePreviewView.EditMode.NONE
            }

            // Load images
            tmpl.backgroundUrl?.let { loadBackgroundImage(it) }
            tmpl.logoUrl?.let { loadLogoImage(it) }
        }
    }

    private fun loadBackgroundImage(url: String) {
        lifecycleScope.launch {
            try {
                val loader = ImageLoader(this@TemplateDisplayActivity)
                val request = ImageRequest.Builder(this@TemplateDisplayActivity)
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
                val loader = ImageLoader(this@TemplateDisplayActivity)
                val request = ImageRequest.Builder(this@TemplateDisplayActivity)
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

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onBackPressed() {
        if (isLocked) {
            // Do nothing when locked
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(hideUnlockRunnable)
    }

    companion object {
        const val EXTRA_TEMPLATE_ID = "template_id"
    }
}