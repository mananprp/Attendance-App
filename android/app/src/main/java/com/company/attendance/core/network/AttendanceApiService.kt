package com.company.attendance.core.network

import com.company.attendance.core.network.model.AppConfigResponseDto
import com.company.attendance.core.network.model.LoginRequestDto
import com.company.attendance.core.network.model.LoginResponseDto
import com.company.attendance.core.network.model.ModelManifestResponseDto
import com.company.attendance.core.network.model.MonthlyAttendanceResponseDto
import com.company.attendance.core.network.model.PhotoDetailResponseDto
import com.company.attendance.core.network.model.PhotoUploadResponseDto
import com.company.attendance.core.network.model.SyncBatchResponseDto
import com.company.attendance.core.network.model.AdminSiteDto
import com.company.attendance.core.network.model.AdminUserDto
import com.company.attendance.core.network.model.CreateUserRequestDto
import com.company.attendance.core.network.model.CreateSiteRequestDto
import com.company.attendance.core.network.model.UpdateUserSiteRequestDto
import com.company.attendance.core.network.model.UpdateUserRoleRequestDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query

interface AttendanceApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<LoginResponseDto>

    @GET("config")
    suspend fun getConfig(): Response<AppConfigResponseDto>

    @Multipart
    @POST("users/me/photo")
    suspend fun uploadPhotoV1(
        @Part photo: MultipartBody.Part
    ): Response<PhotoUploadResponseDto>

    @Multipart
    @PUT("users/me/photo")
    suspend fun replacePhotoV2(
        @Part photo: MultipartBody.Part
    ): Response<PhotoUploadResponseDto>

    @GET("users/me/photo")
    suspend fun getActivePhoto(): Response<PhotoDetailResponseDto>

    @Multipart
    @POST("attendance/sync")
    suspend fun syncPunches(
        @Part batchJson: MultipartBody.Part,
        @Part images: List<MultipartBody.Part> = emptyList()
    ): Response<SyncBatchResponseDto>

    @GET("attendance")
    suspend fun getMonthlyAttendance(
        @Query("month") month: String
    ): Response<MonthlyAttendanceResponseDto>

    @GET("models/manifest")
    suspend fun getModelManifest(): Response<ModelManifestResponseDto>

    // Admin endpoints
    @GET("admin/sites")
    suspend fun getAdminSites(): Response<List<AdminSiteDto>>

    @POST("admin/sites")
    suspend fun createAdminSite(
        @Body request: CreateSiteRequestDto
    ): Response<AdminSiteDto>

    @GET("admin/users")
    suspend fun getAdminUsers(): Response<List<AdminUserDto>>

    @POST("admin/users")
    suspend fun createAdminUser(
        @Body request: CreateUserRequestDto
    ): Response<AdminUserDto>

    @PATCH("admin/users/{userId}/site")
    suspend fun updateAdminUserSite(
        @Path("userId") userId: String,
        @Body request: UpdateUserSiteRequestDto
    ): Response<AdminUserDto>

    @PATCH("admin/users/{userId}/role")
    suspend fun updateAdminUserRole(
        @Path("userId") userId: String,
        @Body request: UpdateUserRoleRequestDto
    ): Response<AdminUserDto>
}
