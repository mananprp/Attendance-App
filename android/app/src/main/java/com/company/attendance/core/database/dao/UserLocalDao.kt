package com.company.attendance.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.attendance.core.database.entity.UserLocalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserLocalDao {
    @Query("SELECT * FROM user_local WHERE user_id = :userId")
    fun getUser(userId: String): Flow<UserLocalEntity?>

    @Query("SELECT * FROM user_local WHERE user_id = :userId")
    suspend fun getUserDirect(userId: String): UserLocalEntity?

    @Query("SELECT * FROM user_local LIMIT 1")
    fun getActiveUser(): Flow<UserLocalEntity?>

    @Query("SELECT * FROM user_local LIMIT 1")
    suspend fun getActiveUserDirect(): UserLocalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserLocalEntity)

    @Query("UPDATE user_local SET photo_change_used = :used, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun updatePhotoChangeUsed(userId: String, used: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE user_local SET reference_embedding = :embedding, has_photo = 1, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun updateReferenceEmbedding(userId: String, embedding: ByteArray, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE user_local SET has_photo = :hasPhoto, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun updateHasPhoto(userId: String, hasPhoto: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_local")
    suspend fun clearAll()
}
