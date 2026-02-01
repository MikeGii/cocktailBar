package com.example.cocktailbar.data.remote

import com.example.cocktailbar.SupabaseClient
import com.example.cocktailbar.data.model.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage

class SupabaseDataSource {

    private val client = SupabaseClient.client

    // ==================== DRINKS ====================

    suspend fun getDrinks(): List<Drink> {
        return client.from(Tables.DRINKS).select().decodeList()
    }

    suspend fun getDrinkVariants(): List<DrinkVariant> {
        return client.from(Tables.DRINK_VARIANTS).select().decodeList()
    }

    suspend fun insertDrink(request: DrinkRequest): Drink {
        return client.from(Tables.DRINKS)
            .insert(request) { select() }
            .decodeList<Drink>()
            .first()
    }

    suspend fun updateDrink(id: String, request: DrinkRequest) {
        client.from(Tables.DRINKS).update(request) {
            filter { eq("id", id) }
        }
    }

    suspend fun deleteDrink(id: String) {
        client.from(Tables.DRINKS).delete {
            filter { eq("id", id) }
        }
    }

    suspend fun insertVariants(variants: List<DrinkVariantRequest>) {
        if (variants.isNotEmpty()) {
            client.from(Tables.DRINK_VARIANTS).insert(variants)
        }
    }

    suspend fun deleteVariantsByDrinkId(drinkId: String) {
        client.from(Tables.DRINK_VARIANTS).delete {
            filter { eq("drink_id", drinkId) }
        }
    }

    // ==================== TEMPLATES ====================

    suspend fun getTemplates(): List<Template> {
        return client.from(Tables.TEMPLATES).select().decodeList()
    }

    suspend fun getTemplateById(id: String): Template {
        return client.from(Tables.TEMPLATES).select {
            filter { eq("id", id) }
        }.decodeSingle()
    }

    suspend fun getTemplateDrinks(): List<TemplateDrink> {
        return client.from(Tables.TEMPLATE_DRINKS).select().decodeList()
    }

    suspend fun getTemplateDrinksByTemplateId(templateId: String): List<TemplateDrink> {
        return client.from(Tables.TEMPLATE_DRINKS).select {
            filter { eq("template_id", templateId) }
        }.decodeList()
    }

    suspend fun insertTemplate(request: TemplateRequest): Template {
        return client.from(Tables.TEMPLATES)
            .insert(request) { select() }
            .decodeSingle()
    }

    suspend fun updateTemplate(id: String, request: TemplateRequest) {
        client.from(Tables.TEMPLATES).update(request) {
            filter { eq("id", id) }
        }
    }

    suspend fun deleteTemplate(id: String) {
        client.from(Tables.TEMPLATES).delete {
            filter { eq("id", id) }
        }
    }

    suspend fun deleteTemplateDrinks(templateId: String) {
        client.from(Tables.TEMPLATE_DRINKS).delete {
            filter { eq("template_id", templateId) }
        }
    }

    suspend fun insertTemplateDrinks(drinks: List<TemplateDrinkRequest>) {
        if (drinks.isNotEmpty()) {
            client.from(Tables.TEMPLATE_DRINKS).insert(drinks)
        }
    }

    // ==================== ADMIN ====================

    suspend fun verifyAdminCredentials(username: String, password: String): Boolean {
        val result = client.from(Tables.ADMIN).select {
            filter {
                eq("username", username)
                eq("password", password)
            }
        }.decodeList<Admin>()
        return result.isNotEmpty()
    }

    suspend fun pingConnection(): Boolean {
        return try {
            client.from(Tables.ADMIN).select { limit(1) }
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==================== STORAGE ====================

    suspend fun getStorageFiles(bucket: String): List<String> {
        val storageBucket = client.storage.from(bucket)
        val files = storageBucket.list()
        return files
            .filter { it.name.isNotEmpty() && !it.name.startsWith(".") }
            .map { storageBucket.publicUrl(it.name) }
    }

    suspend fun uploadFile(bucket: String, fileName: String, bytes: ByteArray) {
        client.storage.from(bucket).upload(fileName, bytes)
    }

    suspend fun deleteFile(bucket: String, fileName: String) {
        client.storage.from(bucket).delete(fileName)
    }

    // ==================== CONSTANTS ====================

    private object Tables {
        const val ADMIN = "admin"
        const val DRINKS = "drinks"
        const val DRINK_VARIANTS = "drink_variants"
        const val TEMPLATES = "templates"
        const val TEMPLATE_DRINKS = "template_drinks"
    }
}