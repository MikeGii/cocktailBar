package com.example.cocktailbar.data.repository

import com.example.cocktailbar.data.model.*
import com.example.cocktailbar.data.remote.SupabaseDataSource

class TemplatesRepository(
    private val dataSource: SupabaseDataSource = SupabaseDataSource()
) {

    suspend fun getTemplates(): Result<List<Template>> {
        return runCatching {
            dataSource.getTemplates()
        }
    }

    suspend fun getTemplatesWithDrinks(): Result<List<Template>> {
        return runCatching {
            val templates = dataSource.getTemplates()
            val templateDrinks = dataSource.getTemplateDrinks()
            val drinks = dataSource.getDrinks()
            val variants = dataSource.getDrinkVariants()

            val variantsByDrink = variants.groupBy { it.drinkId }
            val drinksWithVariants = drinks.map { drink ->
                drink.copy().apply {
                    this.variants = variantsByDrink[drink.id]
                        ?.sortedBy { it.sortOrder ?: 0 } ?: emptyList()
                }
            }

            val drinksByTemplate = templateDrinks.groupBy { it.templateId }

            templates
                .filter { it.isActive }
                .map { template ->
                    val templateDrinkIds = drinksByTemplate[template.id]
                        ?.sortedBy { it.sortOrder ?: 0 }
                        ?.map { it.drinkId } ?: emptyList()

                    template.copy().apply {
                        this.drinks = drinksWithVariants
                            .filter { templateDrinkIds.contains(it.id) }
                            .sortedBy { drink -> templateDrinkIds.indexOf(drink.id) }
                    }
                }
        }
    }

    suspend fun getTemplateWithDrinks(templateId: String): Result<Template> {
        return runCatching {
            val template = dataSource.getTemplateById(templateId)
            val templateDrinks = dataSource.getTemplateDrinksByTemplateId(templateId)
            val drinkIds = templateDrinks.map { it.drinkId }

            if (drinkIds.isEmpty()) {
                return@runCatching template.copy().apply { drinks = emptyList() }
            }

            val allDrinks = dataSource.getDrinks()
            val variants = dataSource.getDrinkVariants()
            val variantsByDrink = variants.groupBy { it.drinkId }

            val drinks = allDrinks
                .filter { drinkIds.contains(it.id) }
                .map { drink ->
                    drink.copy().apply {
                        this.variants = variantsByDrink[drink.id]
                            ?.sortedBy { it.sortOrder ?: 0 } ?: emptyList()
                    }
                }
                .sortedBy { drink ->
                    templateDrinks.find { it.drinkId == drink.id }?.sortOrder ?: 0
                }

            template.copy().apply { this.drinks = drinks }
        }
    }

    suspend fun saveTemplate(
        id: String?,
        request: TemplateRequest,
        drinkIds: List<String>
    ): Result<String> {
        return runCatching {
            val templateId = if (id != null) {
                dataSource.updateTemplate(id, request)
                dataSource.deleteTemplateDrinks(id)
                id
            } else {
                dataSource.insertTemplate(request).id
            }

            val drinkAssociations = drinkIds.mapIndexed { index, drinkId ->
                TemplateDrinkRequest(
                    templateId = templateId,
                    drinkId = drinkId,
                    sortOrder = index
                )
            }
            dataSource.insertTemplateDrinks(drinkAssociations)

            templateId
        }
    }

    suspend fun deleteTemplate(id: String): Result<Unit> {
        return runCatching {
            dataSource.deleteTemplate(id)
        }
    }
}