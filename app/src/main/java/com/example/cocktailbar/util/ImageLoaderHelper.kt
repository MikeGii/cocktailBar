package com.example.cocktailbar.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageLoaderHelper {

    suspend fun loadBitmap(context: Context, url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()

                val result = (loader.execute(request) as? SuccessResult)?.drawable
                (result as? BitmapDrawable)?.bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}