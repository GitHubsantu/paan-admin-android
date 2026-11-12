package com.paan.safaadmin.data.remote

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.MultipartBody
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.Part

interface AdminApi {
    @POST("admin/login")
    suspend fun login(@Body body: AdminLoginRequest): ApiResponse<AdminAuthDto>

    @POST("auth/logout")
    suspend fun logout(): ApiResponse<JsonElement>

    @GET("admin/orders")
    suspend fun orders(
        @Query("status") status: String,
        @Query("sort") sort: String,
        @Query("per_page") perPage: Int,
        @Query("page") page: Int,
    ): ApiResponse<List<OrderDto>>

    @GET("admin/orders/{id}")
    suspend fun order(@Path("id") id: String): ApiResponse<OrderDto>

    @PATCH("admin/orders/{id}/status")
    suspend fun updateStatus(@Path("id") id: String, @Body body: StatusRequest): ApiResponse<OrderDto>

    @POST("admin/orders/{id}/cancel")
    suspend fun cancel(@Path("id") id: String, @Body body: CancelRequest): ApiResponse<OrderDto>

    @GET("admin/dashboard")
    suspend fun dashboard(): ApiResponse<DashboardDto>

    @GET("admin/products")
    suspend fun products(
        @Query("per_page") perPage: Int,
        @Query("page") page: Int,
    ): ApiResponse<List<ProductDto>>

    @GET("admin/categories")
    suspend fun categories(): ApiResponse<List<CategoryRowDto>>

    @Multipart @POST("admin/categories")
    suspend fun createCategory(@Part parts: List<MultipartBody.Part>): ApiResponse<CategoryRowDto>

    @Multipart @POST("admin/categories/{id}")
    suspend fun updateCategory(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ApiResponse<CategoryRowDto>

    @DELETE("admin/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): ApiResponse<JsonElement>

    @Multipart @POST("admin/products")
    suspend fun createProduct(@Part parts: List<MultipartBody.Part>): ApiResponse<ProductDto>

    @Multipart @POST("admin/products/{id}")
    suspend fun updateProduct(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ApiResponse<ProductDto>

    @DELETE("admin/products/{id}")
    suspend fun deleteProduct(@Path("id") id: String): ApiResponse<JsonElement>

    @PATCH("admin/products/{id}/availability")
    suspend fun setAvailability(@Path("id") id: String, @Body body: AvailabilityRequest): ApiResponse<ProductDto>

    @GET("admin/settings")
    suspend fun settings(): ApiResponse<SettingsDto>

    @PUT("admin/settings")
    suspend fun updateSettings(@Body body: SettingsDto): ApiResponse<SettingsDto>

    @GET("admin/customers")
    suspend fun customers(
        @Query("search") search: String?,
        @Query("per_page") perPage: Int,
        @Query("page") page: Int,
    ): ApiResponse<List<CustomerRowDto>>

    @GET("admin/customers/{id}")
    suspend fun customer(@Path("id") id: String): ApiResponse<CustomerDetailDto>

    @POST("admin/customers")
    suspend fun createCustomer(@Body body: CustomerRequest): ApiResponse<CustomerRowDto>

    @PUT("admin/customers/{id}")
    suspend fun updateCustomer(@Path("id") id: String, @Body body: CustomerRequest): ApiResponse<CustomerRowDto>

    @PATCH("admin/customers/{id}/block")
    suspend fun blockCustomer(@Path("id") id: String, @Body body: BlockRequest): ApiResponse<CustomerRowDto>

    @DELETE("admin/customers/{id}")
    suspend fun deleteCustomer(@Path("id") id: String): ApiResponse<JsonElement>

    @GET("admin/customers/deleted")
    suspend fun deletedCustomers(
        @Query("search") search: String?,
        @Query("per_page") perPage: Int,
        @Query("page") page: Int,
    ): ApiResponse<List<DeletedCustomerDto>>

    @POST("admin/customers/{id}/restore")
    suspend fun restoreCustomer(@Path("id") id: String): ApiResponse<CustomerRowDto>

    @DELETE("admin/customers/{id}/force")
    suspend fun purgeCustomer(@Path("id") id: String): ApiResponse<JsonElement>

    @POST("device-tokens")
    suspend fun registerDevice(@Body body: DeviceTokenRequest): ApiResponse<JsonElement>

    @DELETE("device-tokens/{token}")
    suspend fun removeDevice(@Path("token") token: String): ApiResponse<JsonElement>
}