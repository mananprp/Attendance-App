package com.company.attendance.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_local")
data class UserLocalEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "photo_change_used")
    val photoChangeUsed: Boolean,

    @ColumnInfo(name = "has_photo")
    val hasPhoto: Boolean,

    @ColumnInfo(name = "reference_embedding")
    val referenceEmbedding: ByteArray?,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserLocalEntity

        if (userId != other.userId) return false
        if (displayName != other.displayName) return false
        if (photoChangeUsed != other.photoChangeUsed) return false
        if (hasPhoto != other.hasPhoto) return false
        if (referenceEmbedding != null) {
            if (other.referenceEmbedding == null) return false
            if (!referenceEmbedding.contentEquals(other.referenceEmbedding)) return false
        } else if (other.referenceEmbedding != null) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + photoChangeUsed.hashCode()
        result = 31 * result + hasPhoto.hashCode()
        result = 31 * result + (referenceEmbedding?.contentHashCode() ?: 0)
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
