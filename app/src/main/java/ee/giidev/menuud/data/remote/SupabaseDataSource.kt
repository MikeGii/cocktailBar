package ee.giidev.menuud.data.remote

import ee.giidev.menuud.SupabaseClient
import io.github.jan.supabase.storage.storage

/**
 * Thin wrapper over Supabase Storage. The app only stores and serves images;
 * there is no database access.
 */
class SupabaseDataSource {

    private val storage = SupabaseClient.client.storage

    suspend fun getStorageFiles(bucket: String): List<String> {
        val storageBucket = storage.from(bucket)
        return storageBucket.list()
            .filter { it.name.isNotEmpty() && !it.name.startsWith(".") }
            .map { storageBucket.publicUrl(it.name) }
    }

    suspend fun uploadFile(bucket: String, fileName: String, bytes: ByteArray) {
        storage.from(bucket).upload(fileName, bytes)
    }

    suspend fun deleteFile(bucket: String, fileName: String) {
        storage.from(bucket).delete(fileName)
    }

    /** Lightweight reachability check: list the bucket, succeed or fail. */
    suspend fun pingConnection(bucket: String): Boolean {
        return try {
            storage.from(bucket).list()
            true
        } catch (e: Exception) {
            false
        }
    }
}
