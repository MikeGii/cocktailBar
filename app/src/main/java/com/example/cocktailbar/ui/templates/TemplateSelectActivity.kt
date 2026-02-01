package com.example.cocktailbar.ui.templates

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.cocktailbar.R
import com.example.cocktailbar.SupabaseClient
import com.example.cocktailbar.data.model.Drink
import com.example.cocktailbar.data.model.DrinkVariant
import com.example.cocktailbar.data.model.Template
import com.example.cocktailbar.data.model.TemplateDrink
import com.example.cocktailbar.databinding.ActivityTemplateSelectBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TemplateSelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTemplateSelectBinding
    private lateinit var adapter: TemplateSelectAdapter
    private var templates: MutableList<Template> = mutableListOf()

    private val imageCache = mutableMapOf<String, Bitmap?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTemplateSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupButtons()
        loadTemplates()
    }

    private fun setupRecyclerView() {
        adapter = TemplateSelectAdapter(
            templates = templates,
            onTemplateClick = { template -> openTemplateDisplay(template) },
            coroutineScope = lifecycleScope,
            imageLoader = { url -> loadImage(url) }
        )
        binding.rvTemplates.layoutManager = LinearLayoutManager(this)
        binding.rvTemplates.adapter = adapter
    }

    private suspend fun loadImage(url: String): Bitmap? {
        imageCache[url]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(this@TemplateSelectActivity)
                val request = ImageRequest.Builder(this@TemplateSelectActivity)
                    .data(url)
                    .allowHardware(false)
                    .build()

                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap

                imageCache[url] = bitmap
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun loadTemplates() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val templatesResult = SupabaseClient.client
                    .from("templates")
                    .select()
                    .decodeList<Template>()

                val allTemplateDrinks = SupabaseClient.client
                    .from("template_drinks")
                    .select()
                    .decodeList<TemplateDrink>()

                val allDrinks = SupabaseClient.client
                    .from("drinks")
                    .select()
                    .decodeList<Drink>()

                val allVariants = SupabaseClient.client
                    .from("drink_variants")
                    .select()
                    .decodeList<DrinkVariant>()

                val variantsByDrink = allVariants.groupBy { it.drinkId }

                val drinksWithVariants = allDrinks.map { drink ->
                    drink.copy().apply {
                        variants = variantsByDrink[drink.id]?.sortedBy { it.sortOrder ?: 0 } ?: emptyList()
                    }
                }

                val drinksByTemplate = allTemplateDrinks.groupBy { it.templateId }

                val templatesWithDrinks = templatesResult
                    .filter { it.isActive }
                    .map { template ->
                        val templateDrinkIds = drinksByTemplate[template.id]
                            ?.sortedBy { it.sortOrder ?: 0 }
                            ?.map { it.drinkId } ?: emptyList()

                        template.copy().apply {
                            drinks = drinksWithVariants.filter { templateDrinkIds.contains(it.id) }
                                .sortedBy { drink -> templateDrinkIds.indexOf(drink.id) }
                        }
                    }

                templates.clear()
                templates.addAll(templatesWithDrinks)

                runOnUiThread {
                    adapter.updateTemplates(templates)
                    showLoading(false)
                    updateEmptyState()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showLoading(false)
                    updateEmptyState()
                    Toast.makeText(this@TemplateSelectActivity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openTemplateDisplay(template: Template) {
        val intent = Intent(this, TemplateDisplayActivity::class.java)
        intent.putExtra(TemplateDisplayActivity.EXTRA_TEMPLATE_ID, template.id)
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvTemplates.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState() {
        val isEmpty = adapter.itemCount == 0
        binding.tvEmpty.visibility = if (isEmpty && binding.progressBar.visibility != View.VISIBLE)
            View.VISIBLE else View.GONE
    }
}