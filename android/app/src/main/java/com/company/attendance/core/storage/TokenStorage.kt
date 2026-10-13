package com.company.attendance.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.company.attendance.core.model.UserSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface TokenStorage {
    fun getToken(): String?
    fun getUserId(): String?
    fun getDisplayName(): String?
    fun isPhotoChangeUsed(): Boolean
    fun hasPhoto(): Boolean
    fun getActivePhotoVersion(): Int?
    fun getRole(): String
    fun isAdmin(): Boolean
    fun getSiteCode(): String
    fun getUserSession(): UserSession?
    val sessionFlow: StateFlow<UserSession?>

    fun saveSession(session: UserSession)
    fun updatePhotoChangeUsed(used: Boolean)
    fun updateHasPhoto(hasPhoto: Boolean, version: Int? = null)
    fun clearSession()
}

@Singleton
class TokenStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenStorage {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to standard SharedPreferences for unit tests or environments without Keystore
            context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        }
    }

    private val _sessionFlow = MutableStateFlow<UserSession?>(null)
    override val sessionFlow: StateFlow<UserSession?> = _sessionFlow.asStateFlow()

    init {
        _sessionFlow.value = loadSessionInternal()
    }

    private fun loadSessionInternal(): UserSession? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""
        val photoChangeUsed = prefs.getBoolean(KEY_PHOTO_CHANGE_USED, false)
        val hasPhoto = prefs.getBoolean(KEY_HAS_PHOTO, false)
        val version = if (prefs.contains(KEY_ACTIVE_PHOTO_VERSION)) prefs.getInt(KEY_ACTIVE_PHOTO_VERSION, 1) else null
        val role = prefs.getString(KEY_ROLE, "EMPLOYEE") ?: "EMPLOYEE"
        val siteCode = prefs.getString(KEY_SITE_CODE, "DEFAULT_OFFICE") ?: "DEFAULT_OFFICE"

        return UserSession(
            userId = userId,
            displayName = displayName,
            token = token,
            role = role,
            siteCode = siteCode,
            photoChangeUsed = photoChangeUsed,
            hasPhoto = hasPhoto,
            activePhotoVersion = version
        )
    }

    override fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    override fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    override fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)

    override fun isPhotoChangeUsed(): Boolean = prefs.getBoolean(KEY_PHOTO_CHANGE_USED, false)

    override fun hasPhoto(): Boolean = prefs.getBoolean(KEY_HAS_PHOTO, false)

    override fun getActivePhotoVersion(): Int? {
        return if (prefs.contains(KEY_ACTIVE_PHOTO_VERSION)) prefs.getInt(KEY_ACTIVE_PHOTO_VERSION, 1) else null
    }

    override fun getRole(): String = prefs.getString(KEY_ROLE, "EMPLOYEE") ?: "EMPLOYEE"

    override fun isAdmin(): Boolean = getRole().equals("ADMIN", ignoreCase = true)

    override fun getSiteCode(): String = prefs.getString(KEY_SITE_CODE, "DEFAULT_OFFICE") ?: "DEFAULT_OFFICE"

    override fun getUserSession(): UserSession? = _sessionFlow.value

    override fun saveSession(session: UserSession) {
        prefs.edit().apply {
            putString(KEY_TOKEN, session.token)
            putString(KEY_USER_ID, session.userId)
            putString(KEY_DISPLAY_NAME, session.displayName)
            putString(KEY_ROLE, session.role)
            putString(KEY_SITE_CODE, session.siteCode)
            putBoolean(KEY_PHOTO_CHANGE_USED, session.photoChangeUsed)
            putBoolean(KEY_HAS_PHOTO, session.hasPhoto)
            if (session.activePhotoVersion != null) {
                putInt(KEY_ACTIVE_PHOTO_VERSION, session.activePhotoVersion)
            } else {
                remove(KEY_ACTIVE_PHOTO_VERSION)
            }
            apply()
        }
        _sessionFlow.value = session
    }

    override fun updatePhotoChangeUsed(used: Boolean) {
        prefs.edit().putBoolean(KEY_PHOTO_CHANGE_USED, used).apply()
        _sessionFlow.value = _sessionFlow.value?.copy(photoChangeUsed = used)
    }

    override fun updateHasPhoto(hasPhoto: Boolean, version: Int?) {
        prefs.edit().apply {
            putBoolean(KEY_HAS_PHOTO, hasPhoto)
            if (version != null) {
                putInt(KEY_ACTIVE_PHOTO_VERSION, version)
            }
            apply()
        }
        _sessionFlow.value = _sessionFlow.value?.copy(
            hasPhoto = hasPhoto,
            activePhotoVersion = version ?: _sessionFlow.value?.activePhotoVersion
        )
    }

    override fun clearSession() {
        prefs.edit().clear().apply()
        _sessionFlow.value = null
    }

    companion object {
        private const val PREFS_FILENAME = "secure_attendance_session_prefs"
        private const val KEY_TOKEN = "auth_jwt_token"
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_DISPLAY_NAME = "auth_display_name"
        private const val KEY_ROLE = "auth_role"
        private const val KEY_SITE_CODE = "auth_site_code"
        private const val KEY_PHOTO_CHANGE_USED = "auth_photo_change_used"
        private const val KEY_HAS_PHOTO = "auth_has_photo"
        private const val KEY_ACTIVE_PHOTO_VERSION = "auth_active_photo_version"
    }
}
