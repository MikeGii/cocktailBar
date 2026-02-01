package com.example.cocktailbar.data.repository

import com.example.cocktailbar.data.model.Drink
import com.example.cocktailbar.data.model.DrinkRequest
import com.example.cocktailbar.data.model.DrinkVariant
import com.example.cocktailbar.data.model.DrinkVariantRequest
import com.example.cocktailbar.data.remote.SupabaseDataSource

class DrinksRepository(
    private val dataSource: SupabaseDataSource = SupabaseDataSource()
) {

    suspend fun getDrinksWithVariants(): Result<List<Drink>> {
        return runCatching {
            val drinks = dataSource.getDrinks()
            val variants = dataSource.getDrinkVariants()
            val variantsByDrink = variants.groupBy { it.drinkId }

            drinks.map { drink ->
                drink.copy().apply {
                    this.variants = variantsByDrink[drink.id]
                        ?.sortedBy { it.sortOrder ?: 0 } ?: emptyList()
                }
            }
        }
    }

    suspend fun addDrink(
        name: String,
        description: String?,
        variants: List<DrinkVariant>
    ): Result<Drink> {
        return runCatching {
            val request = DrinkRequest(
                name = name,
                description = description?.ifEmpty { null }
            )
            val drink = dataSource.insertDrink(request)

            val variantRequests = variants.map { variant ->
                DrinkVariantRequest(
                    drinkId = drink.id,
                    sizeName = variant.sizeName,
                    price = variant.price,
                    sortOrder = variant.sortOrder ?: 0
                )
            }
            dataSource.insertVariants(variantRequests)

            drink.apply { this.variants = variants }
        }
    }

    suspend fun updateDrink(
        id: String,
        name: String,
        description: String?,
        variants: List<DrinkVariant>
    ): Result<Unit> {
        return runCatching {
            val request = DrinkRequest(
                name = name,
                description = description?.ifEmpty { null }
            )
            dataSource.updateDrink(id, request)

            // Replace variants
            dataSource.deleteVariantsByDrinkId(id)
            val variantRequests = variants.map { variant ->
                DrinkVariantRequest(
                    drinkId = id,
                    sizeName = variant.sizeName,
                    price = variant.price,
                    sortOrder = variant.sortOrder ?: 0
                )
            }
            dataSource.insertVariants(variantRequests)
        }
    }

    suspend fun deleteDrink(id: String): Result<Unit> {
        return runCatching {
            dataSource.deleteDrink(id)
        }
    }
}