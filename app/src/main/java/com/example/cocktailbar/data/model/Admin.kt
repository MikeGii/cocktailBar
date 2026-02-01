package com.example.cocktailbar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Admin(
    val id: Int,
    val username: String,
    val password: String
)