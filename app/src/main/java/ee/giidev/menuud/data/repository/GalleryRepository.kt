package ee.giidev.menuud.data.repository

import ee.giidev.menuud.data.model.GalleryImage
import ee.giidev.menuud.data.remote.SupabaseDataSource

class GalleryRepository(
    private val dataSource: SupabaseDataSource = SupabaseDataSource()
) {

    suspend fun getImages(bucket: String): Result<List<GalleryImage>> {
        return runCatching {
            val urls = dataSource.getStorageFiles(bucket)
            urls.map { url ->
                val fileName = url.substringAfterLast("/")
                GalleryImage(
                    name = fileName,
                    url = url,
                    bucket = bucket
                )
            }
        }
    }

    suspend fun uploadImage(
        bucket: String,
        fileName: String,
        bytes: ByteArray
    ): Result<Unit> {
        return runCatching {
            dataSource.uploadFile(bucket, fileName, bytes)
        }
    }

    suspend fun deleteImage(bucket: String, fileName: String): Result<Unit> {
        return runCatching {
            dataSource.deleteFile(bucket, fileName)
        }
    }

    suspend fun checkConnection(bucket: String): Boolean {
        return try {
            dataSource.pingConnection(bucket)
        } catch (e: Exception) {
            false
        }
    }
}
